package com.btb.exchange.backend.data.es;

import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.DTOUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ESDatabaseService {

    private final ESMessageRepository repository;
    private final DTOUtils dtoUtils;

    // for testing purposes, to subscribe to the event that records are saved to the database
    private final Subject<List<String>> stored = PublishSubject.create();

    /**
     *
     */
    public ESDatabaseService(ESMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.dtoUtils = new DTOUtils(objectMapper);
    }

    @KafkaListener(
            topics = TopicUtils.INPUT_ORDERBOOK,
            containerFactory = "batchFactory",
            groupId = "backend.elasticsearch",
            autoStartup = "${backend.es:false}")
    public void store(List<String> messages) {
        log.debug("save {} records", messages.size());
        var records = messages.stream().map(this::createRecord).toList();
        repository.saveAll(records);
        stored.onNext(messages);
    }

    void store(String message) {
        store(List.of(message));
    }

    @SneakyThrows
    Message createRecord(String orderBook) {
        ExchangeOrderBook exchangeOrderBook = dtoUtils.fromDTO(orderBook, ExchangeOrderBook.class);
        return Message.builder()
                .created(new Date())
                .exchange(exchangeOrderBook.getExchange())
                .currencyPair(Objects.toString(exchangeOrderBook.getCurrencyPair()))
                .orders(exchangeOrderBook.getOrders())
                .build();
    }

    Subject<List<String>> subscribe() {
        return stored;
    }
}
