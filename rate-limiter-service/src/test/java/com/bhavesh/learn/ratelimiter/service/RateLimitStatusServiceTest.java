package com.bhavesh.learn.ratelimiter.service;

import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitStatus;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RateLimitStatusServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private ClientConfigService clientConfigService;
    private RateLimitStatusService statusService;

    @BeforeEach
    void setup() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        clientConfigService = Mockito.mock(ClientConfigService.class);
        statusService = new RateLimitStatusService(redisTemplate, clientConfigService);
    }

    @Test
    void whenNoConfig_returnsEmptyStatus() throws Exception {
        // clientConfigService.getConfig will throw; service should return empty RateLimitStatus
        when(clientConfigService.getConfig(any())).thenThrow(new ConfigNotFoundException("no config"));

        RateLimitStatus status = statusService.getRateLimitStatus(RateLimitRequest.builder().clientId("u").clientType("t").build());
        assertNotNull(status);
        // default builder produces nulls/zeros; check at least the object isn't null
    }

    // Note: deeper integration of RateLimitStatusService depends on Redis state and ClientConfigService returning a config.
    // We'll assert that when a config exists, the service returns a RateLimitStatus (not necessarily populated in this unit test scope).
//    @Test
//    void whenConfigExists_returnsStatusObject() throws Exception {
//        RateLimitConfig cfg = RateLimitConfig.builder().clientType("t").algorithm(Algorithm.FIXED_WINDOW).limit(5).windowSizeSeconds(60).build();
//        when(clientConfigService.getConfig("t")).thenReturn(cfg);
//
//        RateLimitStatus status = statusService.getRateLimitStatus(RateLimitRequest.builder().clientId("u").clientType("t").build());
//        assertNotNull(status);
//    }
}