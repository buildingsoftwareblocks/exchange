package com.btb.exchange.frontend.service;

import com.btb.exchange.frontend.hazelcast.ExchangeDataSerializableFactory;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.utils.DTOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.IAtomicReference;
import com.hazelcast.cp.ISemaphore;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.btb.exchange.shared.dto.ExchangeEnum.KRAKEN;

/**
 * Handle Exchanges
 */
@Service
@Slf4j
public class ExchangeService {

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";
    private static final String WEBSOCKET_EXCHANGES = "/topic/exchanges";

    public static final String HAZELCAST_ORDERBOOKS = "orderBooks";
    public static final String HAZELCAST_OPPORTUNITIES = "opportunities";
    public static final String HAZELCAST_UPDATED = "updated";
    public static final String HAZELCAST_EXCHANGE = "exchange";

    private final SimpMessagingTemplate template;
    private final DTOUtils dtoUtils;

    @Value("${frontend.refreshrate:1000}")
    private int refreshRate;

    @Value("${frontend.message.diff.max:50}")
    private long maxMessageDiff;

    @Value("${frontend.opportunities:true}")
    private boolean showOpportunities;
    @Value("${frontend.updated:true}")
    private boolean showUpdated;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<String> sent = PublishSubject.create();

    private final ReferenceData orderBookRef;
    private final ReferenceData opportunityRef;
    private final IMap<Key, ExchangeValue> updated;
    private final IAtomicReference<ExchangeEnum> exchange;
    private final DistributionSummary kafkaMessagesCounter;
    private final DistributionSummary wsMessagesCounter;
    private final DistributionSummary orderbookDelay;
    private final DistributionSummary opportunityDelay;

    public ExchangeService(SimpMessagingTemplate template, HazelcastInstance hazelcastInstance,
                           ObjectMapper objectMapper, MeterRegistry registry) {
        this.template = template;
        this.dtoUtils = new DTOUtils(objectMapper);
        orderBookRef = new ReferenceData(hazelcastInstance, HAZELCAST_ORDERBOOKS);
        opportunityRef = new ReferenceData(hazelcastInstance, HAZELCAST_OPPORTUNITIES);
        updated = hazelcastInstance.getMap(HAZELCAST_UPDATED);
        exchange = hazelcastInstance.getCPSubsystem().getAtomicReference(HAZELCAST_EXCHANGE);

        kafkaMessagesCounter = DistributionSummary.builder("frontend.kafka.queue")
                .description("indicates number of message read form the kafka queue")
                .register(registry);

        wsMessagesCounter = DistributionSummary.builder("frontend.ws.queue")
                .description("indicates the size of message send to the web socket")
                .baseUnit("bytes")
                .register(registry);

        orderbookDelay = DistributionSummary.builder("frontend.orderbook.delay")
                .register(registry);

        opportunityDelay = DistributionSummary.builder("frontend.opportunity.delay")
                .register(registry);

        // set default value
        if (exchange.isNull()) {
            exchange.set(KRAKEN);
        }
    }

    void init() {
        orderBookRef.init();
        opportunityRef.init();
    }

    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory", groupId = "frontend")
    void processOrderBooks(List<String> messages) {
        log.info("process {} messages", messages.size());
        kafkaMessagesCounter.record(messages.size());
        final var now = LocalTime.now();
        messages.stream()
                .map(o -> dtoUtils.fromDTO(o, ExchangeOrderBook.class))
                .peek(o -> log.info("message number: {}", o.getOrder()))
                .peek(o -> updated(new Key(o.getExchange()), now, o.getCurrencyPair()))
                // TODO filter on currency pair as well?!
                .filter(o -> o.getExchange().equals(exchange.get()))
                // pick the last element
                .reduce((a, b) -> b)
                .ifPresent(orderBook -> update(orderBookRef, orderBook.getOrder(), orderBook));
    }

