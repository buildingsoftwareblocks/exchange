package com.btb.exchange.shared.utils;

import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopicUtilsTest {

    @Test
    void orderBookString() {
        assertThat(TopicUtils.orderBook(CurrencyPair.BTC_USD.toString()), equalTo("orderbook.input.BTC_USD"));
    }

    @Test
    void orderBookStringNull() {
        assertThrows(NullPointerException.class, ()-> TopicUtils.orderBook((String)null));
    }

    @Test
    void orderBook() {
        assertThat(TopicUtils.orderBook(CurrencyPair.BTC_USD), equalTo("orderbook.input.BTC_USD"));
    }

    @Test
    void orderBookNull() {
        assertThrows(NullPointerException.class, ()-> TopicUtils.orderBook((CurrencyPair) null));
    }
}