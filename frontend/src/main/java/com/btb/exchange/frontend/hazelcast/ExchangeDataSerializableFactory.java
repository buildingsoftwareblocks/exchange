package com.btb.exchange.frontend.hazelcast;

import com.btb.exchange.frontend.service.ExchangeService;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ExchangeDataSerializableFactory implements DataSerializableFactory {

    public static final int FACTORY_ID = 1;

    public static final int EXCHANGE_KEY_TYPE = 1;
    public static final int EXCHANGE_CP_KEY_TYPE = 2;
    public static final int EXCHANGE_VALUE_TYPE = 3;
    public static final int ORDER_BOOK_DATA_TYPE = 4;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        return switch (typeId) {
            case EXCHANGE_KEY_TYPE -> new ExchangeService.ExchangeKey();
            case EXCHANGE_CP_KEY_TYPE -> new ExchangeService.ExchangeCPKey();
            case EXCHANGE_VALUE_TYPE -> new ExchangeService.ExchangeValue();
            case ORDER_BOOK_DATA_TYPE -> new ExchangeService.OrderBookData();
            default -> null;
        };
    }
}
