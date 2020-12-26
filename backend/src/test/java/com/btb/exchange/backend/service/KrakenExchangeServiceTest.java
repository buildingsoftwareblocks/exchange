package com.btb.exchange.backend.service;

import com.btb.exchange.backend.data.MessageRepository;
import io.reactivex.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@Slf4j
class KrakenExchangeServiceTest {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest").withReuse(true);
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest")).withReuse(true);

    @Autowired
    MessageRepository repository;
    @Autowired
    KrakenExchangeService service;

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

    @Test
    void processInReplayMode() throws InterruptedException {
        var latch = new CountDownLatch(1);
        final List<String> results = new ArrayList<>();
        composite.add(service.subscribe().subscribe(r -> {
            results.add(r);
            latch.countDown();
        }));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        assertThat("check 1 record is added", results.size(), is(1));
        assertThat("check 1 record is default value", results.get(0), is(AbstractExchangeService.DEFAULT_VALUE));
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("backend.recording", () -> false);
        registry.add("backend.replay", () -> true);
    }
}