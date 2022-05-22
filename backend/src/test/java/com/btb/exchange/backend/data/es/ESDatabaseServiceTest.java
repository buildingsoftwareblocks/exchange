package com.btb.exchange.backend.data.es;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.backend.service.ExchangeService;
import com.btb.exchange.backend.service.LeaderService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Orders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

@SpringBootTest
@Testcontainers
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class ESDatabaseServiceTest {

    @Container
    private static final ElasticsearchContainer ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.12.1");
    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    @Container
    private static final GenericContainer ZOOKEEPER = new GenericContainer("zookeeper:latest").withExposedPorts(2181);

    @Autowired
    ESDatabaseService service;
    @Autowired
    ESMessageRepository repository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    MeterRegistry registry;
    @MockBean
    LeaderService leaderService;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    private ExchangeService createExchangeService() {
        ApplicationConfig config = new ApplicationConfig(true, false, false, 5);
        LeaderSelector leaderSelector = Mockito.mock(LeaderSelector.class);
        Mockito.when(leaderSelector.hasLeadership()).thenReturn(true);
        return new ExchangeService(leaderSelector, Mockito.mock(StreamingExchange.class),
                kafkaTemplate, registry, objectMapper, config, ExchangeEnum.KRAKEN, "123", true, Set.of(BTC_USD));
    }

    @Test
    void testStore() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));
        var startCount = repository.count();

        var msg = objectMapper.writeValueAsString(new ExchangeOrderBook(1, LocalTime.now(), ExchangeEnum.BITSTAMP,
                "123", BTC_USD, new Orders(Collections.emptyList(), Collections.emptyList())));
        service.store(msg);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count() - startCount, is(1L));
    }

    @Test
    void testKafkaListener() throws InterruptedException {
        ExchangeService exchangeService = createExchangeService();
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var startCount = repository.count();
        exchangeService.process(new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()), BTC_USD);
        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", repository.count() - startCount, is(1L));
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.elasticsearch.uris", ELASTICSEARCH_CONTAINER::getHttpHostAddress);
        registry.add("backend.es", () -> true);
        registry.add("backend.zookeeper.host", () -> "localhost:" + ZOOKEEPER.getFirstMappedPort());
    }
}