package com.bhavesh.learn.ratelimiter.starter.config;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private List<RateLimitConfigProperties> configs = new ArrayList<>();

    @Data
    public static class RateLimitConfigProperties {
        private String clientType;
        private Algorithm algorithm;
        private Integer limit;
        private Integer windowSizeSeconds;
        private Integer capacity;
        private Double refillRatePerSecond;
    }
}
