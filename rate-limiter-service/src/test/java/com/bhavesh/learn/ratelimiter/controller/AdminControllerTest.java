package com.bhavesh.learn.ratelimiter.controller;

import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitConfig;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitStatus;
import com.bhavesh.learn.ratelimiter.core.service.ClientConfigService;
import com.bhavesh.learn.ratelimiter.service.RateLimitStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private ClientConfigService clientConfigService;
    private RateLimitStatusService rateLimitStatusService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        clientConfigService = Mockito.mock(ClientConfigService.class);
        rateLimitStatusService = Mockito.mock(RateLimitStatusService.class);
        AdminController controller = new AdminController(clientConfigService, rateLimitStatusService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void setConfig_happyPath_returnsOk() throws Exception {
        RateLimitConfig config = RateLimitConfig.builder().clientType("t1").build();

        mockMvc.perform(post("/api/admin/config?clientType=t1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Configuration added successfully")));
    }

    @Test
    void setConfig_invalidConfig_returnsBadRequest() throws Exception {
        RateLimitConfig config = RateLimitConfig.builder().clientType("t1").build();
        doThrow(new InvalidConfigException("invalid")).when(clientConfigService).setConfig(eq("t1"), any(RateLimitConfig.class));

        mockMvc.perform(post("/api/admin/config?clientType=t1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid configuration")));
    }

    @Test
    void getConfig_notFound_returns404() throws Exception {
        when(clientConfigService.getConfig("t1")).thenThrow(new ConfigNotFoundException("nope"));

        mockMvc.perform(get("/api/admin/config/t1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllConfigs_returnsMap() throws Exception {
        when(clientConfigService.getAllConfigs()).thenReturn(Map.of("t1", RateLimitConfig.builder().clientType("t1").build()));

        mockMvc.perform(get("/api/admin/config/all"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteConfig_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/config/t1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deleted successfully")));
    }

    @Test
    void getRateLimitStatus_returnsOk() throws Exception {
        when(rateLimitStatusService.getRateLimitStatus(any())).thenReturn(RateLimitStatus.builder().clientId("u1").clientType("t1").build());

        mockMvc.perform(get("/api/admin/status?clientId=u1&clientType=t1"))
                .andExpect(status().isOk());
    }
}
