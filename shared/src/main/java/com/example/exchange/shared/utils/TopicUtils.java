package com.example.exchange.shared.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

@UtilityClass
public class TopicUtils {

    public static String ORDERBOOK_INPUT_PREFIX  = "orderbook.input";

    public static String orderBook(@NonNull CurrencyPair currencyPair) {
        return String.format("%s.%s_%s", ORDERBOOK_INPUT_PREFIX, currencyPair.base, currencyPair.counter);
    }
}
