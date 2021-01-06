package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.services.OrderService;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final ObjectMapper objectMapper;
    private final SimpleExchangeArbitrage simpleExchangeArbitrage;
    private final OrderService orderService;

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    public void process(String message) throws JsonProcessingException {
        log.trace("Order book received: {}", message);
        var orderbook = objectMapper.readValue(message, ExchangeOrderBook.class);
        var opportunities = simpleExchangeArbitrage.process(orderbook);
        orderService.processSimpleExchangeArbitrage(opportunities);
    }
}
