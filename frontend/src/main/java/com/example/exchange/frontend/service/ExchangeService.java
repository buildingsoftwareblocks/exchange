package com.example.exchange.frontend.service;

import com.example.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;

    @Synchronized
    @KafkaListener(topics = "#{ T(com.example.exchange.shared.utils.TopicUtils).orderBook( T(org.knowm.xchange.currency.CurrencyPair).BTC_USDT)}")
    void process(String orderBook) throws JsonProcessingException {
        log.info("Order book: {}", orderBook);
        var exchangeOrderBook = objectMapper.readValue(orderBook, ExchangeOrderBook.class);
        template.convertAndSend("/topic/orderbook", exchangeOrderBook);
    }
}
