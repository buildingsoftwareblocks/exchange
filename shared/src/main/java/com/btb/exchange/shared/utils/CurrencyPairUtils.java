package com.btb.exchange.shared.utils;

import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.List;

import static org.knowm.xchange.currency.CurrencyPair.*;

@UtilityClass
public class CurrencyPairUtils {
    public static final List<CurrencyPair> CurrencyPairs = List.of(BTC_USDT, ETH_BTC, DASH_USDT);

}
