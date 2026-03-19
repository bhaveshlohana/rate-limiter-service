package com.bhavesh.learn.ratelimiter.core.exception;

import com.bhavesh.learn.ratelimiter.core.exception.ConfigNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigNotFoundExceptionTest {

    @Test
    void message_isStored() {
        ConfigNotFoundException e = new ConfigNotFoundException("nope");
        assertEquals("nope", e.getMessage());
    }
}

