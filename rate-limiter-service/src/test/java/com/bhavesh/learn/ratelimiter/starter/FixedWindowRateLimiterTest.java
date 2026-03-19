package com.bhavesh.learn.ratelimiter.starter;

import com.bhavesh.learn.ratelimiter.config.EmbeddedRedisConfig;
import com.bhavesh.learn.ratelimiter.core.core.FixedWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static com.bhavesh.learn.ratelimiter.core.domain.Algorithm.FIXED_WINDOW;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class FixedWindowRateLimiterTest {

    @Autowired
    private FixedWindowRateLimiter fixedWindowRateLimiter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String userId = "user123";

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldAllowRequestsUnderLimit() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(FIXED_WINDOW)
                .limit(5)
                .windowSizeSeconds(60)
                .build();
        for (int i = 0; i < 5; i++) {
            RateLimitResponse response = fixedWindowRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
    }

    @Test
    void shouldBlockRequestsOverLimit() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(FIXED_WINDOW)
                .limit(3)
                .windowSizeSeconds(60)
                .build();
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = fixedWindowRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = fixedWindowRateLimiter.isAllowed(userId, request);
        assertFalse(response.isAllowed());
        assertEquals("Rate limit exceeded", response.getReason());
        assertEquals(0, response.getRemainingRequests());
    }

    @Test
    void shouldAllowAndBlock_usingLuaScript() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(FIXED_WINDOW)
                .limit(3)
                .windowSizeSeconds(60)
                .build();
        // Lua path should mirror the non-Lua behavior
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = fixedWindowRateLimiter.isAllowedLua(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = fixedWindowRateLimiter.isAllowedLua(userId, request);
        assertFalse(response.isAllowed());
    }

}