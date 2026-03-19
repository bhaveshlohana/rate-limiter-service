package com.bhavesh.learn.ratelimiter.core.service;

import com.bhavesh.learn.ratelimiter.core.core.FixedWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.SlidingWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.TokenBucketRateLimiter;
import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterFactory {

    private final ClientConfigService clientConfigService;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final MeterRegistry meterRegistry;

    public RateLimiterFactory(ClientConfigService clientConfigService, FixedWindowRateLimiter fixedWindowRateLimiter, SlidingWindowRateLimiter slidingWindowRateLimiter, TokenBucketRateLimiter tokenBucketRateLimiter, MeterRegistry meterRegistry) {
        this.clientConfigService = clientConfigService;
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.slidingWindowRateLimiter = slidingWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.meterRegistry = meterRegistry;
    }

    public RateLimitResponse applyRateLimiting(RateLimitRequest rateLimitRequest) {
        RateLimitConfig config;
        try {
            config = clientConfigService.getConfig(rateLimitRequest.getClientType());
        } catch (ConfigNotFoundException e) {
            meterRegistry.counter("ratelimit.requests.total",
                    "clientType", rateLimitRequest.getClientType(),
                    "algorithm", "UNKNOWN",
                    "result", "rejected"
            ).increment();
            return RateLimitResponse.builder()
                    .allowed(false)
                    .reason(String.format("Error fetching rate limit config for client type: %s with error: %s",  rateLimitRequest.getClientType(), e.getMessage()))
                    .remainingRequests(0)
                    .build();
        }
        String userId = rateLimitRequest.getClientId();
        RateLimitResponse response = switch (config.getAlgorithm()) {
            case Algorithm.FIXED_WINDOW -> fixedWindowRateLimiter.isAllowedLua(userId, config);
            case Algorithm.SLIDING_WINDOW -> slidingWindowRateLimiter.isAllowedLua(userId, config);
            case Algorithm.TOKEN_BUCKET -> tokenBucketRateLimiter.isAllowedLua(userId, config);
        };
        meterRegistry.counter("ratelimit.request.total",
                "clientType", rateLimitRequest.getClientType(),
                "algorithm", config.getAlgorithm().name(),
                "result", response.isAllowed() ? "allowed" : "rejected"
        ).increment();
        return response;
    }
}
