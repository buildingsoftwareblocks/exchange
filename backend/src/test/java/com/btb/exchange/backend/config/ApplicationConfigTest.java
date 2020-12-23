package com.btb.exchange.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigTest {

    @Test
    void validateIllegalValue() {
        var config = new ApplicationConfig(true, true);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate() {
        new ApplicationConfig(true, false).validate();
        new ApplicationConfig(false, false).validate();
        new ApplicationConfig(false, true).validate();
        // doesn't throw an exception
    }
}