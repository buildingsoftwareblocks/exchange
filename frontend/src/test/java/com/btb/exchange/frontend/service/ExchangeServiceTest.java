package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Orders;
import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.CompositeDisposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;
import static com.btb.exchange.shared.utils.CurrencyPairUtils.getSecondCurrencyPair;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@Testcontainers
@Slf4j
class ExchangeServiceTest {

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @MockBean
    SimpMessagingTemplate websocketMock;

    @Autowired
    ExchangeService service;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeEach
    void afterEach() {
        service.init();
        composite.clear();
    }

    @Test
    void process() throws InterruptedException, JsonProcessingException {
        // unclear why a wait is needed. but it seems that kafka is not complete ready
        Thread.sleep(1000);
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));
        var message = new ExchangeOrderBook(100, LocalTime.now(), ExchangeEnum.KRAKEN, getFirstCurrencyPair(),
                new Orders(new Date(), Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.orderBook(message.getCurrencyPair()), objectMapper.writeValueAsString(message));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("Result before timeout", waitResult);
        Mockito.verify(websocketMock).convertAndSend(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void processNonMatching() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));
        var message = new ExchangeOrderBook(100, LocalTime.now(), ExchangeEnum.BITSTAMP, getSecondCurrencyPair(),
                new Orders(new Date(), Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.orderBook(message.getCurrencyPair()), objectMapper.writeValueAsString(message));

        var waitResult = latch.await(2, TimeUnit.SECONDS);

        assertThat("No result before timeout", !waitResult);
        Mockito.verify(websocketMock, Mockito.never()).convertAndSend(Mockito.anyString(), Mockito.anyString());
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
            // iterate over currency pairs and register new beans
            CurrencyPairUtils.CurrencyPairs.forEach(cp ->
                    ac.registerBean(String.format("topic.%s", cp), NewTopic.class, () -> TopicBuilder.name(TopicUtils.orderBook(cp)).build()));
        }
    }
}