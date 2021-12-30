package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Orders;
import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.utils.CloseableUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.exceptions.ExchangeException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;

import java.io.Closeable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class ExchangeService extends LeaderSelectorListenerAdapter implements Closeable {

    private final StreamingExchange exchange;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig config;

    private final ExchangeEnum exchangeEnum;
    private final boolean subscriptionRequired;

    private final LeaderSelector leaderSelector;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicBoolean leader = new AtomicBoolean(false);

    private final Counter messageCounter;

    private final int currencyCount;
    private final Set<String> currencies;

    /**
     * for testing purposes, to subscribe to broadcast events.
     * It's 'BehaviorSubject' so we can process the events, even if  the service is already started via the 'Application Ready event'
     */
    private final Subject<String> messageSent = PublishSubject.create();

    /**
     *
     */
    public ExchangeService(CuratorFramework client, ExecutorService executor,
                           StreamingExchange exchange, KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry registry,
                           ObjectMapper objectMapper, ApplicationConfig config,
                           ExchangeEnum exchangeEnum, boolean subscriptionRequired, String path,
                           int currencyCount, Set<String> currencies) {
        this.exchange = exchange;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.config = config;

        this.exchangeEnum = exchangeEnum;
        this.subscriptionRequired = subscriptionRequired;

        this.currencyCount = currencyCount;
        this.currencies = currencies;

        messageCounter = Counter.builder("backend.exchange.messages")
                .description("indicates number of message received from the given exchange")
                .tag("exchange", exchangeEnum.toString())
                .register(registry);

        leaderSelector = new LeaderSelector(client, path, executor, this);
        leaderSelector.autoRequeue();
    }

    void start() {
        leaderSelector.start();
    }

    @Override
    public void close() {
        CloseableUtils.closeQuietly(leaderSelector);
    }

    boolean hasLeadership() {
        return leaderSelector.hasLeadership();
    }

    void interruptLeadership() {
        if (leader.get()) {
            log.info("{} interrupted", exchangeEnum);
            leaderSelector.interruptLeadership();
        }
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
            leader.set(true);
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException i) {
            log.info("Interrupted {} : {}", exchangeEnum, i.getMessage());
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            log.error("Exception", t);
        } finally {
            log.info("Relinquishing leadership - {}", exchangeEnum);
            leader.set(false);
            teardown();
        }
    }

    void init() {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            // set counter to initial value to let the readers know that the counter is reset as well
            counter.set(1);

            // set symbol, but due to the different Exchange implementations this is not always successful.
            final Collection<CurrencyPair> symbols = symbols(exchange);
            log.info("{} : subscription needed : {}, symbols: {}", exchangeEnum, subscriptionRequired, symbols);
            var subscription = symbols.stream()
                    .reduce(ProductSubscription.create(),
                            ProductSubscription.ProductSubscriptionBuilder::addOrderbook,
                            (psb1, psb2) -> {
                                throw new UnsupportedOperationException();
                            }).build();

            if (subscriptionRequired) {
                exchange.connect(subscription).blockingAwait();
            } else {
                exchange.connect().blockingAwait();
            }

            // Subscribe order book data with the reference to the currency pair.
            symbols.forEach(this::subscribe);
        }
    }

    Collection<CurrencyPair> symbols(StreamingExchange exchange) {
        try {
            var results = exchange.getExchangeSymbols().stream().filter(this::overlap).limit(currencyCount).collect(Collectors.toSet());
            // to be sure that the default currency pairs are their as well
            results.addAll(CurrencyPairUtils.CurrencyPairs);
            return results;
        } catch (Exception e) {
            log.info("Exception: {}", e.getMessage());
            return CurrencyPairUtils.CurrencyPairs;
        }
    }

    boolean overlap(CurrencyPair cp) {
        return currencies.contains(cp.base.getCurrencyCode()) || currencies.contains(cp.counter.getCurrencyCode());
    }

    private void subscribe(final CurrencyPair currencyPair) {
        try {
            log.info("{} : Subscribe: {}", exchangeEnum, currencyPair);
            exchange.getStreamingMarketDataService().getOrderBook(currencyPair)
                    .subscribe(orderBook -> process(orderBook, currencyPair), throwable -> log.error("Error in trade subscription", throwable));
        } catch (ExchangeException e) {
            log.warn("Exception", e);
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }


    /**
     * for testing purposes
     */
    final Observable<String> subscribe() {
        return messageSent;
    }

    public void process(OrderBook orderBook, @NonNull CurrencyPair currencyPair) {
        log.trace("Order book: {}", orderBook);
        messageCounter.increment();
        try {
            var future = kafkaTemplate.send(TopicUtils.orderBook(currencyPair),
                    objectMapper.writeValueAsString(new ExchangeOrderBook(counter.getAndIncrement(),
                            LocalTime.now(), exchangeEnum, currencyPair,
                            new Orders(orderBook.getTimeStamp(), orderBook.getAsks(), orderBook.getBids(), config.getMaxOrders()))));

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
        // Disconnect from exchange (blocking to wait for it)
        if (!config.isReplay() && exchange.isAlive()) {
            exchange.disconnect().blockingAwait();
        }
    }
}

