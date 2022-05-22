package com.btb.exchange.shared.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

@Slf4j
@UtilityClass
public class IDUtils {

    public String generateID() {
        return RandomStringUtils.randomNumeric(3);
    }
}
