package com.btb.exchange.analysis.hazelcast;

import com.btb.exchange.analysis.simple.SimpleExchangeArbitrage;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ExchangeDataSerializableFactory implements DataSerializableFactory {

    public static final int FACTORY_ID = 1;

    public static final int KEY_TYPE = 1;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        return switch (typeId) {
            case KEY_TYPE -> new SimpleExchangeArbitrage.Key();
            default -> null;
        };
    }
}
