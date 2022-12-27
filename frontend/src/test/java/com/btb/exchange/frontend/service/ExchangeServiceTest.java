package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Orders;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

@SpringBootTest
@Testcontainers
@Slf4j
class ExchangeServiceTest {

    @Container
    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Autowired
    ExchangeService service;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeEach
    void beforeEach() {
        service.init();
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void process() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));

        var message =
                new ExchangeOrderBook(
                        100,
                        LocalTime.now(),
                        ExchangeEnum.KRAKEN,
                        "123",
                        BTC_USD,
                        new Orders(Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.INPUT_ORDERBOOK, objectMapper.writeValueAsString(message));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("Result before timeout", waitResult);
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("frontend.opportunities", () -> false);
        registry.add("frontend.updated", () -> false);
    }

    @TestConfiguration
    @RequiredArgsConstructor
    static class ExchangeTestConfig {

        private final GenericApplicationContext ac;

        @PostConstruct
        public void init() {
            ac.registerBean(
                    TopicUtils.INPUT_ORDERBOOK,
                    NewTopic.class,
                    () -> TopicBuilder.name(TopicUtils.INPUT_ORDERBOOK).build());
            ac.registerBean(
                    TopicUtils.INPUT_TICKER,
                    NewTopic.class,
                    () -> TopicBuilder.name(TopicUtils.INPUT_TICKER).build());
            ac.registerBean(
                    TopicUtils.OPPORTUNITIES,
                    NewTopic.class,
                    () -> TopicBuilder.name(TopicUtils.OPPORTUNITIES).build());
        }
    }
}
