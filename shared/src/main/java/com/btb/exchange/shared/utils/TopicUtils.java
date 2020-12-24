package com.btb.exchange.shared.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

/**
 * Utilities for Kafka topics
 */
@UtilityClass
public class TopicUtils {

    public static final String ORDERBOOK_INPUT_PREFIX = "orderbook.input";

    public static String orderBook(@NonNull CurrencyPair currencyPair) {
        return String.format("%s.%s_%s", ORDERBOOK_INPUT_PREFIX, currencyPair.base, currencyPair.counter);
    }
}
