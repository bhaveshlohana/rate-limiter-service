package com.bhavesh.learn.ratelimiter.core.service;

import com.bhavesh.learn.ratelimiter.core.core.FixedWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.SlidingWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.TokenBucketRateLimiter;
import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.core.service.RateLimiterFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimiterFactoryTest {

    private ClientConfigService clientConfigService;
    private FixedWindowRateLimiter fixedWindowRateLimiter;
    private SlidingWindowRateLimiter slidingWindowRateLimiter;
    private TokenBucketRateLimiter tokenBucketRateLimiter;
    private SimpleMeterRegistry meterRegistry;
    private RateLimiterFactory factory;

    @BeforeEach
    void setup() {
        clientConfigService = Mockito.mock(ClientConfigService.class);
        fixedWindowRateLimiter = Mockito.mock(FixedWindowRateLimiter.class);
        slidingWindowRateLimiter = Mockito.mock(SlidingWindowRateLimiter.class);
        tokenBucketRateLimiter = Mockito.mock(TokenBucketRateLimiter.class);
        meterRegistry = new SimpleMeterRegistry();
        factory = new RateLimiterFactory(clientConfigService, fixedWindowRateLimiter, slidingWindowRateLimiter, tokenBucketRateLimiter, meterRegistry);
    }

    @Test
    void whenConfigMissing_returnsRejectedResponse() throws Exception {
        when(clientConfigService.getConfig("t1")).thenThrow(new ConfigNotFoundException("no config"));
        RateLimitRequest req = RateLimitRequest.builder().clientId("u").clientType("t1").build();
        RateLimitResponse resp = factory.applyRateLimiting(req);
        assertFalse(resp.isAllowed());
        assertTrue(resp.getReason().contains("Error fetching rate limit config"));
    }

    @Test
    void whenAlgorithmFixed_callsFixedWindow() throws Exception {
        RateLimitConfig cfg = RateLimitConfig.builder().algorithm(Algorithm.FIXED_WINDOW).build();
        when(clientConfigService.getConfig("t1")).thenReturn(cfg);
        when(fixedWindowRateLimiter.isAllowedLua(anyString(), any())).thenReturn(RateLimitResponse.builder().allowed(true).remainingRequests(1).build());

        RateLimitResponse resp = factory.applyRateLimiting(RateLimitRequest.builder().clientId("u").clientType("t1").build());
        assertTrue(resp.isAllowed());
        verify(fixedWindowRateLimiter).isAllowedLua("u", cfg);
    }

    @Test
    void whenAlgorithmSliding_callsSlidingWindow() throws Exception {
        RateLimitConfig cfg = RateLimitConfig.builder().algorithm(Algorithm.SLIDING_WINDOW).build();
        when(clientConfigService.getConfig("t1")).thenReturn(cfg);
        when(slidingWindowRateLimiter.isAllowedLua(anyString(), any())).thenReturn(RateLimitResponse.builder().allowed(false).remainingRequests(0).build());

        RateLimitResponse resp = factory.applyRateLimiting(RateLimitRequest.builder().clientId("u").clientType("t1").build());
        assertFalse(resp.isAllowed());
        verify(slidingWindowRateLimiter).isAllowedLua("u", cfg);
    }

    @Test
    void whenAlgorithmToken_callsTokenBucket() throws Exception {
        RateLimitConfig cfg = RateLimitConfig.builder().algorithm(Algorithm.TOKEN_BUCKET).build();
        when(clientConfigService.getConfig("t1")).thenReturn(cfg);
        when(tokenBucketRateLimiter.isAllowedLua(anyString(), any())).thenReturn(RateLimitResponse.builder().allowed(true).remainingRequests(2).build());

        RateLimitResponse resp = factory.applyRateLimiting(RateLimitRequest.builder().clientId("u").clientType("t1").build());
        assertTrue(resp.isAllowed());
        verify(tokenBucketRateLimiter).isAllowedLua("u", cfg);
    }
}