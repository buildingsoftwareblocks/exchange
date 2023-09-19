package com.btb.exchange.frontend.service;

import com.btb.exchange.frontend.hazelcast.ExchangeDataSerializableFactory;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.ExchangeTicker;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.utils.DTOUtils;
import com.btb.exchange.shared.utils.TopicUtils;
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
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.btb.exchange.shared.utils.TopicUtils.OPPORTUNITIES;

/**
 * Handle a Exchange
 */
@Service
@Slf4j
public class ExchangeService {

    public static final String HAZELCAST_ORDERBOOKS = "frontend.orderBooks";
    public static final String HAZELCAST_TICKERS = "frontend.tickers";
    public static final String HAZELCAST_OPPORTUNITIES = "frontend.opportunities";
    public static final String HAZELCAST_UPDATED = "frontend.updated";

    private static final String EXCHANGES_TIME_FORMAT = "HH:mm:ss.SSS";

    private final DTOUtils dtoUtils;

    private final ReferenceData opportunities;
    private final IMap<ExchangeKey, ExchangeValue> updated;
    private final IMap<ExchangeCPKey, ExchangeData> orderBooks;
    private final IMap<ExchangeCPKey, ExchangeData> tickers;

    private final DistributionSummary kafkaMessagesCounter;
    private final DistributionSummary orderBookDelay;
    private final DistributionSummary opportunityDelay;

    // for testing purposes, to subscribe to the event that an orderbook is received
    private final Subject<String> orderBookReceived = PublishSubject.create();

    /**
     *
     */
    public ExchangeService(
            HazelcastInstance hazelcastInstance, ObjectMapper objectMapper, MeterRegistry registry) {
        this.dtoUtils = new DTOUtils(objectMapper);
        opportunities = new ReferenceData(hazelcastInstance, HAZELCAST_OPPORTUNITIES);
        updated = hazelcastInstance.getMap(HAZELCAST_UPDATED);
        orderBooks = hazelcastInstance.getMap(HAZELCAST_ORDERBOOKS);
        tickers = hazelcastInstance.getMap(HAZELCAST_TICKERS);

        kafkaMessagesCounter =
                DistributionSummary.builder("frontend.kafka.queue")
                        .description("indicates number of message read form the kafka queue")
                        .register(registry);

        orderBookDelay = DistributionSummary.builder("frontend.orderbook.delay").register(registry);
        opportunityDelay = DistributionSummary.builder("frontend.opportunity.delay").register(registry);
    }

    void init() {
        opportunities.init();
    }

    @KafkaListener(
            topics = TopicUtils.INPUT_ORDERBOOK,
            containerFactory = "batchFactory",
            groupId = "frontend")
    void processOrderBooks(String msg) {
        log.debug("process {}", msg);
        kafkaMessagesCounter.record(1);
        final var now = LocalTime.now();
        ExchangeOrderBook orderBook = dtoUtils.fromDTO(msg, ExchangeOrderBook.class);
        updated(orderBook, now);
        orderBook(new ExchangeCPKey(orderBook.getExchange(), orderBook.getCurrencyPair()),
                now,
                orderBook,
                msg);
        orderBookReceived.onNext(msg);
    }

    @KafkaListener(topics = TopicUtils.INPUT_TICKER, containerFactory = "batchFactory", groupId = "frontend")
    void processTickers(String msg) {
        log.debug("process {} message", msg);
        kafkaMessagesCounter.record(1);
        final var now = LocalTime.now();
        ExchangeTicker ticker = dtoUtils.fromDTO(msg, ExchangeTicker.class);
        ticker(new ExchangeCPKey(ticker.getExchange(), ticker.getCurrencyPair()), now, ticker, msg);
    }

    void updated(ExchangeOrderBook orderBook, LocalTime localTime) {
        final ExchangeKey key = new ExchangeKey(orderBook.getExchange());
        final CurrencyPair cp = orderBook.getCurrencyPair();
        updated.computeIfAbsent(key, v -> new ExchangeValue(localTime, cp));
        updated.computeIfPresent(key, (k, v) -> new ExchangeValue(localTime, v.cps, cp));
    }

