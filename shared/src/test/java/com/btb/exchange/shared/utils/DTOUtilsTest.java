package com.btb.exchange.shared.utils;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;

import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

class DTOUtilsTest {

    static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void OpportunitiesString() {
        final var dtoUtils = new DTOUtils(objectMapper, true);

        var obj1 = createOpportunities();
        var serialized1 = dtoUtils.to(obj1);
        var obj2 = dtoUtils.from(serialized1, Opportunities.class);
        assertThat("smoke test", obj2, is(notNullValue()));
        assertThat(obj2, is(obj1));
    }

    @Test
    void ExchangeOrderBookString() {
        final var dtoUtils = new DTOUtils(objectMapper, true);

        var obj1 = createExchangeOrderBook();
        var serialized1 = dtoUtils.to(obj1);
        var obj2 = dtoUtils.from(serialized1, ExchangeOrderBook.class);
        assertThat("smoke test", obj2, is(notNullValue()));
        assertThat(obj2, is(obj1));
    }

    @Test
    void OpportunitiesByte() {
        final var dtoUtils = new DTOUtils(objectMapper, false);

        var obj1 = createOpportunities();
        var serialized1 = dtoUtils.to(obj1);
        var obj2 = dtoUtils.from(serialized1, Opportunities.class);
        assertThat("smoke test", obj2, is(notNullValue()));
        assertThat(obj2, is(obj1));
    }

    @Test
    void ExchangeOrderBookByte() {
        final var dtoUtils = new DTOUtils(objectMapper, false);

        var obj1 = createExchangeOrderBook();
        var serialized1 = dtoUtils.to(obj1);
        var obj2 = dtoUtils.from(serialized1, ExchangeOrderBook.class);
        assertThat("smoke test", obj2, is(notNullValue()));
        assertThat(obj2, is(obj1));
    }


    Opportunities createOpportunities() {
        LocalTime now = LocalTime.of( 19, 20, 40, 123000000);
        var opportunities = Opportunities.builder()
                .order(1)
                .timestamp(now)
                .value(new Opportunity(CurrencyPair.BTC_AUD, BigDecimal.ONE, BigDecimal.valueOf(10), ExchangeEnum.KRAKEN, BigDecimal.valueOf(100), ExchangeEnum.BITFINEX, BigDecimal.valueOf(110), now))
                .value(new Opportunity(CurrencyPair.ETH_BTC, BigDecimal.ONE, BigDecimal.valueOf(9), ExchangeEnum.BITSTAMP, BigDecimal.valueOf(200), ExchangeEnum.COINBASE, BigDecimal.valueOf(210), now))
                .build();
        return opportunities;
    }

    ExchangeOrderBook createExchangeOrderBook() {
        var orderBook = new ExchangeOrderBook(1, LocalTime.now(), ExchangeEnum.BITSTAMP, getFirstCurrencyPair(),
                new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        return orderBook;
    }

}