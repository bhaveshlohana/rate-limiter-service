package com.bhavesh.learn.ratelimiter.seeder;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.bhavesh.learn.ratelimiter.core.constants.ApplicationConstants.DEFAULT;

@Component
@Profile("!test") // Exclude this seeder from test profile to avoid interference with test cases
public class ConfigSeeder {
    public final static Logger logger = LoggerFactory.getLogger(ConfigSeeder.class);

    private final ClientConfigService clientConfigService;

    public ConfigSeeder(ClientConfigService clientConfigService) {
        this.clientConfigService = clientConfigService;
    }

    @PostConstruct
    public void seedDefaultConfig() {
        logger.info("=== ConfigSeeder running ===");
        try {
            clientConfigService.getConfig(DEFAULT);
        } catch (ConfigNotFoundException e) {
            logger.info("Default config not found, seeding default config...");
            try {
                clientConfigService.setConfig(DEFAULT,
                        RateLimitConfig.builder()
                                .algorithm(Algorithm.FIXED_WINDOW)
                                .limit(10)
                                .windowSizeSeconds(60)
                                .build()
                );
            } catch (InvalidConfigException ice) {
                logger.info("Failed to seed default config: " + ice.getMessage());
                throw new RuntimeException("Failed to seed default config: " + ice.getMessage());
            }
        }
    }
}
