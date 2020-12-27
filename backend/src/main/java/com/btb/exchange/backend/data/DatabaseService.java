package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void store(String orderBook) throws JsonProcessingException {
        if (config.isRecording()) {
            log.info("Store order book: {}", orderBook);
            var exchangeOrderBook = objectMapper.readValue(orderBook, ExchangeOrderBook.class);
            var saved = repository.save(Message.builder()
                    .created(new Date())
                    .exchange(exchangeOrderBook.getExchange())
                    .currencyPair(exchangeOrderBook.getCurrencyPair())
                    .message(orderBook).build());
            synchronized (stored) {
                log.info("save ready");
                saved.subscribeOn(Schedulers.io()).subscribe(stored::onNext);
            }
        }
    }

    Observable<Message> subscribe() {
        return stored;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndicesAfterStartup() throws InterruptedException {
        log.info("Start create indexes");
        var mappingContext = mongoTemplate.getConverter().getMappingContext();
        var resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        createIndexForEntity(Message.class, resolver);
        log.info("End create indexes");
    }

    private void createIndexForEntity(Class<?> entityClass, IndexResolver resolver) throws InterruptedException {
        var indexOps = mongoTemplate.indexOps(entityClass);

        // count number of indexes
        final List<IndexDefinition> indexes = new ArrayList<>();
        resolver.resolveIndexFor(entityClass).forEach(indexes::add);

        var latch = new CountDownLatch(indexes.size());
        resolver.resolveIndexFor(entityClass).forEach(i -> {
            indexOps.ensureIndex(i);
            latch.countDown();
        });
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
            log.debug("Replay: {}", message);
            kafkaTemplate.send(TopicUtils.orderBook(m.getCurrencyPair()), message);
        }, t -> log.error("Exception", t), () -> {
            replayWatch.stop();
            log.info("End replay events, and took: {}", Duration.ofMillis(replayWatch.getTotalTimeMillis()));
        });
    }
}
