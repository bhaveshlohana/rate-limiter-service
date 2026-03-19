package com.bhavesh.learn.ratelimiter.seeder;

import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigSeederTest {

    @Mock
    private ClientConfigService clientConfigService;

    @Test
    void shouldSeedDefaultConfigWhenNotExists() throws Exception {
        when(clientConfigService.getConfig("DEFAULT"))
                .thenThrow(new ConfigNotFoundException("not found"));

        ConfigSeeder configSeeder = new ConfigSeeder(clientConfigService);

        configSeeder.seedDefaultConfig();

        verify(clientConfigService).setConfig(eq("DEFAULT"), any(RateLimitConfig.class));
    }

    @Test
    void shouldSkipSeedingWhenDefaultExists() throws Exception {
        when(clientConfigService.getConfig("DEFAULT"))
                .thenReturn(RateLimitConfig.builder().build());

        ConfigSeeder configSeeder = new ConfigSeeder(clientConfigService);

        configSeeder.seedDefaultConfig();

        verify(clientConfigService).getConfig("DEFAULT");
        verify(clientConfigService, never()).setConfig(any(), any());
    }

    @Test
    void shouldThrowRuntimeWhenSetConfigFails() throws Exception {
        when(clientConfigService.getConfig("DEFAULT"))
                .thenThrow(new ConfigNotFoundException("not found"));

        doThrow(new InvalidConfigException("invalid"))
                .when(clientConfigService).setConfig(eq("DEFAULT"), any(RateLimitConfig.class));

        ConfigSeeder configSeeder = new ConfigSeeder(clientConfigService);

        assertThrows(RuntimeException.class, () -> configSeeder.seedDefaultConfig());

        verify(clientConfigService).setConfig(eq("DEFAULT"), any(RateLimitConfig.class));
    }
}