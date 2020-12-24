package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
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
    private final ApplicationConfig config;
    private final ObjectMapper objectMapper;

    // for testing purposes, to subscribe to the event that records are saved to the database
    private final Subject<Message> stored = PublishSubject.create();

    @Synchronized
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void store(String orderBook) throws JsonProcessingException {
        if (config.isRecording()) {
            log.info("Store order book: {}", orderBook);
            var exchangeOrderBook = objectMapper.readValue(orderBook, ExchangeOrderBook.class);
            repository.save(Message.builder()
                    .created(new Date())
                    .currencyPair(exchangeOrderBook.getCurrencyPair())
                    .message(orderBook).build()).subscribe(stored::onNext);
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
            kafkaTemplate.send(TopicUtils.orderBook(m.getCurrencyPair()), message);
        });
    }
}
