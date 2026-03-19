package com.bhavesh.learn.ratelimiter.service;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitStatus;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitStatusService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ClientConfigService clientConfigService;

    public RateLimitStatusService(RedisTemplate<String, String> redisTemplate, ClientConfigService clientConfigService) {
        this.redisTemplate = redisTemplate;
        this.clientConfigService = clientConfigService;
    }

    public RateLimitStatus getRateLimitStatus(RateLimitRequest rateLimitRequest) {
        RateLimitConfig config;
        try {
            config = clientConfigService.getConfig(rateLimitRequest.getClientType());
        }
        catch (Exception e) {
            return RateLimitStatus.builder().build(); // No config means no rate limiting
        }
        return switch (config.getAlgorithm()) {
            case Algorithm.FIXED_WINDOW -> getFixedWindowStatus(rateLimitRequest, config);
            case Algorithm.SLIDING_WINDOW -> getSlidingWindowStatus(rateLimitRequest, config);
            case Algorithm.TOKEN_BUCKET -> getTokenBucketStatus(rateLimitRequest, config);

        };
    }

    private RateLimitStatus getTokenBucketStatus(RateLimitRequest rateLimitRequest, RateLimitConfig config) {
        String userId = rateLimitRequest.getClientId();
        int capacity = config.getCapacity();
        double refillRatePerSecond = config.getRefillRatePerSecond();
        long currentTime = System.currentTimeMillis();
        String key = RateLimiterUtils.getKey(userId, config.getAlgorithm());
        List<Object> values = redisTemplate.opsForHash().multiGet(key, List.of("tokens", "lastRefillTime"));
        Double tokens = values.get(0) != null ? Double.parseDouble((String) values.get(0)) : null;
        Long lastRefillTime = values.get(1) != null ? Long.parseLong((String) values.get(1)) : null;
        if (tokens == null || lastRefillTime == null) {
            return RateLimitStatus.builder()
                    .clientId(userId)
                    .clientType(config.getClientType())
                    .algorithm(config.getAlgorithm())
                    .currentTokens((double) capacity)
                    .remainingRequests(capacity)
                    .build();
        }
        long elapsedTime = currentTime - lastRefillTime;
        double tokensToAdd = (elapsedTime / 1000.0) * refillRatePerSecond;
        double currentTokens = Math.min(capacity, tokens + tokensToAdd);

        return RateLimitStatus.builder()
                .clientId(userId)
                .clientType(rateLimitRequest.getClientType())
                .algorithm(config.getAlgorithm())
                .currentTokens(currentTokens)
                .remainingRequests(Math.max(0, (int) Math.floor(currentTokens)))
                .build();
    }

    private RateLimitStatus getSlidingWindowStatus(RateLimitRequest rateLimitRequest, RateLimitConfig config) {
        String userId = rateLimitRequest.getClientId();
        int limit = config.getLimit();

        String key = RateLimiterUtils.getKey(userId, config.getAlgorithm());

        Long count = redisTemplate.opsForZSet().zCard(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return RateLimitStatus.builder()
                .clientId(userId)
                .clientType(config.getClientType())
                .algorithm(config.getAlgorithm())
                .requestsInWindow(count != null ? count.intValue() : 0)
                .remainingRequests(Math.max(0, limit - (count != null ? count.intValue() : 0)))
                .windowResetsInSeconds(ttl > 0 ? ttl : 0)
                .build();
    }

    private RateLimitStatus getFixedWindowStatus(RateLimitRequest rateLimitRequest, RateLimitConfig config) {
        String userId = rateLimitRequest.getClientId();
        int limit = config.getLimit();
        int windowSizeSeconds = config.getWindowSizeSeconds();
        long windowStart = System.currentTimeMillis() / 1000 / windowSizeSeconds * windowSizeSeconds;
        String key = RateLimiterUtils.getKey(userId, config.getAlgorithm(), windowStart);

        String value = redisTemplate.opsForValue().get(key);
        int count = value == null ? 0 : Integer.parseInt(value);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return RateLimitStatus.builder()
                .clientId(userId)
                .clientType(config.getClientType())
                .algorithm(config.getAlgorithm())
                .requestsInWindow(count)
                .remainingRequests(Math.max(0, limit - count))
                .windowResetsInSeconds(ttl > 0 ? ttl : 0)
                .build();
    }
}
