package com.btb.exchange.backend.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApplicationConfigTest {

  @Test
  void validateIllegalValue() {
    var replayRecording = new ApplicationConfig(true, true, true, 0);
    assertThrows(IllegalArgumentException.class, replayRecording::validate);
    var maxOrdersToLow = new ApplicationConfig(false, false, false, -1);
    assertThrows(IllegalArgumentException.class, maxOrdersToLow::validate);
  }

  @Test
  void validate() {
    new ApplicationConfig(true, false, true, 5).validate();
    new ApplicationConfig(false, false, true, 5).validate();
    new ApplicationConfig(false, true, true, 5).validate();
  }
}