    void updated(Key key, LocalTime localTime, CurrencyPair cp) {
        updated.computeIfAbsent(key, v -> new ExchangeValue(localTime, cp));
        updated.computeIfPresent(key, (k, v) -> new ExchangeValue(localTime, v.cps, cp));
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).OPPORTUNITIES}", containerFactory = "batchFactory")
    void processOpportunities(List<String> messages) {
        // only get the last value and add it to the reference
        messages.stream()
                .map(o -> dtoUtils.fromDTO(o, Opportunities.class))
                .filter(opportunities -> !opportunities.getValues().isEmpty())
                // pick the last element
                .reduce((a, b) -> b)
                .ifPresent(opportunities -> update(opportunityRef, opportunities.getOrder(), opportunities));
    }

    Observable<String> subscribe() {
        return sent;
    }

    void update(ReferenceData data, long orderNr, ExchangeOrderBook orderBook) {
        if (orderBook.getTimestamp() != null) {
            orderbookDelay.record(orderBook.getTimestamp().until(LocalDateTime.now(), ChronoUnit.SECONDS));
        }
        update(data, orderNr, dtoUtils.toDTO(orderBook));
    }

    void update(ReferenceData data, long orderNr, Opportunities opportunities) {
        if (opportunities.getTimestamp() != null) {
            opportunityDelay.record(opportunities.getTimestamp().until(LocalDateTime.now(), ChronoUnit.MILLIS));
        }
        update(data, orderNr, dtoUtils.toDTO(opportunities));
    }

    /**
     * Update reference data in an atomic way
     */
    @SneakyThrows
    void update(ReferenceData data, long orderNr, String message) {
        try {
            data.semaphore.acquire();
            if (diffAccepted(data.counter.get(), orderNr)) {
                data.ref.set(message);
                data.counter.set(orderNr);
            } else {
                log.debug("out of sync ({}): {} vs {}", data.name, data.counter.get(), orderNr);
            }
        } finally {
            data.semaphore.release();
        }
    }

    /**
     * Is diff in orderNumber acceptable
     */
    boolean diffAccepted(long current, long update) {
        return current < update || (current - update > maxMessageDiff);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        // send order books if there is data
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!orderBookRef.ref.isNull()) {
                        var message = orderBookRef.ref.get();
                        log.debug("Tick : {}", message);
                        wsMessagesCounter.record(message.length());
                        template.convertAndSend(WEBSOCKET_ORDERBOOK, message);
                        sent.onNext(message);
                    }

                    if (showOpportunities && !opportunityRef.ref.isNull()) {
                        // send opportunities if there is data
                        var message = opportunityRef.ref.get();
                        log.debug("Send opportunities: {}", message);
                        wsMessagesCounter.record(message.length());
                        template.convertAndSend(WEBSOCKET_OPPORTUNITIES, message);
                    }

                    if (showUpdated && !updated.isEmpty()) {
                        var message = exchangesData();
                        wsMessagesCounter.record(message.length());
                        template.convertAndSend(WEBSOCKET_EXCHANGES, message);
                    }
                });
    }

    String exchangesData() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        Map<String, Map<String, String>> message = new TreeMap<>();
        updated.forEach((k, v) -> {
            Map<String, String> map = new HashMap<>();
            map.put("timestamp", v.timestamp.format(formatter));
            map.put("cps", v.cps.toString());
            message.put(k.exchange.toString(), map);
        });
        return dtoUtils.toDTO(message);
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
        if (showUpdated && !updated.isEmpty()) {
            template.convertAndSend(WEBSOCKET_EXCHANGES, exchangesData());
        }
    }

    /**
     * Change to given exchange to show on the GUI
     */
    @SneakyThrows
    public void changeExchange(ExchangeEnum exchangeEnum) {
        exchange.set(exchangeEnum);

        // make sure we receive the data due to order numbering of a different exchange
        try {
            orderBookRef.semaphore.acquire();
            orderBookRef.init();
        } finally {
            orderBookRef.semaphore.release();
        }
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Key implements IdentifiedDataSerializable {
        private ExchangeEnum exchange;

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.KEY_TYPE;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeString(exchange.toString());
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            exchange = ExchangeEnum.valueOf(in.readString());
        }
    }

    @Data
    @NoArgsConstructor
    public static class ExchangeValue implements IdentifiedDataSerializable {
        private LocalTime timestamp;
        private Set<CurrencyPair> cps = new HashSet<>();

        public ExchangeValue(LocalTime localTime, CurrencyPair cp) {
            this.timestamp = localTime;
            cps.add(cp);
        }

        public ExchangeValue(LocalTime localTime, Set<CurrencyPair> cps, CurrencyPair cp) {
            this.timestamp = localTime;
            this.cps.addAll(cps);
            this.cps.add(cp);
        }

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.EXCHANGE_VALUE_TYPE;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeLong(timestamp.toNanoOfDay());
            out.writeStringArray(cps.stream().map(CurrencyPair::toString).toArray(String[]::new));
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            timestamp = LocalTime.ofNanoOfDay(in.readLong());
            cps = Arrays.stream(in.readStringArray()).map(CurrencyPair::new).collect(Collectors.toSet());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyPairValue implements IdentifiedDataSerializable {
        private CurrencyPair currencyPair;

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.CURRENYPAIR_VALUE_TYPE;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeString(currencyPair.toString());
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            currencyPair = new CurrencyPair(in.readString());
        }
    }
}
