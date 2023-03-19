package com.btb.exchange.shared.utils;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;

@UtilityClass
public class CurrencyPairUtils {

  public static void writeData(ObjectDataOutput out, CurrencyPair currencyPair) throws IOException {
    out.writeString(currencyPair.toString());
  }

  public static CurrencyPair readData(ObjectDataInput in) throws IOException {
    return new CurrencyPair(in.readString());
  }
}
