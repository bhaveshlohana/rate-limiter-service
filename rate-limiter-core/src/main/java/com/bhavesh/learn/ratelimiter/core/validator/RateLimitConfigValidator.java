package com.bhavesh.learn.ratelimiter.core.validator;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RateLimitConfigValidator {

    public void validate(String clientId, RateLimitConfig config) throws InvalidConfigException {
        if (config == null) {
            throw new InvalidConfigException("Configuration cannot be null");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new InvalidConfigException("Client ID cannot be null or empty");
        }
        if (config.getAlgorithm() == null) {
            throw new InvalidConfigException("Algorithm must be specified, the value must be one of: " + Arrays.toString(Algorithm.values()));
        }
        switch (config.getAlgorithm()) {
            case FIXED_WINDOW, SLIDING_WINDOW -> {
                if (config.getLimit() == null || config.getLimit() <= 0) {
                    throw new InvalidConfigException("Limit must be a positive integer for FIXED_WINDOW and SLIDING_WINDOW algorithms");
                }
                if (config.getWindowSizeSeconds() == null || config.getWindowSizeSeconds() <= 0) {
                    throw new InvalidConfigException("Window size in seconds must be a positive integer for FIXED_WINDOW and SLIDING_WINDOW algorithms");
                }
                config.setCapacity(0); // Set capacity to 0 for non TOKEN_BUCKET algorithms
                config.setRefillRatePerSecond(0.0); // Set refill rate to 0 for non TOKEN_BUCKET algorithms
            }
            case TOKEN_BUCKET -> {
                if (config.getCapacity() == null || config.getCapacity() <= 0) {
                    throw new InvalidConfigException("Capacity must be a positive integer for TOKEN_BUCKET algorithm");
                }
                if (config.getRefillRatePerSecond() == null || config.getRefillRatePerSecond() <= 0) {
                    throw new InvalidConfigException("Refill rate per second must be a positive number for TOKEN_BUCKET algorithm");
                }
                config.setLimit(0); // Set limit to 0 for non FIXED_WINDOW and SLIDING_WINDOW algorithms
                config.setWindowSizeSeconds(0); // Set window size to 0 for non FIXED_WINDOW and SLIDING_WINDOW algorithms
            }
        }
    }
}
