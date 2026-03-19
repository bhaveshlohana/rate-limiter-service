package com.bhavesh.learn.ratelimiter.controller;

import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.service.RateLimiterFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimiterControllerTest {

    private RateLimiterFactory rateLimiterFactory;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        rateLimiterFactory = Mockito.mock(RateLimiterFactory.class);
        RateLimiterController controller = new RateLimiterController(rateLimiterFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void whenAllowed_thenReturns200AndBody() throws Exception {
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(true)
                .reason(null)
                .remainingRequests(5)
                .build();

        when(rateLimiterFactory.applyRateLimiting(any(RateLimitRequest.class))).thenReturn(response);

        RateLimitRequest req = RateLimitRequest.builder().clientId("u1").clientType("t1").build();

        mockMvc.perform(post("/api/rate-limiter/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void whenRejected_thenReturns429() throws Exception {
        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(false)
                .reason("limit reached")
                .remainingRequests(0)
                .build();

        when(rateLimiterFactory.applyRateLimiting(any(RateLimitRequest.class))).thenReturn(response);

        RateLimitRequest req = RateLimitRequest.builder().clientId("u1").clientType("t1").build();

        mockMvc.perform(post("/api/rate-limiter/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }
}
