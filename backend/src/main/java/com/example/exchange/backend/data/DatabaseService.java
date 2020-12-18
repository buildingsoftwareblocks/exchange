package com.example.exchange.backend.data;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class DatabaseService {

    private final MessageRepository repository;

    public DatabaseService(MessageRepository repository) {
        this.repository = repository;
    }

    // listen on all orderbook topics
    @Synchronized
    @KafkaListener(topicPattern = "#{T(com.example.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void process(String orderBook) {
        log.trace("Store: {}", orderBook);
        repository.save(Message.builder().created(new Date()).messageType(MessageType.ORDERBOOK).message(orderBook).build());
    }
}
