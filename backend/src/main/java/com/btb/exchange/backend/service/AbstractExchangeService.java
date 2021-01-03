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
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;

import javax.annotation.PreDestroy;

/**
 * Generic behavior for all exchanges
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractExchangeService {

    private final StreamingExchange exchange;
    private final ExchangeEnum exchangeEnum;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig config;

    public static final String DEFAULT_VALUE = "";

    /**
     * for testing purposes, to subscribe to the events that are broadcasted.
     * It's 'BehaviorSubject' so we can proces the events, even if  the service is already started via the 'Application Ready event'
     */
    private final Subject<String> messageSent = BehaviorSubject.createDefault(DEFAULT_VALUE);

    /**
     * Initialize the connection with the exchange
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            var subscription = CurrencyPairUtils.CurrencyPairs.stream()
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

    final Observable<String> subscribe() {
        return messageSent;
    }

    public void process(OrderBook orderBook, @NonNull CurrencyPair currencyPair) throws JsonProcessingException {
        log.debug("Order book: {}", orderBook);
        var future = kafkaTemplate.send(TopicUtils.orderBookFull(currencyPair),
                objectMapper.writeValueAsString(new ExchangeOrderBook(exchangeEnum, currencyPair, orderBook)));

        future.addCallback(result -> {
            if (config.isTesting()) {
                messageSent.onNext(result.getRecordMetadata().topic());
            }
        }, e -> log.error("Exception", e));
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
