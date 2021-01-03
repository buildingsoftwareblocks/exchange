package com.btb.exchange.shared.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

/**
 * Utilities for Kafka topics
 */
@UtilityClass
public class TopicUtils {

    public static final String ORDERBOOK_INPUT_FULL_PREFIX = "orderbook.input.full";
    public static final String OPPORTUNITIES = "order.opportunities";

    public static String orderBookFull(@NonNull String currencyPair) {
        return orderBookFull(new CurrencyPair(currencyPair));
    }

    public static String orderBookFull(@NonNull CurrencyPair currencyPair) {
        return String.format("%s.%s_%s", ORDERBOOK_INPUT_FULL_PREFIX, currencyPair.base, currencyPair.counter);
    }

}
