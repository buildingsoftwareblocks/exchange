package com.btb.exchange.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Service
@Slf4j
public class ExchangeService {

    private final StreamingExchange exchange;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NewTopic topic;

    public ExchangeService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, NewTopic topic, StreamingExchange exchange) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.exchange = exchange;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // Connect to the Exchange WebSocket API. Here we use a blocking wait.
        exchange.connect().blockingAwait();
        // Subscribe order book data with the reference to the subscription.
        exchange.getStreamingMarketDataService()
                .getOrderBook(CurrencyPair.BTC_USD)
                .subscribe(this::process);
    }

    public void process(OrderBook orderBook) throws JsonProcessingException {
        log.trace("Order book: {}", orderBook);
        kafkaTemplate.send(topic.name(), objectMapper.writeValueAsString(orderBook));
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
