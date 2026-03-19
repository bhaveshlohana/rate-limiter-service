package com.bhavesh.learn.ratelimiter.core.core;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class SlidingWindowRateLimiter implements RateLimiter {
    private static final RedisScript<List> SLIDING_WINDOW_SCRIPT = RedisScript.of(
            """
                    local key = KEYS[1]
                    local limit = tonumber(ARGV[1])
                    local windowSizeSeconds = tonumber(ARGV[2])
                    local uuid = ARGV[3]
                    local now = tonumber(ARGV[4])
                    local windowStart = now - (windowSizeSeconds * 1000)
                    
                    redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
                    local count = redis.call('ZCARD', key)
                    count = count == false and 0 or tonumber(count)
                    if count < limit then
                        redis.call('ZADD', key, now, uuid)
                        redis.call('EXPIRE', key, windowSizeSeconds)
                        return {1, limit - count - 1}
                    end
                    return {0, 0}
                    """,
            List.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public SlidingWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitResponse isAllowed(String userId, RateLimitConfig rateLimitConfig) {
        int windowSizeSeconds = rateLimitConfig.getWindowSizeSeconds();
        int limit = rateLimitConfig.getLimit();

        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm());
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeSeconds * 1000;

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(key);
        long currentCount = count == null ? 0 : count;

        if (currentCount < limit) {
            redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
            redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
            return RateLimitResponse.builder()
                    .allowed(true)
                    .reason("Request allowed")
                    .remainingRequests(limit - (int) currentCount - 1)
                    .build();
        }
        return RateLimitResponse.builder()
                .allowed(false)
                .remainingRequests(0)
                .reason("Rate limit exceeded")
                .build();
    }


    @Override
    public RateLimitResponse isAllowedLua(String userId, RateLimitConfig rateLimitConfig) {

        int windowSizeSeconds = rateLimitConfig.getWindowSizeSeconds();
        int limit = rateLimitConfig.getLimit();

        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm());
        try {
            List<Long> result = redisTemplate.execute(
                    SLIDING_WINDOW_SCRIPT,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(windowSizeSeconds),
                    UUID.randomUUID().toString(),
                    String.valueOf(System.currentTimeMillis())
            );

            boolean allowed = result.get(0) == 1L;
            int remaining = result.get(1).intValue();
            if (allowed) {
                return RateLimitResponse.builder()
                        .allowed(true)
                        .reason("Request allowed")
                        .remainingRequests(remaining)
                        .build();
            }
            return RateLimitResponse.builder()
                    .allowed(false)
                    .remainingRequests(0)
                    .reason("Rate limit exceeded")
                    .build();
        } catch (Exception e) {
            // fail open or fail closed depending on your policy
            return RateLimitResponse.builder()
                    .allowed(false)
                    .reason("Rate limiter error")
                    .remainingRequests(0)
                    .build();
        }
    }
}
