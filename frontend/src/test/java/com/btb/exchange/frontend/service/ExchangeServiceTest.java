package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USDT;
import static org.knowm.xchange.currency.CurrencyPair.ETH_BTC;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {ExchangeServiceTest.Initializer.class})
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

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void process() throws InterruptedException, JsonProcessingException, ExecutionException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));
        var message = new ExchangeOrderBook(ExchangeEnum.BITSTAMP, BTC_USDT.toString(), new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.orderBook(message.getCurrencyPair()), objectMapper.writeValueAsString(message));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        Mockito.verify(websocketMock).convertAndSend(Mockito.anyString(), Mockito.anyString());
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    String.format("spring.kafka.bootstrap-servers=%s", KAFKA_CONTAINER.getBootstrapServers())
            ).applyTo(configurableApplicationContext);
        }
    }
}