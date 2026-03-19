package com.bhavesh.learn.ratelimiter.core.core;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenBucketRateLimiter implements RateLimiter {

    private static final RedisScript<List> TOKEN_BUCKET_SCRIPT = RedisScript.of(
            """
                  local key = KEYS[1]
                  local capacity = tonumber(ARGV[1])
                  local refillRatePerSecond = tonumber(ARGV[2])
                  local now = tonumber(ARGV[3])
                  
                  local data = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
                  local tokens = tonumber(data[1])
                  local lastRefillTime = tonumber(data[2])
                  
                  if tokens == nil or lastRefillTime == nil then
                      redis.call('HMSET', key, 'tokens', capacity - 1, 'lastRefillTime', now)
                      return {1, capacity -1}
                  end
                  
                  local elapsedTime = now - lastRefillTime
                  local tokensToAdd = (elapsedTime / 1000) * refillRatePerSecond
                  
                  local currentTokens = capacity < tokens + tokensToAdd and capacity or tokens + tokensToAdd
                  local allowed = currentTokens >= 1
                  if allowed then
                        currentTokens = currentTokens - 1
                  end
                  redis.call('HMSET', key, 'tokens', currentTokens, 'lastRefillTime', now)
                  if allowed then
                        return {1, currentTokens}
                  end
                  return {0, 0}
                  """,
            List.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBucketRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitResponse isAllowed(String userId, RateLimitConfig rateLimitConfig) {
        double refillRatePerSecond = rateLimitConfig.getRefillRatePerSecond();
        int capacity = rateLimitConfig.getCapacity();

        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm());

        long now = System.currentTimeMillis();
        List<Object> values = redisTemplate.opsForHash().multiGet(key, List.of("tokens", "lastRefillTime"));
        String tokens = (String) values.get(0);
        String lastRefillTime = (String) values.get(1);

        if (tokens == null || lastRefillTime == null) {
            redisTemplate.opsForHash().put(key, "tokens", String.valueOf(capacity-1));
            redisTemplate.opsForHash().put(key, "lastRefillTime", String.valueOf(now));
            return RateLimitResponse.builder()
                    .allowed(true)
                    .reason("Request allowed")
                    .remainingRequests(capacity - 1)
                    .build();
        }

        double currentTokens = Double.parseDouble(tokens);

        long lastRefill = Long.parseLong(lastRefillTime);
        long elapsedTime = now - lastRefill;
        double tokensToAdd = (elapsedTime / 1000.0) * refillRatePerSecond;

        currentTokens = Math.min(capacity, currentTokens + tokensToAdd);


        boolean allowed = currentTokens >= 1;
        if (allowed) currentTokens -= 1;

        Map<String, String> updates = new HashMap<>();
        updates.put("tokens", String.valueOf(currentTokens));
        updates.put("lastRefillTime", String.valueOf(now));
        redisTemplate.opsForHash().putAll(key, updates);

        if (allowed) {
            return RateLimitResponse.builder()
                    .allowed(true)
                    .reason("Request allowed")
                    .remainingRequests((int) currentTokens)
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

        double refillRatePerSecond = rateLimitConfig.getRefillRatePerSecond();
        int capacity = rateLimitConfig.getCapacity();

        String key = RateLimiterUtils.getKey(userId, rateLimitConfig.getAlgorithm());
        try {
            List<Long> result = redisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRatePerSecond),
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
