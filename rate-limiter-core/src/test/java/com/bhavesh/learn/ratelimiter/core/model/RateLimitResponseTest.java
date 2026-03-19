package com.bhavesh.learn.ratelimiter.core.model;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitResponseTest {

    @Test
    void allowedFlag_accessor() {
        RateLimitResponse r1 = RateLimitResponse.builder().allowed(true).build();
        RateLimitResponse r2 = RateLimitResponse.builder().allowed(false).build();

        assertTrue(r1.isAllowed());
        assertFalse(r2.isAllowed());
    }
}

