package com.btb.exchange.frontend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private final SimpMessagingTemplate template;

    @KafkaListener(topics = "orderbook")
    void process(String orderBook) {
        log.info("Order book: {}", orderBook);
        template.convertAndSend("/topic/orderbook", orderBook);
    }
}
