package com.bhavesh.learn.ratelimiter.core.model;

import com.bhavesh.learn.ratelimiter.core.domain.Algorithm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitStatus {
    private String clientId;
    private String clientType;
    private Algorithm algorithm;
    private Integer requestsInWindow;    // Fixed/Sliding Window
    private Double currentTokens;        // Token Bucket
    private Integer remainingRequests;
    private Long windowResetsInSeconds;  // TTL of the key
}
