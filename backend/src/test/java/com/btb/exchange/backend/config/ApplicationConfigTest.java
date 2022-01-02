package com.btb.exchange.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigTest {

    @Test
    void validateIllegalValue() {
        var replayRecording = new ApplicationConfig(true, true, true, 0, false);
        assertThrows(IllegalArgumentException.class, replayRecording::validate);
        var maxOrdersToLow = new ApplicationConfig(false, false, false, -1, false);
        assertThrows(IllegalArgumentException.class, maxOrdersToLow::validate);
    }

    @Test
    void validate() {
        new ApplicationConfig(true, false, true, 5, false).validate();
        new ApplicationConfig(false, false, true, 5, false).validate();
        new ApplicationConfig(false, true, true, 5, false).validate();
    }
}