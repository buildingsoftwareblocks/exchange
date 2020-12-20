package com.btb.exchange.backend.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final MessageRepository repository;
    // for testing purposes
    @Getter
    private CountDownLatch latch = new CountDownLatch(1);

    @Synchronized
    @KafkaListener(topics = "orderbook")
    void store(String orderBook) {
        log.info("Store order book: {}", orderBook);
        repository.save(Message.builder().messageType(MessageType.ORDERBOOK).created(new Date()).message(orderBook).build());
        latch.countDown();
    }
}
