package com.btb.exchange.backend.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.backend.data.mongodb.MongoDBDatabaseService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@Slf4j
class ExchangeServiceTest {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest");

    @Container
    private static final ElasticsearchContainer ELASTICSEARCH_CONTAINER =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.8");

    @Container
    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest")).withEmbeddedZookeeper();

    @Container
    private static final GenericContainer ZOOKEEPER = new GenericContainer("zookeeper:latest").withExposedPorts(2181);

    @Autowired
    MongoDBDatabaseService mongoDBDatabaseService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MeterRegistry registry;

    @Autowired
    CuratorFramework curatorFramework;

    @MockBean
    LeaderService leaderService;

    private final CompositeDisposable composite = new CompositeDisposable();

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    ExchangeService createExchangeService() {
        ApplicationConfig config = new ApplicationConfig(false, true, false, 5);
        LeaderSelector leaderSelector = Mockito.mock(LeaderSelector.class);
        Mockito.when(leaderSelector.hasLeadership()).thenReturn(true);
        return new ExchangeService(
                leaderSelector,
                Mockito.mock(StreamingExchange.class),
                kafkaTemplate,
                registry,
                objectMapper,
                config,
                ExchangeEnum.KRAKEN,
                "123",
                true,
                Set.of(BTC_USD));
    }

    @Test
    void process() throws InterruptedException {
        CountDownLatch latch;
        final List<String> results;
        try (ExchangeService service = createExchangeService()) {
            latch = new CountDownLatch(1);
            results = new ArrayList<>();
            composite.add(service.subscribe().subscribe(r -> {
                results.add(r);
                latch.countDown();
            }));

            service.process(new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()), BTC_USD);
        }

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", results.size(), is(1));
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("backend.zookeeper.host", () -> "localhost:" + ZOOKEEPER.getFirstMappedPort());
        registry.add("spring.elasticsearch.uris", ELASTICSEARCH_CONTAINER::getHttpHostAddress);
    }
}
