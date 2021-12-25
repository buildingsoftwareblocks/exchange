package com.btb.exchange.shared.utils;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.util.List;

import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;
import static org.knowm.xchange.currency.CurrencyPair.ETH_BTC;

@UtilityClass
public class CurrencyPairUtils {
    public static final List<CurrencyPair> CurrencyPairs = List.of(BTC_USD, ETH_BTC);

    public static CurrencyPair getFirstCurrencyPair() {
        return CurrencyPairs.get(0);
    }

    public static CurrencyPair getSecondCurrencyPair() {
        return CurrencyPairs.get(1);
    }

    public static void writeData(ObjectDataOutput out, CurrencyPair currencyPair) throws IOException {
        out.writeString(currencyPair.toString());
    }

    public static CurrencyPair readData(ObjectDataInput in) throws IOException {
        return new CurrencyPair(in.readString());
    }
}
