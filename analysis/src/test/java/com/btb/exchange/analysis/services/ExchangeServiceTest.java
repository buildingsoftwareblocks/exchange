package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;

class ExchangeServiceTest {

    private static final BigDecimal ERROR = BigDecimal.valueOf(0.0005);


    @Test
    void transactionBuyFees() {
        var service = new ExchangeService(new ApplicationConfig());
        assertThat(service.transactionBuyFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transactionSellFees() {
        var service = new ExchangeService(new ApplicationConfig());
        assertThat(service.transactionSellFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transportationFees() {
        var service = new ExchangeService(new ApplicationConfig());
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE, CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(5), ERROR)));
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE, CurrencyPair.ETH_BTC),
                is(closeTo(BigDecimal.valueOf(4), ERROR)));
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE, CurrencyPair.DASH_BTC),
                is(closeTo(BigDecimal.valueOf(1), ERROR)));
    }

    @Test
    void validData() {
        var service = new ExchangeService(new ApplicationConfig(false, 0, 0, 0));
        LocalTime now = LocalTime.of(19, 20, 40, 123000000);
        LocalTime time = LocalTime.of(19, 20, 41, 122000000);
        var result = service.validData(ExchangeEnum.KRAKEN, CurrencyPair.BTC_AUD, now, time);
        assertThat(result, is(true));
    }
}