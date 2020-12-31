package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final MessageRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationConfig config;
    private final ObjectMapper objectMapper;
    private final ReactiveMongoTemplate mongoTemplate;

    // for testing purposes, to subscribe to the event that records are saved to the database
    private final Subject<Message> stored = PublishSubject.create();

    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    void store(List<String> messages) {
        if (config.isRecording()) {
            var records = messages.stream().map(this::createRecord).collect(Collectors.toList());
            repository.saveAll(records).subscribeOn(Schedulers.io()).subscribe(r -> {
                if (config.isTesting()) {
                    stored.onNext(r);
                }
            });
        }
    }

    void store(String message) {
        store(List.of(message));
    }

    @SneakyThrows
    Message createRecord(String orderBook) {
        ExchangeOrderBook exchangeOrderBook = objectMapper.readValue(orderBook, ExchangeOrderBook.class);
        return Message.builder()
                .created(new Date())
                .exchange(exchangeOrderBook.getExchange())
                .currencyPair(exchangeOrderBook.getCurrencyPair())
                .message(orderBook).build();
    }


    Observable<Message> subscribe() {
        return stored;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndicesAfterStartup() {
        log.info("Start create indexes");
        var mappingContext = mongoTemplate.getConverter().getMappingContext();
        var resolver = new MongoPersistentEntityIndexResolver(mappingContext);

        // consider only entities that are annotated with @Document
        mappingContext.getPersistentEntities().stream()
                .filter(it -> it.isAnnotationPresent(Document.class))
                .forEach(it -> createIndexForEntity(it.getType(), resolver));
        log.info("End create indexes");
    }

    @SneakyThrows
    private void createIndexForEntity(Class<?> entityClass, IndexResolver resolver) {
        var indexOps = mongoTemplate.indexOps(entityClass);

        // count number of indexes
        int size = (int) StreamSupport.stream(resolver.resolveIndexFor(entityClass).spliterator(), false).count();
        var latch = new CountDownLatch(size);
        resolver.resolveIndexFor(entityClass).forEach(i -> indexOps.ensureIndex(i).subscribe(j -> latch.countDown()));
        latch.await();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (config.isReplay()) {
            replayEvents();
        }
    }

    void replayEvents() {
        log.info("Start replay events");
        StopWatch replayWatch = new StopWatch("replayEvents");
        replayWatch.start();
        repository.findAll(Sort.by(Sort.Direction.ASC, "created"))
                .subscribeOn(Schedulers.io())
                .subscribe(m -> {
                    var message = m.getMessage();
                    log.debug("Replay : {}", message);
                    kafkaTemplate.send(TopicUtils.orderBook(m.getCurrencyPair()), message);
                }, t -> log.error("Exception", t), () -> {
                    replayWatch.stop();
                    log.info("End replay events, and took: {}", Duration.ofMillis(replayWatch.getTotalTimeMillis()));
                });
    }
}
