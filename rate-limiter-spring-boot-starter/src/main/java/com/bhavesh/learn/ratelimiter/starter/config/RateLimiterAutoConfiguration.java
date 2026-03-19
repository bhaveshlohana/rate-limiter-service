package com.bhavesh.learn.ratelimiter.starter.config;

import com.bhavesh.learn.ratelimiter.core.core.FixedWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.SlidingWindowRateLimiter;
import com.bhavesh.learn.ratelimiter.core.core.TokenBucketRateLimiter;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.core.service.RateLimiterFactory;
import com.bhavesh.learn.ratelimiter.core.validator.RateLimitConfigValidator;
import com.bhavesh.learn.ratelimiter.starter.annotation.RateLimit;
import com.bhavesh.learn.ratelimiter.starter.aspect.RateLimitAspect;
import com.bhavesh.learn.ratelimiter.starter.handler.RateLimitExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitConfigValidator rateLimitConfigValidator() {
        return new RateLimitConfigValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientConfigService clientConfigService(RedisTemplate<String, String> redisTemplate,
                                                   RateLimitConfigValidator validator) {
        return new ClientConfigService(redisTemplate, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public FixedWindowRateLimiter fixedWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new FixedWindowRateLimiter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowRateLimiter slidingWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new SlidingWindowRateLimiter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenBucketRateLimiter tokenBucketRateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new TokenBucketRateLimiter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterFactory rateLimiterFactory(ClientConfigService clientConfigService,
                                                 FixedWindowRateLimiter fixedWindowRateLimiter,
                                                 SlidingWindowRateLimiter slidingWindowRateLimiter,
                                                 TokenBucketRateLimiter tokenBucketRateLimiter,
                                                 MeterRegistry meterRegistry) {
        return new RateLimiterFactory(clientConfigService, fixedWindowRateLimiter,
                slidingWindowRateLimiter, tokenBucketRateLimiter, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimiterFactory rateLimiterFactory,
                                           HttpServletRequest request) {
        return new RateLimitAspect(rateLimiterFactory, request);
    }

    @Bean
    public ApplicationRunner configSeeder(RateLimiterProperties properties,
                                          ClientConfigService clientConfigService) {
        return args -> {
            for (RateLimiterProperties.RateLimitConfigProperties config : properties.getConfigs()) {
                clientConfigService.setConfig(config.getClientType(),
                        RateLimitConfig.builder()
                                .algorithm(config.getAlgorithm())
                                .limit(config.getLimit())
                                .windowSizeSeconds(config.getWindowSizeSeconds())
                                .capacity(config.getCapacity())
                                .refillRatePerSecond(config.getRefillRatePerSecond())
                                .build()
                );
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitExceptionHandler rateLimitExceptionHandler() {
        return new RateLimitExceptionHandler();
    }

}