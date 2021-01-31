package com.btb.exchange.frontend.hazelcast;

import com.btb.exchange.frontend.service.ExchangeService;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ExchangeDataSerializableFactory implements DataSerializableFactory {

    public static final int FACTORY_ID = 1;

    public static final int KEY_TYPE = 1;
    public static final int EXCHANGE_VALUE_TYPE = 2;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        return switch (typeId) {
            case KEY_TYPE -> new ExchangeService.Key();
            case EXCHANGE_VALUE_TYPE -> new ExchangeService.ExchangeValue();
            default -> null;
        };
    }
}
