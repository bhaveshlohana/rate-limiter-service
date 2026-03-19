package com.bhavesh.learn.ratelimiter.core.model;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitConfigTest {

    @Test
    void builder_and_getters_work() {
        RateLimitConfig cfg = RateLimitConfig.builder()
                .clientType("t1")
                .algorithm(Algorithm.FIXED_WINDOW)
                .limit(10)
                .windowSizeSeconds(60)
                .build();

        assertEquals("t1", cfg.getClientType());
        assertEquals(Algorithm.FIXED_WINDOW, cfg.getAlgorithm());
        assertEquals(10, cfg.getLimit());
    }
}

