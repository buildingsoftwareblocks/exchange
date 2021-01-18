package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.DTOUtils;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.IAtomicReference;
import com.hazelcast.cp.ISemaphore;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
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
public class ExchangeService {

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";

    public static final String HAZELCAST_ORDERBOOKS = "orderBooks";
    public static final String HAZELCAST_OPPORTUNITIES = "opportunities";

    private final SimpMessagingTemplate template;
    private final DTOUtils dtoUtils;

    @Value("${frontend.refreshrate:1000}")
    private int refreshRate;

    @Value("${frontend.opportunities:true}")
    private boolean showOpportunities;

    private ExchangeEnum exchange = KRAKEN;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<String> sent = PublishSubject.create();

    private final ReferenceData orderBookRef;
    private final ReferenceData opportunityRef;

    public ExchangeService(SimpMessagingTemplate template, HazelcastInstance hazelcastInstance, ObjectMapper objectMapper) {
        this.template = template;
        this.dtoUtils = new DTOUtils(objectMapper);
        orderBookRef = new ReferenceData(hazelcastInstance, HAZELCAST_ORDERBOOKS);
        opportunityRef = new ReferenceData(hazelcastInstance, HAZELCAST_OPPORTUNITIES);
    }

    void init() {
        orderBookRef.init();
        opportunityRef.init();
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    void processOrderBooks(List<String> messages) {
        log.debug("process {} messages", messages.size());
        messages.stream()
                .map(o -> dtoUtils.fromDTO(o, ExchangeOrderBook.class))
                .filter(o -> o.getExchange().equals(exchange) && (o.getCurrencyPair().equals(getFirstCurrencyPair())))
                // pick the last element
                .reduce((a, b) -> b)
                .ifPresent(orderBook -> update(orderBookRef, orderBook.getOrder(), dtoUtils.toDTO(orderBook)));
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).OPPORTUNITIES}", containerFactory = "batchFactory")
    void processOpportunities(List<String> messages) {
        // only get the last value and add it to the reference
        messages.stream()
                .map(o -> dtoUtils.fromDTO(o, Opportunities.class))
                // pick the last element
                .reduce((a, b) -> b)
                .filter(opportunities -> !opportunities.getValues().isEmpty())
                .ifPresent(opportunities -> update(opportunityRef, opportunities.getOrder(), dtoUtils.toDTO(opportunities)));
    }

    Observable<String> subscribe() {
        return sent;
    }

    /**
     * Update reference data in a atomic way
     */
    void update(ReferenceData data, long orderNr, String message) {
        try {
            data.semaphore.acquire();
            if ((orderNr == 0) || (data.counter.get() < orderNr)) {
                data.ref.set(message);
                data.counter.set(orderNr);
            } else {
                log.info("out of sync ({}): {} vs {}", data.name, data.counter.get(), orderNr);
            }
        } catch (InterruptedException e) {
            log.info("Exception", e);
        } finally {
            data.semaphore.release();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        // send order books if there is data
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!orderBookRef.ref.isNull()) {
                        var message = orderBookRef.ref.get();
                        log.debug("Tick : {}", message);
                        template.convertAndSend(WEBSOCKET_ORDERBOOK, message);
                        sent.onNext(message);
                    }
                });

        if (showOpportunities) {
            // send opportunities if there is data
            Observable.interval(refreshRate * 2L, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                    .subscribe(e -> {
                        if (!opportunityRef.ref.isNull()) {
                            log.debug("Send opportunities: {}", opportunityRef.ref.get());
                            template.convertAndSend(WEBSOCKET_OPPORTUNITIES, opportunityRef.ref.get());
                        }
                    });
        }
    }

    /**
     * Show something even the replay of events is over.
     */
    @EventListener
    public void handleSessionConnected(SessionSubscribeEvent event) {
        log.info("Session connected: {}", event);
        if (!orderBookRef.ref.isNull()) {
            template.convertAndSend(WEBSOCKET_ORDERBOOK, orderBookRef.ref.get());
        }
        if (showOpportunities && !opportunityRef.ref.isNull()) {
            template.convertAndSend(WEBSOCKET_OPPORTUNITIES, opportunityRef.ref.get());
        }
    }

    /**
     * Change to given exchange to show on the GUI
     */
    public void changeExchange(ExchangeEnum exchangeEnum) {
        exchange = exchangeEnum;
        // make sure we receive the data due to order numbering of a different exchange
        orderBookRef.init();
    }

    @lombok.Value
    static class ReferenceData {
        IAtomicReference<String> ref;
        IAtomicLong counter;
        ISemaphore semaphore;
        String name;

        ReferenceData(HazelcastInstance hazelcastInstance, String name) {
            ref = hazelcastInstance.getCPSubsystem().getAtomicReference(name);
            counter = hazelcastInstance.getCPSubsystem().getAtomicLong(name);
            semaphore = hazelcastInstance.getCPSubsystem().getSemaphore(name);
            this.name = name;
            init();
        }

        void init() {
            ref.clear();
            counter.set(-1);
        }
    }
}
