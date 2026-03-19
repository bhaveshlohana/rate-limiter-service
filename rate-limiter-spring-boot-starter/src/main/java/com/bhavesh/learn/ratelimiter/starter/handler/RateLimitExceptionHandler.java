package com.bhavesh.learn.ratelimiter.starter.handler;

import com.bhavesh.learn.ratelimiter.starter.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }
}
