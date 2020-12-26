package com.btb.exchange.analysis.simple;

import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final ObjectMapper objectMapper;
    private final SimpleExchangeArbitrage simpleExchangeArbitrage;

    @Synchronized
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void process(String message) throws JsonProcessingException {
        log.info("Order book received: {}", message);
        var orderbook = objectMapper.readValue(message, ExchangeOrderBook.class);
        simpleExchangeArbitrage.process(orderbook);
    }

}
