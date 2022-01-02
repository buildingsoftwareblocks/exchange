package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.hazelcast.ExchangeCPKey;
import com.btb.exchange.analysis.services.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.btb.exchange.shared.dto.Order;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@Slf4j
public class SimpleExchangeArbitrage {

    private final ExchangeService exchangeService;

    private final Map<ExchangeCPKey, BigDecimal> bids;
    private final Map<ExchangeCPKey, BigDecimal> asks;
    // to prevent working with old data
    private final Map<ExchangeCPKey, LocalTime> updated;

    public SimpleExchangeArbitrage(ExchangeService exchangeService, HazelcastInstance hazelcastInstance) {
        this.exchangeService = exchangeService;
        bids = hazelcastInstance.getMap("bids");
        asks = hazelcastInstance.getMap("asks");
        updated = hazelcastInstance.getMap("simpleexchangearbitrage.updated");
    }

    public Opportunities process(List<ExchangeOrderBook> orderBooks) {
        var opportunitiesList = orderBooks.stream().map(this::process).toList();
        return merge(opportunitiesList);
    }

    /**
     * Merge opportunities of same currency pair, from, to together.
     */
    Opportunities merge(List<Opportunities> opportunitiesList) {
        var opportunitiesBuilder = Opportunities.builder();
        var opportunities = opportunitiesList.stream().flatMap(o -> o.getValues().stream()).toList();
        var distinct = opportunities.stream()
                .filter(distinctByKeys(Opportunity::getCurrencyPair, Opportunity::getFrom, Opportunity::getTo))
                .toList();
        var timestamp = distinct.stream().map(Opportunity::getCreated).reduce((a, b) -> a).orElse(null);
        return opportunitiesBuilder.values(distinct).timestamp(timestamp).build();
    }

    @SafeVarargs
    private static <T> Predicate<T> distinctByKeys(Function<? super T, ?>... keyExtractors) {
        final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();

        return t -> {
            final List<?> keys = Arrays.stream(keyExtractors).map(ke -> ke.apply(t)).toList();
            return seen.putIfAbsent(keys, Boolean.TRUE) == null;
        };
    }

    Opportunities process(ExchangeOrderBook orderBook) {
        Optional<BigDecimal> askPrice = orderBook.getOrders().getAsks().stream().findFirst().map(Order::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderBook.getOrders().getBids().stream().findFirst().map(Order::getLimitPrice);

        var key = new ExchangeCPKey(orderBook.getExchange(), orderBook.getCurrencyPair());
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
        var opportunitiesBuilder = Opportunities.builder().timestamp(orderBook.getTimestamp());
        final BigDecimal ask = orderBook.getOrders().getAsks().stream().findFirst().map(Order::getLimitPrice).orElse(BigDecimal.ZERO);
        final BigDecimal bid = orderBook.getOrders().getBids().stream().findFirst().map(Order::getLimitPrice).orElse(BigDecimal.ZERO);
        final CurrencyPair currencyPair = orderBook.getCurrencyPair();

        asks.entrySet().stream()
                .filter(e -> e.getKey().getCurrencyPair().equals(currencyPair))
                .filter(e -> exchangeService.validData(e.getKey().getExchange(), e.getKey().getCurrencyPair(), updated.get(e.getKey())))
                .filter(e -> bid.subtract(e.getValue()).compareTo(BigDecimal.ZERO) > 0)
                .forEach(e -> opportunitiesBuilder.value(new Opportunity(currencyPair, e.getKey().getExchange(),
                        e.getValue(), orderBook.getExchange(), bid, orderBook.getTimestamp())));

        if (ask.compareTo(BigDecimal.ZERO) > 0) {
            bids.entrySet().stream()
                    .filter(e -> e.getKey().getCurrencyPair().equals(currencyPair))
                    .filter(e -> exchangeService.validData(e.getKey().getExchange(), e.getKey().getCurrencyPair(), updated.get(e.getKey())))
                    .filter(e -> e.getValue().subtract(ask).compareTo(BigDecimal.ZERO) > 0)
                    .forEach(e -> opportunitiesBuilder.value(new Opportunity(currencyPair, orderBook.getExchange(),
                            ask, e.getKey().getExchange(), e.getValue(), orderBook.getTimestamp())));
        }
        return opportunitiesBuilder.build();
    }
}
