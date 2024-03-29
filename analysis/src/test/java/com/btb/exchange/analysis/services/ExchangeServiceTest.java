package com.btb.exchange.analysis.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;

import com.btb.exchange.analysis.config.ApplicationConfig;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

class ExchangeServiceTest {

    private static final BigDecimal ERROR = BigDecimal.valueOf(0.0005);

    @Test
    void transactionBuyFees() {
        var config = new ApplicationConfig();
        config.setBuyfees(0.0015);
        var service = new ExchangeService(config);
        assertThat(service.transactionBuyFees(BigDecimal.valueOf(100000)), is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transactionSellFees() {
        var config = new ApplicationConfig();
        config.setSellfees(0.0015);
        var service = new ExchangeService(config);
        assertThat(
                service.transactionSellFees(BigDecimal.valueOf(100000)), is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transportationFees() {
        var config = new ApplicationConfig();
        config.setTransportfees(1);
        var service = new ExchangeService(config);
        assertThat(service.transportationFees(CurrencyPair.BTC_USD), is(closeTo(BigDecimal.valueOf(5), ERROR)));
        assertThat(service.transportationFees(CurrencyPair.ETH_BTC), is(closeTo(BigDecimal.valueOf(4), ERROR)));
        assertThat(service.transportationFees(CurrencyPair.DASH_BTC), is(closeTo(BigDecimal.valueOf(1), ERROR)));
    }

    @Test
    void validData() {
        var config = new ApplicationConfig();
        var service = new ExchangeService(config);
        LocalTime now = LocalTime.of(19, 20, 40, 123000000);
        LocalTime time = LocalTime.of(19, 20, 41, 122000000);
        var result = service.validData(now, time);
        assertThat(result, is(true));
    }
}
