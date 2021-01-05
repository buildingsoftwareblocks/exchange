package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.ExchangeEnum;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;

class TransactionServiceTest {

    @Test
    void transactionBuyFees() {
        var service = new TransactionService();
        assertThat(service.transactionBuyFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD), is(closeTo(BigDecimal.valueOf(100), BigDecimal.valueOf(0.0005))));
    }

    @Test
    void transactionSellFees() {
        var service = new TransactionService();
        assertThat(service.transactionSellFees(BigDecimal.valueOf(100000), ExchangeEnum.KRAKEN, CurrencyPair.BTC_USD), is(closeTo(BigDecimal.valueOf(100), BigDecimal.valueOf(0.0005))));
    }
}