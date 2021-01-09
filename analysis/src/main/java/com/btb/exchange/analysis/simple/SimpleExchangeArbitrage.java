package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.data.CurrencyPairOpportunities;
import com.btb.exchange.analysis.services.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleExchangeArbitrage {

    private final ExchangeService exchangeService;

    private final Map<Key, BigDecimal> bids = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();
    // to prevent working with old data
    private final Map<Key, LocalTime> updated = new ConcurrentHashMap<>();

    public CurrencyPairOpportunities process(ExchangeOrderBook orderBook)  {
        Optional<BigDecimal> askPrice = orderBook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderBook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        var key = new Key(orderBook.getExchange(), orderBook.getCurrencyPair());
        bids.put(key, askPrice.orElse(BigDecimal.ZERO));
        asks.put(key, bidPrice.orElse(BigDecimal.ZERO));
        updated.put(key, LocalTime.now());
        return findOpportunities(orderBook);
    }

    private CurrencyPairOpportunities findOpportunities(ExchangeOrderBook orderBook) {
        var opportunityBuilder = CurrencyPairOpportunities.builder();
        final BigDecimal ask = orderBook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        final BigDecimal bid = orderBook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        final CurrencyPair currencyPair = orderBook.getCurrencyPair();
        opportunityBuilder.currencyPair(currencyPair);

        for (Map.Entry<Key, BigDecimal> entry : asks.entrySet()) {
            Key k = entry.getKey();
            BigDecimal a = entry.getValue();
            if (k.currencyPair.equals(currencyPair)) {
                if (exchangeService.validData(k.exchange, k.currencyPair, updated.get(k))) {
                    var profit = bid.subtract(a);
                    if (profit.compareTo(BigDecimal.ZERO) > 0) {
                        opportunityBuilder.opportunity(new Opportunity(currencyPair, profit, k.exchange, a, orderBook.getExchange(), bid));
                    }
                } else {
                    log.warn("Stale data from {}:{} : {}", k.exchange, k.currencyPair, updated.get(k));
                }
            }
        }

        for (Map.Entry<Key, BigDecimal> entry : bids.entrySet()) {
            Key k = entry.getKey();
            BigDecimal b = entry.getValue();
            if (k.currencyPair.equals(currencyPair)) {
                if (exchangeService.validData(k.exchange, k.currencyPair, updated.get(k))) {
                    var profit = b.subtract(ask);
                    if (profit.compareTo(BigDecimal.ZERO) > 0) {
                        opportunityBuilder.opportunity(new Opportunity(currencyPair, profit, k.exchange, ask, orderBook.getExchange(), b));
                    }
                } else {
                    log.warn("Stale data from {}:{} : {}", k.exchange, k.currencyPair, updated.get(k));
                }
            }
        }

       return opportunityBuilder.build();
    }

    @Value
    @AllArgsConstructor
    static class Key {
        ExchangeEnum exchange;
        CurrencyPair currencyPair;
    }
}
