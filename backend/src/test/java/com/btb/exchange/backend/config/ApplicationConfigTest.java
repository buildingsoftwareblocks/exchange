package com.btb.exchange.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigTest {

    @Test
    void validateIllegalState() {
        var config = new ApplicationConfig();
        config.setRecording(true);
        config.setReplay(true);
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}