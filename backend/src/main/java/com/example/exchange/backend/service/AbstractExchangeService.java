package com.example.exchange.backend.service;

import com.example.exchange.shared.dto.ExchangeEnum;
import com.example.exchange.shared.dto.ExchangeOrderBook;
import com.example.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static com.example.exchange.shared.dto.ExchangeEnum.KRAKEN;
import static org.knowm.xchange.currency.CurrencyPair.*;

@Slf4j
public abstract class AbstractExchangeService {

    public static final List<CurrencyPair> CurrencyPairs = Arrays.asList(BTC_USDT, ETH_BTC, DASH_USDT);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AbstractExchangeService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize the connection with the exchange
     */
    protected void init(StreamingExchange exchange, ExchangeEnum exchangeEnum) {
        var subscription = CurrencyPairs.stream().reduce(ProductSubscription.create(), ProductSubscription.ProductSubscriptionBuilder::addOrderbook,
                (psb1, psb2) -> {
                    throw new UnsupportedOperationException();
                }).build();

        exchange.connect(subscription).blockingAwait();

        // Subscribe order book data with the reference to the subscription.
        CurrencyPairs.stream().forEach(cp -> process(exchange, cp, exchangeEnum, TopicUtils.orderBook(cp)));
    }

    final protected void process(StreamingExchange exchange, CurrencyPair currencyPair, ExchangeEnum exchangeEnum, String topic) {
        exchange.getStreamingMarketDataService().getOrderBook(currencyPair).subscribe(orderBook -> process(orderBook, exchangeEnum, topic));
    }

    protected void process(OrderBook orderBook, ExchangeEnum exchange, String topic) throws JsonProcessingException {
        log.info("Order book: {}", orderBook);
        kafkaTemplate.send(topic, objectMapper.writeValueAsString(new ExchangeOrderBook(exchange, orderBook)));
    }
}
