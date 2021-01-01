package com.btb.exchange.shared.utils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;
import static org.knowm.xchange.currency.CurrencyPair.ETH_BTC;

class CurrencyPairUtilsTest {

    @Test
    void getFirstCurrencyPair() {
        assertThat(CurrencyPairUtils.getFirstCurrencyPair(), equalTo(BTC_USD));
    }

    @Test
    void getSecondCurrencyPair() {
        assertThat(CurrencyPairUtils.getSecondCurrencyPair(), equalTo(ETH_BTC));
    }
}