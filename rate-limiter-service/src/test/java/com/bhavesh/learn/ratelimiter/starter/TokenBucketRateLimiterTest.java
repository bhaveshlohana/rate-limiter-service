package com.bhavesh.learn.ratelimiter.starter;

import com.bhavesh.learn.ratelimiter.config.EmbeddedRedisConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.core.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static com.bhavesh.learn.ratelimiter.core.domain.Algorithm.TOKEN_BUCKET;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class TokenBucketRateLimiterTest {

    @Autowired
    private TokenBucketRateLimiter tokenBucketRateLimiter;

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
                .algorithm(TOKEN_BUCKET)
                .capacity(5)
                .refillRatePerSecond(1.0)
                .build();
        for (int i = 0; i < 5; i++) {
            RateLimitResponse response = tokenBucketRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
    }

    @Test
    void shouldAllowRequestsUnderLimitWithSleep() throws InterruptedException {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(TOKEN_BUCKET)
                .capacity(5)
                .refillRatePerSecond(1.0)
                .build();
        for (int i = 0; i < 5; i++) {
            RateLimitResponse response = tokenBucketRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
            sleep(Duration.ofMillis(600));
        }
        RateLimitResponse response = tokenBucketRateLimiter.isAllowed(userId, request);
        assertTrue(response.isAllowed());
    }

    @Test
    void shouldBlockRequestsOverLimit() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(TOKEN_BUCKET)
                .capacity(3)
                .refillRatePerSecond(1.0)
                .build();
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = tokenBucketRateLimiter.isAllowed(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = tokenBucketRateLimiter.isAllowed(userId, request);
        assertFalse(response.isAllowed());
        assertEquals("Rate limit exceeded", response.getReason());
        assertEquals(0, response.getRemainingRequests());
    }

    @Test
    void shouldAllowAndBlock_usingLuaScript() {
        RateLimitConfig request = RateLimitConfig.builder()
                .algorithm(TOKEN_BUCKET)
                .capacity(3)
                .refillRatePerSecond(1.0)
                .build();
        for (int i = 0; i < 3; i++) {
            RateLimitResponse response = tokenBucketRateLimiter.isAllowedLua(userId, request);
            assertTrue(response.isAllowed());
        }
        RateLimitResponse response = tokenBucketRateLimiter.isAllowedLua(userId, request);
        assertFalse(response.isAllowed());
    }

}