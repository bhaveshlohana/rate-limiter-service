package com.bhavesh.learn.ratelimiter.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void redisTemplateBean_shouldReturnTemplate() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, String> template = redisConfig.redisTemplate(factory);
        assertNotNull(template);
    }
}

