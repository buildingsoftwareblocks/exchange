package com.btb.exchange.analysis.simple;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SimpleExchangeArbitrage {

    private final Map<Key, BigDecimal> bid = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> ask = new ConcurrentHashMap<>();

    public void process(ExchangeOrderBook orderbook) {
        Optional<BigDecimal> askPrice = orderbook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        ask.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), askPrice.orElse(BigDecimal.ZERO));
        bid.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), bidPrice.orElse(BigDecimal.ZERO));
    }

    @Value
    @AllArgsConstructor
    class Key {
        ExchangeEnum exchange;
        String currencyPair;
    }
}
