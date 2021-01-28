package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.hazelcast.ExchangeDataSerializableFactory;
import com.btb.exchange.analysis.services.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class SimpleExchangeArbitrage {

    private final ExchangeService exchangeService;

    private final Map<Key, BigDecimal> bids;
    private final Map<Key, BigDecimal> asks;
    // to prevent working with old data
    private final Map<Key, LocalTime> updated;

    public SimpleExchangeArbitrage(ExchangeService exchangeService, HazelcastInstance hazelcastInstance) {
        this.exchangeService = exchangeService;
        bids = hazelcastInstance.getMap("bids");
        asks = hazelcastInstance.getMap("asks");
        updated = hazelcastInstance.getMap("updated");
    }

    public Opportunities process(ExchangeOrderBook orderBook) {
        Optional<BigDecimal> askPrice = orderBook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderBook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        var key = new Key(orderBook.getExchange(), orderBook.getCurrencyPair());
        bids.put(key, askPrice.orElse(BigDecimal.ZERO));
        asks.put(key, bidPrice.orElse(BigDecimal.ZERO));
        updated.put(key, LocalTime.now());
        return findOpportunities(orderBook);
    }

    /**
     * Find opportunities
     * <p>
     * TODO amount of opportunity should contain amount of incoming order.
     */
    private Opportunities findOpportunities(ExchangeOrderBook orderBook) {
        var opportunitiesBuilder = Opportunities.builder();
        final BigDecimal ask = orderBook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        final BigDecimal bid = orderBook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        final CurrencyPair currencyPair = orderBook.getCurrencyPair();

        asks.entrySet().stream()
                .filter(e -> e.getKey().currencyPair.equals(currencyPair))
                .filter(e -> exchangeService.validData(e.getKey().exchange, e.getKey().currencyPair, updated.get(e.getKey())))
                .filter(e -> bid.subtract(e.getValue()).compareTo(BigDecimal.ZERO) > 0)
                .forEach(e -> opportunitiesBuilder.value(new Opportunity(currencyPair, e.getKey().exchange, e.getValue(), orderBook.getExchange(), bid)));

        bids.entrySet().stream()
                .filter(e -> e.getKey().currencyPair.equals(currencyPair))
                .filter(e -> exchangeService.validData(e.getKey().exchange, e.getKey().currencyPair, updated.get(e.getKey())))
                .filter(e -> e.getValue().subtract(ask).compareTo(BigDecimal.ZERO) > 0)
                .forEach(e -> opportunitiesBuilder.value(new Opportunity(currencyPair, orderBook.getExchange(), ask, e.getKey().getExchange(), e.getValue())));

        return opportunitiesBuilder.build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static public class Key implements IdentifiedDataSerializable {
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
            out.writeUTF(currencyPair.toString());
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            exchange = ExchangeEnum.valueOf(in.readUTF());
            currencyPair = new CurrencyPair(in.readUTF());
        }
    }
}
