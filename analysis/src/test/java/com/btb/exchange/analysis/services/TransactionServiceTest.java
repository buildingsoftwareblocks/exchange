package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.ExchangeEnum;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;

class TransactionServiceTest {

    private static final BigDecimal ERROR = BigDecimal.valueOf(0.0005);

    @Test
    void transactionBuyFees() {
        var service = new TransactionService();
        assertThat(service.transactionBuyFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transactionSellFees() {
        var service = new TransactionService();
        assertThat(service.transactionSellFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(150), ERROR)));
    }

    @Test
    void transportationFees() {
        var service = new TransactionService();
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE,  CurrencyPair.BTC_USD),
                is(closeTo(BigDecimal.valueOf(5), ERROR)));
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE,  CurrencyPair.ETH_BTC),
                is(closeTo(BigDecimal.valueOf(4), ERROR)));
        assertThat(service.transportationFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, ExchangeEnum.BINANCE,  CurrencyPair.DASH_BTC),
                is(closeTo(BigDecimal.valueOf(1), ERROR)));
    }
}