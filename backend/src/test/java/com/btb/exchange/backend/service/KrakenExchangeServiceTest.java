package com.btb.exchange.backend.service;

import com.btb.exchange.backend.data.MessageRepository;
import io.reactivex.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {KrakenExchangeServiceTest.Initializer.class})
@Slf4j
class KrakenExchangeServiceTest {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest");
    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Autowired
    MessageRepository repository;
    @Autowired
    KrakenExchangeService service;

    private final CompositeDisposable composite = new CompositeDisposable();

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

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl()),
                    String.format("spring.kafka.bootstrap-servers: %s", KAFKA_CONTAINER.getBootstrapServers()),
                    "backend.recording: false",
                    "backend.replay: true"
            ).applyTo(configurableApplicationContext);
        }
    }
}