package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.utils.CloseableUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;

import java.io.Closeable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ExchangeService extends LeaderSelectorListenerAdapter implements Closeable {

    private final StreamingExchange exchange;
    private final ExchangeEnum exchangeEnum;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig config;

    private final LeaderSelector leaderSelector;
    private final Semaphore mutex;
    private final LeaderService leaderService;
    private final AtomicLong counter = new AtomicLong();

    /**
     * for testing purposes, to subscribe to broadcast events.
     * It's 'BehaviorSubject' so we can process the events, even if  the service is already started via the 'Application Ready event'
     */
    private final Subject<String> messageSent = PublishSubject.create();

    /**
     *
     */
    public ExchangeService(CuratorFramework client, LeaderService leaderService, StreamingExchange exchange, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                           ApplicationConfig config,
                           ExchangeEnum exchangeEnum,
                           String path, Semaphore mutex) {
        this.mutex = mutex;
        this.leaderService = leaderService;
        this.exchange = exchange;
        this.exchangeEnum = exchangeEnum;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.config = config;

        leaderSelector = new LeaderSelector(client, path, this);
        leaderSelector.autoRequeue();
    }

    public void start() {
        leaderSelector.start();
    }

    @Override
    public void close() {
        CloseableUtils.closeQuietly(leaderSelector);
    }

    boolean hasLeadership() {
        return leaderSelector.hasLeadership();
    }

    ExchangeEnum leaderOf() {
        if (hasLeadership()) {
            return exchangeEnum;
        } else {
            return null;
        }
    }

    @Override
    public void takeLeadership(CuratorFramework client) {
        try {
            log.info("Take leadership - {}", exchangeEnum);
            init();
            mutex.acquireUninterruptibly();
        } finally {
            log.info("Relinquishing leadership - {}", exchangeEnum);
            teardown();
            mutex.release();
            leaderService.acquire(this);
        }
    }

    void init() {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            // set counter to initial value to let the readers know that the counter is reset as well
            counter.set(1);
            var subscription = exchange.getExchangeSymbols().stream()
                    .filter(CurrencyPairUtils.CurrencyPairs::contains)
                    .reduce(ProductSubscription.create(),
                            ProductSubscription.ProductSubscriptionBuilder::addOrderbook,
                            (psb1, psb2) -> {
                                throw new UnsupportedOperationException();
                            }).build();

            exchange.connect(subscription).blockingAwait();

            // Subscribe order book data with the reference to the currency pair.
            CurrencyPairUtils.CurrencyPairs.forEach(this::subscribe);
        }
    }

    private void subscribe(CurrencyPair currencyPair) {
        exchange.getStreamingMarketDataService().getOrderBook(currencyPair)
                .subscribe(orderBook -> process(orderBook, currencyPair), throwable -> log.error("Error in trade subscription", throwable));
    }

    /**
     * for testing purposes
     */
    final Observable<String> subscribe() {
        return messageSent;
    }

    public void process(OrderBook orderBook, @NonNull CurrencyPair currencyPair) {
        log.trace("Order book: {}", orderBook);
        try {
            var future = kafkaTemplate.send(TopicUtils.orderBook(currencyPair),
                    objectMapper.writeValueAsString(new ExchangeOrderBook(counter.getAndIncrement(), exchangeEnum, currencyPair, orderBook)));

            future.addCallback(result -> {
                if (config.isTesting()) {
                    messageSent.onNext(result.getRecordMetadata().topic());
                }
            }, e -> log.error("Exception-1", e));
        } catch (JsonProcessingException e) {
            log.error("Exception-2", e);
        }
    }

    void teardown() {
        if (!config.isReplay()) {
            // Disconnect from exchange (blocking to wait for it)
            exchange.disconnect().blockingAwait();
        }
    }
}

