package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final MessageRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewTopic topic;
    private final ApplicationConfig config;
    // for testing purposes, to subscribe to the event that records are saved to the database
    private final Subject<Message> stored = PublishSubject.create();

    @Synchronized
    @KafkaListener(topics = "orderbook")
    void store(String orderBook) {
        if (config.isRecording()) {
            log.info("Store order book: {}", orderBook);
            repository.save(Message.builder().created(new Date()).message(orderBook).build()).subscribe(stored::onNext);
        }
    }

    Observable<Message> subscribe() {
        return stored;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (config.isReplay()) {
            replayEvents();
        }
    }

    void replayEvents() {
        repository.findAll(Sort.by(Sort.Direction.ASC, "created")).subscribe(m -> {
            var message = m.getMessage();
            log.info("Replay: {}", message);
            kafkaTemplate.send(topic.name(), message);
        });
    }
}
