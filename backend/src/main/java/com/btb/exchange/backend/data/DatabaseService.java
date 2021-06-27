package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.ISemaphore;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class DatabaseService {

    public static final String HAZELCAST_DB = "database";

    private final MessageRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationConfig config;
    private final ObjectMapper objectMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final ISemaphore semaphore;

    // for testing purposes, to subscribe to the event that records are saved to the database
    private final Subject<Message> stored = PublishSubject.create();

    public DatabaseService(MessageRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                           ApplicationConfig config, ObjectMapper objectMapper, ReactiveMongoTemplate mongoTemplate,
                           HazelcastInstance hazelcastInstance) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
        semaphore = hazelcastInstance.getCPSubsystem().getSemaphore(HAZELCAST_DB);
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    public void store(List<String> messages) {
        if (config.isRecording()) {
            log.debug("save {} records", messages.size());
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
                .data(orderBook).build();
    }


    Observable<Message> subscribe() {
        return stored;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndicesAfterStartup() {
        if (config.isReplay() || config.isRecording()) {
            boolean permit = false;
            try {
                // make sure that only 1 backend instance inits the indices
                semaphore.acquire();
                permit = true;
                log.info("Start create indexes");
                var mappingContext = mongoTemplate.getConverter().getMappingContext();
                var resolver = new MongoPersistentEntityIndexResolver(mappingContext);

                // consider only entities that are annotated with @Document
                mappingContext.getPersistentEntities().stream()
                        .filter(it -> it.isAnnotationPresent(Document.class))
                        .forEach(it -> createIndexForEntity(it.getType(), resolver));
                log.info("End create indexes");
            } catch (InterruptedException e) {
                log.warn("Interrupted", e);
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            } finally {
                if (permit) {
                    semaphore.release();
                }
            }
        }
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
                    var data = m.getData();
                    log.debug("Replay : {}", data);
                    kafkaTemplate.send(TopicUtils.orderBook(m.getCurrencyPair()), data);
                }, t -> log.error("Exception", t), () -> {
                    replayWatch.stop();
                    log.info("End replay events, and took: {}", Duration.ofMillis(replayWatch.getTotalTimeMillis()));
                });
    }
}
