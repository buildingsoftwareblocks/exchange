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

    private final Map<Key, BigDecimal> bids = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();

    public CurrencyPairOpportunities process(ExchangeOrderBook orderbook)  {
        Optional<BigDecimal> askPrice = orderbook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        asks.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), askPrice.orElse(BigDecimal.ZERO));
        bids.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), bidPrice.orElse(BigDecimal.ZERO));
        return findOportunities(orderbook);
    }

    private CurrencyPairOpportunities findOportunities(ExchangeOrderBook orderbook) {
        var opportunityBuilder = CurrencyPairOpportunities.builder();
        BigDecimal bid = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        CurrencyPair currencyPair = orderbook.getCurrencyPair();
        opportunityBuilder.currencyPair(currencyPair);

        for (Map.Entry<Key, BigDecimal> entry : asks.entrySet()) {
            Key k = entry.getKey();
            BigDecimal ask = entry.getValue();
            if (k.currencyPair.equals(currencyPair)) {
                var profit = bid.subtract(ask);
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    opportunityBuilder.opportunity(new Opportunity(k.exchange, orderbook.getExchange(), currencyPair, ask, bid, LocalTime.now()));
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
