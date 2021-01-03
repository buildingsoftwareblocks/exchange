package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.services.TransactionService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleExchangeArbitrage {

    private final Map<Key, BigDecimal> bids = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();

    @Getter
    private final Subject<Opportunities> opportunities = PublishSubject.create();

    public void process(ExchangeOrderBook orderbook) throws JsonProcessingException {
        Optional<BigDecimal> askPrice = orderbook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        asks.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), askPrice.orElse(BigDecimal.ZERO));
        bids.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), bidPrice.orElse(BigDecimal.ZERO));
        findOportunities(orderbook);
    }

    private void findOportunities(ExchangeOrderBook orderbook) {
        var opportunityBuilder = Opportunities.builder();
        BigDecimal bid = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice).orElse(BigDecimal.ZERO);
        CurrencyPair currencyPair = orderbook.getCurrencyPair();
        opportunityBuilder.currencyPair(currencyPair);

        for (Map.Entry<Key, BigDecimal> entry : asks.entrySet()) {
            Key k = entry.getKey();
            BigDecimal ask = entry.getValue();
            if (k.currencyPair.equals(currencyPair)) {
                var profit = bid.subtract(ask);
                if (profit.compareTo(BigDecimal.ZERO) == 1) {
                    opportunityBuilder.value(new Opportunity(k.exchange, orderbook.getExchange(), currencyPair, ask, bid));
                }
            }
        }
        var opportunity = opportunityBuilder.build();
        if (!opportunity.getValues().isEmpty()) {
            // tell the subscribers that there is new information.
            opportunities.onNext(opportunity);
        }
    }


    @Value
    @AllArgsConstructor
    static class Key {
        ExchangeEnum exchange;
        CurrencyPair currencyPair;
    }
}
