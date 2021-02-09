package com.btb.exchange.shared.utils;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.util.List;

import static org.knowm.xchange.currency.CurrencyPair.*;

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
        out.writeUTF(currencyPair.toString());
    }

    public static CurrencyPair readData(ObjectDataInput in) throws IOException {
        return new CurrencyPair(in.readUTF());
    }

    public boolean overlap(CurrencyPair cp) {
        for (CurrencyPair i : CurrencyPairs) {
            if ((i.base == cp.base) || (i.base == cp.counter)) {
                return true;
            }
            if ((i.counter == cp.base) || (i.counter == cp.counter)) {
                return true;
            }
        }
        return false;
    }
}
