package com.bhavesh.learn.ratelimiter.core.service;

import com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants;
import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.util.RateLimiterUtils;
import com.bhavesh.learn.ratelimiter.core.validator.RateLimitConfigValidator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClientConfigService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfigValidator rateLimitConfigValidator;

    public ClientConfigService(RedisTemplate<String, String> redisTemplate, RateLimitConfigValidator rateLimitConfigValidator) {
        this.redisTemplate = redisTemplate;
        this.rateLimitConfigValidator = rateLimitConfigValidator;
    }

    public RateLimitConfig getConfig(String clientType) throws ConfigNotFoundException {
        try {
            return fetchConfig(clientType);
        } catch (ConfigNotFoundException e) {
            try {
                return fetchConfig(ApplicationConstants.DEFAULT);
            } catch (ConfigNotFoundException ex) {
                throw new ConfigNotFoundException("No configuration found for client type: " + clientType + " and no default configuration found.");
            }
        }
    }

    public RateLimitConfig fetchConfig(String clientType) throws ConfigNotFoundException {
        String key = RateLimiterUtils.getConfigKey(clientType);
        List<Object> data = redisTemplate.opsForHash().multiGet(key, List.of("clientType", "algorithm", "capacity", "refillRatePerSecond", "limit", "windowSizeSeconds"));
        if (data.stream().allMatch(Objects::isNull)) {
            throw new ConfigNotFoundException("No configuration found for client type: " + clientType);
        }
        String clientTypeFromRedis = data.get(0) != null ? data.get(0).toString() : null;
        Algorithm algorithm = data.get(1) != null ? Algorithm.valueOf(data.get(1).toString()) : null;
        Integer capacity = data.get(2) != null ? Integer.parseInt((String) data.get(2)) : null;
        Double refillRatePerSecond = data.get(3) != null ? Double.parseDouble((String) data.get(3)) : null;
        Integer limit = data.get(4) != null ? Integer.parseInt((String) data.get(4)) : null;
        Integer windowSizeSeconds = data.get(5) != null ? Integer.parseInt((String) data.get(5)) : null;
        return RateLimitConfig.builder()
                .clientType(clientTypeFromRedis)
                .algorithm(algorithm)
                .capacity(capacity)
                .refillRatePerSecond(refillRatePerSecond)
                .limit(limit)
                .windowSizeSeconds(windowSizeSeconds)
                .build();
    }

    public Map<String, RateLimitConfig> getAllConfigs() {
        String key = RateLimiterUtils.getConfigKeyPattern();
        Map<String, RateLimitConfig> configs = new HashMap<>();
        redisTemplate.keys(key).forEach(k -> {
            String clientType = k.replace(ApplicationConstants.RATELIMIT_CONFIG_PREFIX, "");
            try {
                configs.put(clientType, getConfig(clientType));
            } catch (ConfigNotFoundException e) {
                // skip invalid configs
            }
        });
        return configs;
    }

    public void setConfig(String clientType, RateLimitConfig config) throws InvalidConfigException {
        rateLimitConfigValidator.validate(clientType, config);
        String key = RateLimiterUtils.getConfigKey(clientType);
        Map<String, String> updates = new HashMap<>();
        updates.put("clientType", clientType);
        updates.put("algorithm", String.valueOf(config.getAlgorithm()));
        updates.put("capacity", String.valueOf(config.getCapacity()));
        updates.put("refillRatePerSecond", String.valueOf(config.getRefillRatePerSecond()));
        updates.put("limit", String.valueOf(config.getLimit()));
        updates.put("windowSizeSeconds", String.valueOf(config.getWindowSizeSeconds()));
        redisTemplate.opsForHash().putAll(key, updates);
    }

    public void deleteConfig(String clientType) {
        String key = RateLimiterUtils.getConfigKey(clientType);
        redisTemplate.delete(key);
    }
}
