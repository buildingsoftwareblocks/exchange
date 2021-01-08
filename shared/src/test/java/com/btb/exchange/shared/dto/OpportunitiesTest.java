package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class OpportunitiesTest {

    static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void serializeToJson() throws JsonProcessingException {
        var opportunity1 = new Opportunity(ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE, CurrencyPair.BTC_AUD, BigDecimal.valueOf(100), BigDecimal.valueOf(110), LocalTime.now());
        var opportunity2 = new Opportunity(ExchangeEnum.BITSTAMP, ExchangeEnum.COINBASE, CurrencyPair.ETH_BTC, BigDecimal.valueOf(200), BigDecimal.valueOf(210), LocalTime.now());
        var opportunities = Opportunities.builder().value(opportunity1).value(opportunity2).build();
        var serialized = objectMapper.writeValueAsString(opportunities);
        assertThat("smoke test", serialized, is(notNullValue()));
    }

    @Test
    void deserializeToJson() throws JsonProcessingException {
        LocalTime now = LocalTime.of(19,20,40,123000000);
        var opportunities1 = Opportunities.builder()
                .value(new Opportunity(ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE, CurrencyPair.BTC_AUD, BigDecimal.valueOf(100), BigDecimal.valueOf(110), now))
                .value( new Opportunity(ExchangeEnum.BITSTAMP, ExchangeEnum.COINBASE, CurrencyPair.ETH_BTC, BigDecimal.valueOf(200), BigDecimal.valueOf(210), now))
                .build();
        var serialized1 = objectMapper.writeValueAsString(opportunities1);
        var opportunities2 = objectMapper.readValue(serialized1, Opportunities.class);
        assertThat("smoke test", opportunities2, is(notNullValue()));
        assertThat(opportunities2, is(opportunities1));
    }
}