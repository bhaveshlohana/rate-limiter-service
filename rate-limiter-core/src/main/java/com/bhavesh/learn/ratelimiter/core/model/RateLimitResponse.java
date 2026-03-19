package com.bhavesh.learn.ratelimiter.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitResponse {
    private boolean allowed;
    private String reason;
    private int remainingRequests;
}
