package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class OpportunityTest {

    static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void serializeToJson() throws JsonProcessingException, JSONException {
        LocalTime now = LocalTime.of(19, 20, 40, 123456789);
        var opportunity = new Opportunity(CurrencyPair.BTC_AUD, BigDecimal.ZERO, BigDecimal.valueOf(10), ExchangeEnum.KRAKEN, BigDecimal.valueOf(100), ExchangeEnum.BITFINEX, BigDecimal.valueOf(110), now);
        var serialized = objectMapper.writeValueAsString(opportunity);
        assertThat("smoke test", serialized, is(notNullValue()));
        JSONAssert.assertEquals("test localtime", "{'created': '19:20:40.123'}", serialized, JSONCompareMode.LENIENT);
    }

    @Test
    void deserializeToJson() throws JsonProcessingException {
        LocalTime now = LocalTime.of(19, 20, 40, 123000000);
        var opportunity1 = new Opportunity(CurrencyPair.BTC_AUD, BigDecimal.ONE, BigDecimal.valueOf(10), ExchangeEnum.KRAKEN, BigDecimal.valueOf(100), ExchangeEnum.BITFINEX, BigDecimal.valueOf(110), now);
        var serialized1 = objectMapper.writeValueAsString(opportunity1);
        var opportunity2 = objectMapper.readValue(serialized1, Opportunity.class);
        assertThat("smoke test", opportunity2, is(notNullValue()));
        assertThat(opportunity2, is(opportunity1));
    }
}