package com.btb.exchange.shared.utils;

import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


class TopicUtilsTest {

    @Test
    void orderBook() {
        assertThat(TopicUtils.orderBook(CurrencyPair.BTC_USD), equalTo("orderbook.input.BTC_USD"));
    }
}