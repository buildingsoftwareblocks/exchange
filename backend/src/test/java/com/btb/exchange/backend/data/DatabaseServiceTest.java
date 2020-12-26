package com.btb.exchange.backend.data;

import com.btb.exchange.backend.service.BitstampExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USDT;
import static org.knowm.xchange.currency.CurrencyPair.ETH_BTC;

@SpringBootTest
@Slf4j
class DatabaseServiceTest {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest").withReuse(true);
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest")).withReuse(true);

    @Autowired
    DatabaseService service;
    @Autowired
    MessageRepository repository;
    @Autowired
    BitstampExchangeService exchangeService;
    @Autowired
    ObjectMapper objectMapper;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeAll
    static void beforeAll() {
        MONGO_DB_CONTAINER.start();
        KAFKA_CONTAINER.start();
    }

    @BeforeEach
    void beforeEach() {
        repository.deleteAll().blockingAwait();
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void testStore() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        var msg = objectMapper.writeValueAsString(new ExchangeOrderBook(ExchangeEnum.BITSTAMP, BTC_USDT.toString(),
                new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList())));
        service.store(msg);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count().blockingGet() - startCount, is(1L));
    }

    @Test
    void testKakfaListener() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        exchangeService.process(new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()), BTC_USDT);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count().blockingGet() - startCount, is(1L));
    }

    @Test
    void testReplayMessage() throws InterruptedException, JsonProcessingException {
        // message is stored twice, direct and indirect via replay
        var latch = new CountDownLatch(2);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        final var startCount = repository.count().blockingGet();
        final var replayed = new AtomicBoolean();
        // make sure we subscribe to the event, before we act on it.
        composite.add(service.subscribe().subscribe(r -> {
            // replay only once
            if (!replayed.getAndSet(true)) {
                log.info("start replay: '{}'", r);
                service.replayEvents();
            }
        }));
        var msg = objectMapper.writeValueAsString(new ExchangeOrderBook(ExchangeEnum.BITSTAMP, ETH_BTC.toString(),
                new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList())));
        service.store(msg);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        // because in this test case the replayed records will be stored again, so at least 2 is the answer
        assertThat("check #records are added", repository.count().blockingGet() - startCount, greaterThanOrEqualTo(2L));
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("backend.recording", () -> true);
        registry.add("backend.replay", () -> false);
    }

    @TestConfiguration
    @RequiredArgsConstructor
    static class ExchangeTestConfig {

        private final GenericApplicationContext ac;

        @PostConstruct
        void init() {
            // register all posible Exchanges and register then
            var exchangeMock = Mockito.mock(StreamingExchange.class);
            var smds = Mockito.mock(StreamingMarketDataService.class);
            Mockito.when(smds.getOrderBook(Mockito.any())).thenReturn(Observable.empty());
            Mockito.when(exchangeMock.connect(Mockito.any())).thenReturn(Completable.complete());
            Mockito.when(exchangeMock.getStreamingMarketDataService()).thenReturn(smds);

            Arrays.stream(ExchangeEnum.values()).forEach(e ->
                    ac.registerBean(e.name().toLowerCase(),
                            StreamingExchange.class,
                            () -> exchangeMock,
                            bp -> bp.setPrimary(true)));
        }
    }
}
