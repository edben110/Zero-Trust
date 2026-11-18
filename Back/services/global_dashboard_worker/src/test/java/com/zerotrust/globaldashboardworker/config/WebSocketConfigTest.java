package com.zerotrust.globaldashboardworker.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketConfigTest {
    @Test
    void permiteSoloOrigenesConfigurados() {
        assertDoesNotThrow(() -> new WebSocketConfig("http://localhost:3001,http://192.0.2.10:3000"));
        assertThrows(IllegalArgumentException.class, () -> new WebSocketConfig("*"));
        assertThrows(IllegalArgumentException.class, () -> new WebSocketConfig(""));
    }
}
