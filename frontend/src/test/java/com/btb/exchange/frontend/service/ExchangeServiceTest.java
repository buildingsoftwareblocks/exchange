package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.CompositeDisposable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.knowm.xchange.currency.CurrencyPair.BTC_USDT;

@SpringBootTest
@Slf4j
class ExchangeServiceTest {

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest")).withReuse(true);

    @MockBean
    SimpMessagingTemplate websocketMock;

    @Autowired
    ExchangeService service;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;

    private final CompositeDisposable composite = new CompositeDisposable();

    @BeforeAll
    static void beforeAll() {
        KAFKA_CONTAINER.start();
    }

    @AfterEach
    void afterEach() {
        composite.clear();
    }

    @Test
    void process() throws InterruptedException, JsonProcessingException {
        var latch = new CountDownLatch(1);
        composite.add(service.subscribe().subscribe(r -> latch.countDown()));
        var message = new ExchangeOrderBook(ExchangeEnum.BITSTAMP, BTC_USDT.toString(), new OrderBook(new Date(), Collections.emptyList(), Collections.emptyList()));
        kafkaTemplate.send(TopicUtils.orderBook(message.getCurrencyPair()), objectMapper.writeValueAsString(message));

        var waitResult = latch.await(10, TimeUnit.SECONDS);

        assertThat("result before timeout", waitResult);
        Mockito.verify(websocketMock).convertAndSend(Mockito.anyString(), Mockito.anyString());
    }

    @DynamicPropertySource
    static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }
}