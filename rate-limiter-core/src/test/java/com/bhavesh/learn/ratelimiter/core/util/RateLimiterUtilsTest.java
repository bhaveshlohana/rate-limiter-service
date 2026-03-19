package com.bhavesh.learn.ratelimiter.core.util;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import org.junit.jupiter.api.Test;

import static com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants.RATELIMIT;
import static com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants.SEPARATOR;
import static com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants.RATELIMIT_CONFIG_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterUtilsTest {

    @Test
    void getKey_buildsCorrectKey() {
        String key = RateLimiterUtils.getKey("u1", Algorithm.FIXED_WINDOW);
        assertEquals(RATELIMIT + Algorithm.FIXED_WINDOW + SEPARATOR + "u1", key);
    }

    @Test
    void getConfigKey_buildsConfigKey() {
        assertEquals(RATELIMIT_CONFIG_PREFIX + "T1", RateLimiterUtils.getConfigKey("T1"));
    }

    @Test
    void getKey_withAlgorithmAndUser_returnsExpected() {
        String key = RateLimiterUtils.getKey("user1", Algorithm.FIXED_WINDOW);
        assertEquals(RATELIMIT + Algorithm.FIXED_WINDOW + SEPARATOR + "user1", key);
    }

    @Test
    void getConfigKeyPattern_returnsPattern() {
        assertEquals(RATELIMIT_CONFIG_PREFIX + "*", RateLimiterUtils.getConfigKeyPattern());
    }
}