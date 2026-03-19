package com.bhavesh.learn.ratelimiter.core.service;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.core.validator.RateLimitConfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClientConfigServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOps;
    private RateLimitConfigValidator validator;
    private ClientConfigService clientConfigService;

    @BeforeEach
    void setup() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        hashOps = Mockito.mock(HashOperations.class);
        // lenient so other tests' stubs don't break this test when run together
        Mockito.lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        validator = Mockito.mock(RateLimitConfigValidator.class);
        clientConfigService = new ClientConfigService(redisTemplate, validator);
    }

    @Test
    void fetchConfig_returnsConfigWhenPresent() throws Exception {
        when(redisTemplate.opsForHash().multiGet("ratelimit:config:CLIENT", List.of("clientType", "algorithm", "capacity", "refillRatePerSecond", "limit", "windowSizeSeconds")))
                .thenReturn(List.of("CLIENT", "FIXED_WINDOW", "0", "0", "10", "60"));

        RateLimitConfig cfg = clientConfigService.fetchConfig("CLIENT");

        assertEquals("CLIENT", cfg.getClientType());
        assertEquals(Algorithm.FIXED_WINDOW, cfg.getAlgorithm());
        assertEquals(10, cfg.getLimit());
        assertEquals(60, cfg.getWindowSizeSeconds());
    }

//    @Test
//    void fetchConfig_throwsWhenMissing() {
//        when(hashOps.multiGet(eq("ratelimit:config:FOO"), anyList()))
//                .thenReturn(List.of(null, null, null, null, null, null));
//
//        assertThrows(ConfigNotFoundException.class, () -> clientConfigService.fetchConfig("FOO"));
//    }


    @Test
    void setConfig_validatesAndWritesToRedis() throws Exception {
        RateLimitConfig cfg = RateLimitConfig.builder()
                .clientType("C1")
                .algorithm(Algorithm.FIXED_WINDOW)
                .limit(7)
                .windowSizeSeconds(30)
                .build();

        clientConfigService.setConfig("C1", cfg);

        verify(validator).validate("C1", cfg);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Map<String, String>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
        // verify putAll was called on the hashOps mock
        verify(hashOps).putAll(eq("ratelimit:config:C1"), captor.capture());
        Map<String, String> written = captor.getValue();
        assertEquals("C1", written.get("clientType"));
        assertEquals(String.valueOf(7), written.get("limit"));
    }

//    @Test
//    void setConfig_invalid_throws() throws Exception {
//        doThrow(new InvalidConfigException("bad")).when(validator).validate(anyString(), any());
//        RateLimitConfig cfg = RateLimitConfig.builder().clientType("C1").build();
//        assertThrows(InvalidConfigException.class, () -> clientConfigService.setConfig("C1", cfg));
//        verify(hashOps, never()).putAll(anyString(), anyMap());
//    }
}