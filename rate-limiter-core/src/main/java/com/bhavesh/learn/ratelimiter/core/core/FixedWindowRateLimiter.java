package com.bhavesh.learn.ratelimiter.core.core;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class FixedWindowRateLimiter implements RateLimiter {

    private static final RedisScript<List> FIXED_WINDOW_SCRIPT = RedisScript.of(
            """
                    local key = KEYS[1]
                    local limit = tonumber(ARGV[1])
                    local windowSizeSeconds = tonumber(ARGV[2])
                    
                    local count = redis.call('GET', key)
                    if count == false then
                        redis.call('SET', key, 1)
                        redis.call('EXPIRE', key, windowSizeSeconds)
                        return {1, limit - 1}
                    end
                    count = tonumber(count)
                    if count < limit then
                        redis.call('INCR', key)
                        return {1, limit - count - 1}
                    end
                    return {0, 0}
                    """,
            List.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public FixedWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitResponse isAllowed(String userId, RateLimitConfig rateLimitConfig) {
        int windowSizeSeconds = rateLimitConfig.getWindowSizeSeconds();
        int limit = rateLimitConfig.getLimit();

        long windowStart = System.currentTimeMillis() / 1000 / windowSizeSeconds * windowSizeSeconds;
        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm(), windowStart);

        String value = redisTemplate.opsForValue().get(key);
        int count = value == null ? 0 : Integer.parseInt(value);

        if (value == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSizeSeconds));
            return RateLimitResponse.builder()
                    .allowed(true)
                    .reason("Request allowed")
                    .remainingRequests(limit - 1)
                    .build();
        } else if (count < limit) {
            redisTemplate.opsForValue().increment(key);
            return RateLimitResponse.builder()
                    .allowed(true)
                    .reason("Request allowed")
                    .remainingRequests(limit - (count + 1))
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

        long windowStart = System.currentTimeMillis() / 1000 / windowSizeSeconds * windowSizeSeconds;
        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm(), windowStart);
        try {
            List<Long> result = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(windowSizeSeconds)
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
