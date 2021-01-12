package com.btb.exchange.backend.data;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.backend.service.ExchangeService;
import com.btb.exchange.backend.service.LeaderService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@Testcontainers
@Slf4j
class DatabaseServiceTest {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest");
    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    @Container
    private static final GenericContainer ZOOKEEPER = new GenericContainer("zookeeper:latest").withExposedPorts(2181);

    @Autowired
    DatabaseService service;
    @Autowired
    MessageRepository repository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @MockBean
    LeaderService leaderService;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeEach
    void beforeEach() {
        repository.deleteAll().blockingAwait();
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    ExchangeService createExchangeService() {
        ApplicationConfig config = new ApplicationConfig(true, false, true);
        ExchangeService exchangeService = new ExchangeService(Mockito.mock(CuratorFramework.class), leaderService, Mockito.mock(StreamingExchange.class),
                kafkaTemplate, objectMapper, config, ExchangeEnum.KRAKEN, "/", Mockito.mock(Semaphore.class));
        return exchangeService;
    }

    @Test
    void testStore() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        var msg = objectMapper.writeValueAsString(new ExchangeOrderBook(ExchangeEnum.BITSTAMP, getFirstCurrencyPair(),
                new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList())));
        service.store(msg);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count().blockingGet() - startCount, is(1L));
    }

    @Test
    void testKakfaListener() throws InterruptedException, JsonProcessingException {
        ExchangeService exchangeService = createExchangeService();
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var startCount = repository.count().blockingGet();
        exchangeService.process(new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()), getFirstCurrencyPair());
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
        var msg = objectMapper.writeValueAsString(new ExchangeOrderBook(ExchangeEnum.BITSTAMP, getFirstCurrencyPair(),
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
        registry.add("backend.testing", () -> true);
        registry.add("backend.zookeeper", () -> "localhost:" + ZOOKEEPER.getFirstMappedPort());
    }
}
