package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.dto.marketdata.OrderBook;

import java.util.Collections;
import java.util.Date;

import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class ExchangeOrderBookTest {

    static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeToJson() throws JsonProcessingException {
        var orderBook = new ExchangeOrderBook(1, ExchangeEnum.BITSTAMP, getFirstCurrencyPair(), new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        var serialized = objectMapper.writeValueAsString(orderBook);
        assertThat("smoke test", serialized, is(notNullValue()));
    }

    @Test
    void deserializeToJson() throws JsonProcessingException {
        var orderBook1 = new ExchangeOrderBook(1, ExchangeEnum.BITSTAMP, getFirstCurrencyPair(), new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        var serialized1 = objectMapper.writeValueAsString(orderBook1);
        var orderBook2 = objectMapper.readValue(serialized1, ExchangeOrderBook.class);
        assertThat("smoke test", orderBook2, is(notNullValue()));
    }
}