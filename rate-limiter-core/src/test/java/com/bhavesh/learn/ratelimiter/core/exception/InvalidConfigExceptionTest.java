package com.bhavesh.learn.ratelimiter.core.exception;

import com.bhavesh.learn.ratelimiter.core.exception.InvalidConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidConfigExceptionTest {

    @Test
    void message_isStored() {
        InvalidConfigException e = new InvalidConfigException("msg");
        assertEquals("msg", e.getMessage());
    }
}

