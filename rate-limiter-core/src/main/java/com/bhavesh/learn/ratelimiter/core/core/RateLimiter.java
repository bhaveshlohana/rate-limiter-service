package com.bhavesh.learn.ratelimiter.core.core;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;

public interface RateLimiter {
    RateLimitResponse isAllowed(String userId, RateLimitConfig rateLimitConfig);

    RateLimitResponse isAllowedLua(String userId, RateLimitConfig rateLimitConfig);
}
