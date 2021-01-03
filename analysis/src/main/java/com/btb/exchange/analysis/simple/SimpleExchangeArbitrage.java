package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.costs.TransactionService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    private final Map<Key, BigDecimal> bids = new ConcurrentHashMap<>();
    private final Map<Key, BigDecimal> asks = new ConcurrentHashMap<>();

    /**
     * TODO make it asynchronous
     */
    public void process(ExchangeOrderBook orderbook) throws JsonProcessingException {
        Optional<BigDecimal> askPrice = orderbook.getOrderBook().getAsks().stream().findFirst().map(LimitOrder::getLimitPrice);
        Optional<BigDecimal> bidPrice = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);

        asks.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), askPrice.orElse(BigDecimal.ZERO));
        bids.put(new Key(orderbook.getExchange(), orderbook.getCurrencyPair()), bidPrice.orElse(BigDecimal.ZERO));
        findOportunities(orderbook);
    }

    private void findOportunities(ExchangeOrderBook orderbook) {
        var opportunitiesBuilder = Opportunities.builder();
        Optional<BigDecimal> bidPrice = orderbook.getOrderBook().getBids().stream().findFirst().map(LimitOrder::getLimitPrice);
        CurrencyPair currencyPair = orderbook.getCurrencyPair();

        if (bidPrice.isPresent()) {
                var bid = bidPrice.get();
                for (Map.Entry<Key, BigDecimal> entry : asks.entrySet()) {
                    Key k = entry.getKey();
                    BigDecimal v = entry.getValue();
                    if (k.currencyPair.equals(currencyPair) && ) {
                    }
            }

        }

//        var opportunitiesBuilder = Opportunities.builder();
//
//        Map.Entry<Key, BigDecimal> bidKey = null;
//        BigDecimal higestBid = BigDecimal.ZERO;
//        Map.Entry<Key, BigDecimal> askKey = null;
//        BigDecimal lowestAsk = BigDecimal.valueOf(Long.MAX_VALUE);
//
//        for (Map.Entry<Key, BigDecimal> entry : bid.entrySet()) {
//            Key k = entry.getKey();
//            BigDecimal v = entry.getValue();
//            if (k.currencyPair.equals(currencyPair) && higestBid.compareTo(v) == -1) {
//                higestBid = v;
//                bidKey = entry;
//            }
//        }
//        for (Map.Entry<Key, BigDecimal> entry : ask.entrySet()) {
//            Key k = entry.getKey();
//            BigDecimal v = entry.getValue();
//            if (k.currencyPair.equals(currencyPair) && lowestAsk.compareTo(v) == 1) {
//                lowestAsk = v;
//                askKey = entry;
//            }
//        }
//
//        if (higestBid.compareTo(lowestAsk) == 1) {
//            log.info("Arbitrage oportunity: bid {} : ask {}", bidKey, askKey);
//            opportunitiesBuilder.value(new Opportunity(askKey.getKey().getExchange(), bidKey.getKey().getExchange(), cp.toString(), lowestAsk, higestBid));
//        }
//
//        var opportunities = opportunitiesBuilder.build();
//
//        if (!opportunities.getValues().isEmpty()) {
//            kafkaTemplate.send(TopicUtils.OPPORTUNITIES, objectMapper.writeValueAsString(opportunities));
//        }
    }


    @Value
    @AllArgsConstructor
    static class Key {
        ExchangeEnum exchange;
        CurrencyPair currencyPair;
    }
}
