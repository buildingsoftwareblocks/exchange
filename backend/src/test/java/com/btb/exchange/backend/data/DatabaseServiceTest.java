package com.btb.exchange.backend.data;

import com.btb.exchange.backend.service.ExchangeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MongoDBContainer;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@ContextConfiguration(initializers = {DatabaseServiceTest.Initializer.class})
class DatabaseServiceTest {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest");

    @Autowired
    DatabaseService service;
    @Autowired
    MessageRepository repository;
    @Autowired
    ExchangeService exchangeService;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeAll
    static void setUpAll() {
        MONGO_DB_CONTAINER.start();
    }

    @AfterAll
    static void tearDownAll() {
        if (!MONGO_DB_CONTAINER.isShouldBeReused()) {
            MONGO_DB_CONTAINER.stop();
        }
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void testStore() throws InterruptedException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribeOnStore().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        var message = "this is a message-1";
        service.store(message);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count().blockingGet() - startCount, is(1L));
    }

    @Test
    void testStoreMessage() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribeOnStore().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        exchangeService.process(new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count().blockingGet() - startCount, is(1L));
    }

    @Test
    void testReplayMessage() throws InterruptedException {
        // message is stored twice, direct and indirect via replay
        var latch = new CountDownLatch(2);
        composite.add(service.subscribeOnStore().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        service.store("this is a message-2");
        composite.add(service.subscribeOnStore().subscribe(r -> service.replayEvents()));
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        // because in this test case the replayed records will be stored again, so al least 2 is the answer
        assertThat("check #records are added", repository.count().blockingGet() - startCount, greaterThanOrEqualTo(2L));
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {
            var uri = MONGO_DB_CONTAINER.getReplicaSetUrl();
            TestPropertyValues.of(
                    String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl()),
                    "backend.recording: true", "backend.replay: false").applyTo(configurableApplicationContext);
        }
    }

    @TestConfiguration
    static class ExchangeTestConfig {

        @Bean
        @Primary
        public StreamingExchange bitstamp() {
            var exchangeMock = Mockito.mock(StreamingExchange.class);
            var sms = Mockito.mock(StreamingMarketDataService.class);
            Mockito.when(sms.getOrderBook(Mockito.any())).thenReturn(Observable.empty());
            Mockito.when(exchangeMock.connect()).thenReturn(Completable.complete());
            Mockito.when(exchangeMock.getStreamingMarketDataService()).thenReturn(sms);
            return exchangeMock;
        }
    }
}
