package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.ExchangeTicker;
import com.btb.exchange.shared.dto.Orders;
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
import org.knowm.xchange.dto.marketdata.Ticker;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;

import java.io.Closeable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;
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
    private final String id;
    private final boolean subscriptionRequired;

    private LeaderSelector leaderSelector;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicBoolean leader = new AtomicBoolean(false);

    private final Counter messageCounter;

    private final Set<CurrencyPair> currencyPairs;

    /**
     * for testing purposes, to subscribe to broadcast events.
     */
    private final Subject<String> messageSent = PublishSubject.create();

    /**
     *
     */
    public ExchangeService(
            CuratorFramework client,
            StreamingExchange exchange,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry registry,
            ObjectMapper objectMapper,
            ApplicationConfig config,
            ExchangeEnum exchangeEnum,
            String id,
            boolean subscriptionRequired,
            String path,
            Set<CurrencyPair> currencyPairs) {
        this(
                null,
                exchange,
                kafkaTemplate,
                registry,
                objectMapper,
                config,
                exchangeEnum,
                id,
                subscriptionRequired,
                currencyPairs);
        this.leaderSelector = new LeaderSelector(client, path, this);
    }

    public ExchangeService(
            LeaderSelector leaderSelector,
            StreamingExchange exchange,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry registry,
            ObjectMapper objectMapper,
            ApplicationConfig config,
            ExchangeEnum exchangeEnum,
            String id,
            boolean subscriptionRequired,
            Set<CurrencyPair> currencyPairs) {
        this.exchange = exchange;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
        this.exchangeEnum = exchangeEnum;
        this.id = id;
        this.subscriptionRequired = subscriptionRequired;
        this.currencyPairs = currencyPairs;
        this.leaderSelector = leaderSelector;

        messageCounter = Counter.builder("backend.exchange.messages")
                .description("indicates number of message received from the given exchange")
                .tag("exchange", exchangeEnum.toString())
                .register(registry);
    }

    void start() {
        init();
        leaderSelector.autoRequeue();
        leaderSelector.start();
    }

    @Override
    public void close() {
        teardown();
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
        }
    }

    void init() {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            // set counter to initial value to let the readers know that the counter is reset as
            // well
            counter.set(1);

            // set symbol, but due to the different Exchange implementations this is not always
            // successful.
            final Collection<CurrencyPair> symbols = symbols(exchange);
            log.info("{} : subscription needed : {}, symbols: {}", exchangeEnum, subscriptionRequired, symbols);
            var subscription = symbols.stream()
                    .reduce(
                            ProductSubscription.create(),
                            ProductSubscription.ProductSubscriptionBuilder::addOrderbook,
                            (psb1, psb2) -> {
                                throw new UnsupportedOperationException();
                            })
                    .build();

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
            return exchange.getExchangeInstruments().stream()
                    .filter(CurrencyPair.class::isInstance)
                    .map(CurrencyPair.class::cast)
                    .filter(currencyPairs::contains)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("exchange:{} : Exception: {}", exchange, e.getMessage());
            // at least return something and give it a try later in the overall flow.
            return currencyPairs;
        }
    }

    private void subscribe(final CurrencyPair currencyPair) {
        log.info("{} : Subscribe: {}", exchangeEnum, currencyPair);
        try {
            exchange.getStreamingMarketDataService()
                    .getTicker(currencyPair)
                    .subscribe(
                            ticker -> process(ticker, currencyPair),
                            throwable -> log.error("Error in ticker subscription", throwable));
        } catch (Exception e) {
            log.warn("Ticker: Exchange:{}, currencypair:{}, exception:{}", exchangeEnum, currencyPair, e.getMessage());
        }
        try {
            exchange.getStreamingMarketDataService()
                    .getOrderBook(currencyPair)
                    .subscribe(
                            orderBook -> process(orderBook, currencyPair),
                            throwable -> log.error("Error in order book subscription", throwable));
        } catch (Exception e) {
            log.warn(
                    "Orderbook: Exchange:{}, currencypair:{}, exception:{}",
                    exchangeEnum,
                    currencyPair,
                    e.getMessage());
        }
    }

    /**
     * for testing purposes
     */
    final Observable<String> subscribe() {
        return messageSent;
    }

    public void process(OrderBook orderBook, @NonNull CurrencyPair currencyPair) {
        if (hasLeadership()) {
            log.trace("Order book: {}", orderBook);
            messageCounter.increment();
            try {
                var future = kafkaTemplate.send(
                        TopicUtils.INPUT_ORDERBOOK,
                        objectMapper.writeValueAsString(new ExchangeOrderBook(
                                counter.getAndIncrement(),
                                LocalTime.now(),
                                exchangeEnum,
                                id,
                                currencyPair,
                                new Orders(orderBook.getAsks(), orderBook.getBids(), config.getMaxOrders()))));

                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        messageSent.onNext(result.getRecordMetadata().topic());
                    } else {
                        log.error("Exception-1", ex);
                    }
                });
            } catch (JsonProcessingException e) {
                log.error("Exception-2", e);
            }
        }
    }

    public void process(Ticker ticker, @NonNull CurrencyPair currencyPair) {
        if (hasLeadership()) {
            log.trace("Ticker: {}", ticker);
            try {
                var future = kafkaTemplate.send(
                        TopicUtils.INPUT_TICKER,
                        objectMapper.writeValueAsString(new ExchangeTicker(
                                counter.getAndIncrement(), LocalTime.now(), exchangeEnum, id, currencyPair, ticker)));

                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        messageSent.onNext(result.getRecordMetadata().topic());
                    } else {
                        log.error("Exception-1", ex);
                    }
                });
            } catch (JsonProcessingException e) {
                log.error("Exception-2", e);
            }
        }
    }

    void teardown() {
        // Disconnect from exchange (blocking to wait for it)
        if (!config.isReplay() && exchange.isAlive()) {
            exchange.disconnect().blockingAwait();
        }
    }
}
