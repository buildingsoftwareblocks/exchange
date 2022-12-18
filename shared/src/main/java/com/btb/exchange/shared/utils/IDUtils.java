package com.btb.exchange.shared.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Random;

@Slf4j
@UtilityClass
public class IDUtils {

    private static final Random RANDOM = new SecureRandom();

    public String generateID() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }
}
