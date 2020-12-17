package com.example.exchange.shared.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

@UtilityClass
public class TopicUtils {

    public static String orderBook(@NonNull CurrencyPair currencyPair) {
        return String.format("orderbook.%s_%s", currencyPair.base, currencyPair.counter);
    }
}
