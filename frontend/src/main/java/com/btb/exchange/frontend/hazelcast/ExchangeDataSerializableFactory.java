package com.btb.exchange.frontend.hazelcast;

import com.btb.exchange.frontend.service.ExchangeService;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ExchangeDataSerializableFactory implements DataSerializableFactory {

    public static final int FACTORY_ID = 1;

    public static final int KEY_TYPE = 1;
    public static final int EXCHANGE_VALUE_TYPE = 2;
    public static final int CURRENYPAIR_VALUE_TYPE = 3;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        switch (typeId) {
            case KEY_TYPE:
                return new ExchangeService.Key();
            case EXCHANGE_VALUE_TYPE:
                return new ExchangeService.ExchangeValue();
            case CURRENYPAIR_VALUE_TYPE:
                return new ExchangeService.CurrencyPairValue();
            default:
                return null;
        }
    }
}
