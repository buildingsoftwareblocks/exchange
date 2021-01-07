package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.data.CurrencyPairOpportunities;
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

    private static final int MAX_FRESHNESS_SEC = 5;

    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();
    // to prevent working with old data
    private final Map<Key, LocalTime> updated = new ConcurrentHashMap<>();

    public CurrencyPairOpportunities process(ExchangeOrderBook orderBook)  {
        Optional<BigDecimal> askPrice = orderBook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        var key = new Key(orderBook.getExchange(), orderBook.getCurrencyPair());
        asks.put(key, askPrice.orElse(BigDecimal.ZERO));
        updated.put(key, LocalTime.now());
        return findOpportunities(orderBook);
    }

    private CurrencyPairOpportunities findOpportunities(ExchangeOrderBook orderBook) {
        var opportunityBuilder = CurrencyPairOpportunities.builder();
        BigDecimal bid = orderBook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        CurrencyPair currencyPair = orderBook.getCurrencyPair();
        opportunityBuilder.currencyPair(currencyPair);

        LocalTime watermark = LocalTime.now().minusSeconds(MAX_FRESHNESS_SEC);
        for (Map.Entry<Key, BigDecimal> entry : asks.entrySet()) {
            Key k = entry.getKey();
            BigDecimal ask = entry.getValue();
            if (k.currencyPair.equals(currencyPair) && watermark.isBefore(updated.get(k))) {
                var profit = bid.subtract(ask);
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    opportunityBuilder.opportunity(new Opportunity(k.exchange, orderBook.getExchange(), currencyPair, ask, bid, LocalTime.now()));
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
