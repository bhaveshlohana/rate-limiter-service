package com.bhavesh.learn.ratelimiter.starter;

import com.bhavesh.learn.ratelimiter.config.EmbeddedRedisConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.core.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static com.bhavesh.learn.ratelimiter.core.domain.Algorithm.SLIDING_WINDOW;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class SlidingWindowRateLimiterTest {

    @Autowired
    private SlidingWindowRateLimiter slidingWindowRateLimiter;

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
                .algorithm(SLIDING_WINDOW)
                .limit(5)
                .windowSizeSeconds(60)
                .build();
        for (int i = 0; i < 5; i++) {
            RateLimitResponse response = slidingWindowRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
    }

    @Test
    void shouldAllowRequestsUnderLimitWithSleep() throws InterruptedException {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(SLIDING_WINDOW)
                .limit(5)
                .windowSizeSeconds(2)
                .build();
        for (int i = 0; i < 5; i++) {
            RateLimitResponse response = slidingWindowRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
            sleep(Duration.ofMillis(600));
        }
        RateLimitResponse response = slidingWindowRateLimiter.isAllowed(userId, request);
        assertTrue(response.isAllowed());
    }

    @Test
    void shouldBlockRequestsOverLimit() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(SLIDING_WINDOW)
                .limit(3)
                .windowSizeSeconds(60)
                .build();
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = slidingWindowRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = slidingWindowRateLimiter.isAllowed(userId, request);
        assertFalse(response.isAllowed());
        assertEquals("Rate limit exceeded", response.getReason());
        assertEquals(0, response.getRemainingRequests());
    }

    @Test
    void shouldAllowAndBlock_usingLuaScript() throws InterruptedException {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(SLIDING_WINDOW)
                .limit(3)
                .windowSizeSeconds(60)
                .build();
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = slidingWindowRateLimiter.isAllowedLua(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = slidingWindowRateLimiter.isAllowedLua(userId, request);
        assertFalse(response.isAllowed());
    }

}