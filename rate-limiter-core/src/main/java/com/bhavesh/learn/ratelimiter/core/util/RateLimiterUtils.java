package com.bhavesh.learn.ratelimiter.core.util;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;

import static com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants.*;

public class RateLimiterUtils {
    public static String getKey(String userId, Algorithm algorithm) {
        return RATELIMIT + algorithm.toString() + SEPARATOR + userId;
    }

    public static String getKey(String userId, Algorithm algorithm, Long windowStart) {
        return RATELIMIT + algorithm.toString() + SEPARATOR + userId + SEPARATOR + windowStart;
    }

    public static String getConfigKey(String clientType) {
        return RATELIMIT_CONFIG_PREFIX + clientType;
    }


    public static String getConfigKeyPattern() {
        return getConfigKey("*");
    }
}
