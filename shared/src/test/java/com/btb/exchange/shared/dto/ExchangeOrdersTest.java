package com.btb.exchange.shared.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ExchangeOrdersTest {

    static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void serializeToJson() throws JsonProcessingException {
        var orderBook = new ExchangeOrderBook(
                1,
                LocalTime.now(),
                ExchangeEnum.BITSTAMP,
                "123",
                BTC_USD,
                new Orders(Collections.emptyList(), Collections.emptyList(), 0));
        var serialized = objectMapper.writeValueAsString(orderBook);
        assertThat("smoke test", serialized, is(notNullValue()));
    }

    @Test
    void deserializeToJson() throws JsonProcessingException {
        var orderBook1 = new ExchangeOrderBook(
                1,
                LocalTime.now(),
                ExchangeEnum.BITSTAMP,
                "123",
                BTC_USD,
                new Orders(Collections.emptyList(), Collections.emptyList(), 1));
        var serialized1 = objectMapper.writeValueAsString(orderBook1);
        var orderBook2 = objectMapper.readValue(serialized1, ExchangeOrderBook.class);
        assertThat("smoke test", orderBook2, is(notNullValue()));
    }
}
