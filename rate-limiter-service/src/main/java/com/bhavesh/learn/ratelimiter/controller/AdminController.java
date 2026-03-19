package com.bhavesh.learn.ratelimiter.controller;

import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitStatus;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.service.RateLimitStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ClientConfigService clientConfigService;
    private final RateLimitStatusService rateLimitStatusService;

    public AdminController(ClientConfigService clientConfigService, RateLimitStatusService rateLimitStatusService) {
        this.clientConfigService = clientConfigService;
        this.rateLimitStatusService = rateLimitStatusService;
    }

    @PostMapping("/config")
    public ResponseEntity<String> setConfig(@RequestParam String clientType, @RequestBody RateLimitConfig config) {
        try {
            clientConfigService.setConfig(clientType, config);
        } catch (InvalidConfigException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid configuration: " + e.getMessage());
        }

        return ResponseEntity.ok("Configuration added successfully for client type: " + clientType);
    }

    @PutMapping("/config/{clientType}")
    public ResponseEntity<String> updateConfig(@PathVariable String clientType, @RequestBody RateLimitConfig config) {
        try {
            clientConfigService.setConfig(clientType, config);
        } catch (InvalidConfigException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid configuration: " + e.getMessage());
        }
        return ResponseEntity.ok("Configuration updated successfully for client type: " + clientType);
    }

    @GetMapping("/config/{clientType}")
    public ResponseEntity<RateLimitConfig> getConfig(@PathVariable String clientType) {
        try {
            return ResponseEntity.ok(clientConfigService.getConfig(clientType));
        } catch (ConfigNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/config/all")
    public ResponseEntity<Map<String, RateLimitConfig>> getAllConfigs() {
        return ResponseEntity.ok(clientConfigService.getAllConfigs());
    }

    @DeleteMapping("/config/{clientType}")
    public ResponseEntity<String> deleteConfig(@PathVariable String clientType) {
        clientConfigService.deleteConfig(clientType);
        return ResponseEntity.ok("Configuration deleted successfully for client type: " + clientType);
    }

    @GetMapping("/status")
    public ResponseEntity<RateLimitStatus> getRateLimitStatus(
            @RequestParam String clientId,
            @RequestParam String clientType) {
        RateLimitRequest request = RateLimitRequest.builder()
                .clientId(clientId)
                .clientType(clientType)
                .build();
        return ResponseEntity.ok(rateLimitStatusService.getRateLimitStatus(request));
    }
}
