package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.IAtomicReference;
import com.hazelcast.cp.ISemaphore;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.btb.exchange.shared.dto.ExchangeEnum.KRAKEN;
import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;

/**
 * Handle Exchanges
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";

    public static final String HAZELCAST_ORDERBOOKS = "orderBooks";
    public static final String HAZELCAST_OPPORTUNITIES = "opportunities";

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;
    private final HazelcastInstance hazelcastInstance;

    @Value("${frontend.refreshrate:1000}")
    private int refreshRate;

    @Value("${frontend.opportunities:true}")
    private boolean showOpportunities;

    @Setter
    private ExchangeEnum exchange = KRAKEN;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<String> sent = PublishSubject.create();

    private IAtomicReference<String> orderBookRef;
    private IAtomicLong orderBookCounter;
    private ISemaphore orderBookSemaphore;
    private IAtomicReference<String> opportunityRef;
    private IAtomicLong opportunityCounter;
    private ISemaphore opportunitySemaphore;

    @PostConstruct
    void init() {
        orderBookRef = hazelcastInstance.getCPSubsystem().getAtomicReference(HAZELCAST_ORDERBOOKS);
        orderBookCounter = hazelcastInstance.getCPSubsystem().getAtomicLong(HAZELCAST_ORDERBOOKS);
        orderBookSemaphore = hazelcastInstance.getCPSubsystem().getSemaphore(HAZELCAST_ORDERBOOKS);
        opportunityRef = hazelcastInstance.getCPSubsystem().getAtomicReference(HAZELCAST_OPPORTUNITIES);
        opportunityCounter = hazelcastInstance.getCPSubsystem().getAtomicLong(HAZELCAST_OPPORTUNITIES);
        opportunitySemaphore = hazelcastInstance.getCPSubsystem().getSemaphore(HAZELCAST_OPPORTUNITIES);
        orderBookRef.clear();
        opportunityRef.clear();
        orderBookCounter.set(-1);
        opportunityCounter.set(-1);
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    void processOrderBooks(List<String> messages) {
        log.debug("process {} messages", messages.size());
        messages.stream()
                .map(this::transformOrderBook)
                .filter(o -> o.getExchange().equals(exchange) && (o.getCurrencyPair().equals(getFirstCurrencyPair())))
                .reduce((a, b) -> b)
                .ifPresent(this::update);
    }

    ExchangeOrderBook transformOrderBook(String message) {
        try {
            return objectMapper.readValue(message, ExchangeOrderBook.class);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
        return null;
    }

    String transform(ExchangeOrderBook orderBook) {
        try {
            return objectMapper.writeValueAsString(orderBook);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
        return null;
    }

    Opportunities transformOpportunities(String message) {
        try {
            return objectMapper.readValue(message, Opportunities.class);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
        return null;
    }

    String transform(Opportunities opportunities) {
        try {
            return objectMapper.writeValueAsString(opportunities);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
        return null;
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).OPPORTUNITIES}", containerFactory = "batchFactory")
    void processOpportunities(List<String> messages) {
        // only get the last value and add it to the reference
        messages.stream()
                .map(this::transformOpportunities)
                .reduce((a, b) -> b)
                .filter(opportunities -> !opportunities.getValues().isEmpty())
                .ifPresent(this::update);
    }

    Observable<String> subscribe() {
        return sent;
    }

    void update(Opportunities opportunities) {
        try {
            opportunitySemaphore.acquire();
            if (opportunities.getOrder() == 0 || opportunityCounter.get() < opportunities.getOrder()) {
                opportunityRef.set(transform(opportunities));
                opportunityCounter.set(opportunities.getOrder());
            } else {
                log.info("out of sync: {}", opportunities.getOrder());
            }
        } catch (InterruptedException e) {
            log.info("Exception", e);
        } finally {
            opportunitySemaphore.release();
        }
    }

    void update(ExchangeOrderBook orderBook) {
        try {
            orderBookSemaphore.acquire();
            if (orderBook.getOrder() == 0 || orderBookCounter.get() < orderBook.getOrder()) {
                orderBookRef.set(transform(orderBook));
                orderBookCounter.set(orderBook.getOrder());
            } else {
                log.info("out of sync: {}", orderBook.getOrder());
            }
        } catch (InterruptedException e) {
            log.info("Exception", e);
        } finally {
            orderBookSemaphore.release();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        // send order books
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!orderBookRef.isNull()) {
                        var message = orderBookRef.get();
                        log.debug("Tick : {}", message);
                        template.convertAndSend(WEBSOCKET_ORDERBOOK, message);
                        sent.onNext(message);
                    }
                });

        if (showOpportunities) {
            // send opportunities
            Observable.interval(refreshRate * 2L, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                    .subscribe(e -> {
                        if (!opportunityRef.isNull()) {
                            log.debug("Send opportunities: {}", opportunityRef.get());
                            template.convertAndSend(WEBSOCKET_OPPORTUNITIES, opportunityRef.get());
                        }
                    });
        }
    }

    /**
     * Show something even the replay of events is over.
     */
    @SneakyThrows
    @EventListener
    public void handleSessionConnected(SessionSubscribeEvent event) {
        log.info("Session connected: {}", event);
        if (!orderBookRef.isNull()) {
            template.convertAndSend(WEBSOCKET_ORDERBOOK, orderBookRef.get());
        }
    }
}
