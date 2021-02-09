package com.btb.exchange.analysis.hazelcast;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeCPKey implements IdentifiedDataSerializable {
    private ExchangeEnum exchange;
    private CurrencyPair currencyPair;

    @Override
    public int getFactoryId() {
        return ExchangeDataSerializableFactory.FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return ExchangeDataSerializableFactory.KEY_TYPE;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(exchange.toString());
        CurrencyPairUtils.writeData(out, currencyPair);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        exchange = ExchangeEnum.valueOf(in.readUTF());
        currencyPair = CurrencyPairUtils.readData(in);
    }
}
