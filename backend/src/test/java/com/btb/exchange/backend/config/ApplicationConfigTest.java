package com.btb.exchange.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigTest {

    @Test
    void validateIllegalValue() {
        var config = new ApplicationConfig(true, true, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate() {
        new ApplicationConfig(true, false, false).validate();
        new ApplicationConfig(false, false, false).validate();
        new ApplicationConfig(false, true, false).validate();
        // doesn't throw an exception
    }
}