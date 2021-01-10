package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.services.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleExchangeArbitrage {

    private final ExchangeService exchangeService;

    private final Map<Key, BigDecimal> bids = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();
    // to prevent working with old data
    private final Map<Key, LocalTime> updated = new ConcurrentHashMap<>();

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
     *
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

    @Value
    @AllArgsConstructor
    static class Key {
        ExchangeEnum exchange;
        CurrencyPair currencyPair;
    }
}
