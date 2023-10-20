package com.btb.exchange.analysis.simple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

import com.btb.exchange.analysis.services.OrderService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Orders;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@Slf4j
class MessageHandlerTest {

    @Container
    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    MessageHandler handler;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MeterRegistry registry;

    @MockBean
    SimpleExchangeArbitrage simpleExchangeArbitrage;

    @MockBean
    OrderService orderService;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeEach
    void beforeTest() {
        kafkaListenerEndpointRegistry
                .getListenerContainers()
                .forEach(messageListenerContainer -> ContainerTestUtils.waitForAssignment(messageListenerContainer, 1));
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void process() throws JsonProcessingException, InterruptedException {
        var latch = new CountDownLatch(1);
        composite.add(handler.subscribe().subscribe(r -> latch.countDown()));
        Mockito.when(simpleExchangeArbitrage.process(Mockito.anyList()))
                .thenReturn(Opportunities.builder().timestamp(LocalTime.now()).build());

        var message = new ExchangeOrderBook(
                100,
                LocalTime.now(),
                ExchangeEnum.KRAKEN,
                "12",
                BTC_USD,
                new Orders(Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.INPUT_ORDERBOOK, objectMapper.writeValueAsString(message));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("No result after timeout", waitResult);
        Mockito.verify(orderService).processSimpleExchangeArbitrage(Mockito.any());
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    @TestConfiguration
    @RequiredArgsConstructor
    static class ExchangeTestConfig {

        private final GenericApplicationContext ac;

        @PostConstruct
        public void init() {
            ac.registerBean(
                    TopicUtils.INPUT_ORDERBOOK, NewTopic.class, () -> TopicBuilder.name(TopicUtils.INPUT_ORDERBOOK)
                            .build());
        }
    }
}