    void orderBook(ExchangeCPKey key, LocalTime localTime, ExchangeOrderBook orderBook, String msg) {
        if (orderBook.getTimestamp() != null) {
            orderBookDelay.record(
                    orderBook.getTimestamp().until(LocalDateTime.now(), ChronoUnit.SECONDS));
        }
        orderBooks.set(key, new ExchangeData(localTime, orderBook.getOrder(), msg));
    }

    void ticker(ExchangeCPKey key, LocalTime localTime, ExchangeTicker ticker, String msg) {
        tickers.set(key, new ExchangeData(localTime, ticker.getOrder(), msg));
    }

    @KafkaListener(topics = OPPORTUNITIES, containerFactory = "batchFactory", groupId = "frontend")
    void processOpportunities(String msg) {
        // only get the last value and add it to the reference
        Opportunities opportunities = dtoUtils.fromDTO(msg, Opportunities.class);
        update(this.opportunities, opportunities.getOrder(), opportunities);
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
            data.ref.set(message);
            data.counter.set(orderNr);
        } finally {
            data.semaphore.release();
        }
    }

    public Optional<String> exchangesData() {
        if (updated.isEmpty()) {
            return Optional.empty();
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(EXCHANGES_TIME_FORMAT);
            Map<String, Map<String, String>> message = new TreeMap<>();
            updated.forEach(
                    (k, v) -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("timestamp", v.timestamp.format(formatter));
                        map.put("cps", v.cps.toString());
                        message.put(k.exchange.toString(), map);
                    });
            return Optional.of(dtoUtils.toDTO(message));
        }
    }

    public Optional<String> opportunitiesData() {
        if (opportunities.ref.isNull()) {
            return Optional.empty();
        } else {
            return Optional.of(opportunities.ref.get());
        }
    }

    public Optional<String> orderBooksData(ExchangeEnum exchange, CurrencyPair cp) {
        ExchangeData data = orderBooks.get(new ExchangeCPKey(exchange, cp));
        if (data == null) {
            return Optional.empty();
        } else {
            return Optional.of(data.message);
        }
    }

    public Optional<String> tickersData() {
        if (tickers.isEmpty()) {
            return Optional.empty();
        } else {
            Comparator<ExchangeTicker> comparator =
                    Comparator.comparing(ExchangeTicker::getCurrencyPair)
                            .thenComparing(ExchangeTicker::getExchange);
            List<ExchangeTicker> result =
                    tickers.values().stream()
                            .map(t -> (dtoUtils.fromDTO(t.message, ExchangeTicker.class)))
                            .sorted(comparator)
                            .toList();
            return Optional.of(dtoUtils.toDTO(result));
        }
    }

    public List<ExchangeEnum> activeExchanges() {
        return updated.keySet().stream().map(ExchangeKey::getExchange).toList();
    }

    public List<CurrencyPair> activeCurrencies(ExchangeEnum exchange) {
        ExchangeValue ev = updated.get(new ExchangeKey(exchange));
        if (ev != null) {
            return List.copyOf(ev.getCps());
        } else {
            return Collections.emptyList();
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
    public static class ExchangeKey implements IdentifiedDataSerializable {
        private ExchangeEnum exchange;

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.EXCHANGE_KEY_TYPE;
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
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExchangeCPKey implements IdentifiedDataSerializable {
        private ExchangeEnum exchange;
        private CurrencyPair currencyPair;

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.EXCHANGE_CP_KEY_TYPE;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeString(exchange.toString());
            out.writeString(currencyPair.toString());
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            exchange = ExchangeEnum.valueOf(in.readString());
            currencyPair = new CurrencyPair(Objects.requireNonNull(in.readString()));
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
            cps =
                    Arrays.stream(Objects.requireNonNull(in.readStringArray()))
                            .map(CurrencyPair::new)
                            .collect(Collectors.toSet());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExchangeData implements IdentifiedDataSerializable {
        private LocalTime timestamp;
        private long counter;
        private String message;

        @Override
        public int getFactoryId() {
            return ExchangeDataSerializableFactory.FACTORY_ID;
        }

        @Override
        public int getClassId() {
            return ExchangeDataSerializableFactory.EXCHANGE_DATA_TYPE;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeLong(timestamp.toNanoOfDay());
            out.writeLong(counter);
            out.writeString(message);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            timestamp = LocalTime.ofNanoOfDay(in.readLong());
            counter = in.readLong();
            message = in.readString();
        }
    }

    Observable<String> subscribe() {
        return orderBookReceived;
    }
}
