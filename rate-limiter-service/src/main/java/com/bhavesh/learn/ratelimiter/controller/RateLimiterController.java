package com.bhavesh.learn.ratelimiter.controller;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.service.RateLimiterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limiter")
public class RateLimiterController {

    private final RateLimiterFactory rateLimiterFactory;

    public RateLimiterController(RateLimiterFactory rateLimiterFactory) {
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> getRateLimitResponse(@RequestBody RateLimitRequest rateLimitRequest) {
        try {
            RateLimitResponse rateLimitResponse = rateLimiterFactory.applyRateLimiting(rateLimitRequest);
            if (rateLimitResponse.isAllowed()) {
                return ResponseEntity.ok(rateLimitResponse);
            }
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(rateLimitResponse);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
