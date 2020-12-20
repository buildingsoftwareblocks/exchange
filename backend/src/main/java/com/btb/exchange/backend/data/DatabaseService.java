package com.btb.exchange.backend.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final MessageRepository repository;
    @Getter
    private CountDownLatch latch = new CountDownLatch(1);

    @KafkaListener(topics = "orderbook")
    void store(String orderBook) {
        repository.save(Message.builder().messageType(MessageType.ORDERBOOK).created(new Date()).message(orderBook).build());
        latch.countDown();
    }
}
