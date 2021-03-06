package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.services.OrderService;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.DTOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageHandler {

    private final SimpleExchangeArbitrage simpleExchangeArbitrage;
    private final OrderService orderService;
    private final DTOUtils dtoUtils;
    private final DistributionSummary messagesCounter;

    public MessageHandler(ObjectMapper objectMapper, SimpleExchangeArbitrage simpleExchangeArbitrage, OrderService orderService, MeterRegistry registry) {
        this.simpleExchangeArbitrage = simpleExchangeArbitrage;
        this.orderService = orderService;
        this.dtoUtils = new DTOUtils(objectMapper);
        messagesCounter = DistributionSummary.builder("analysis.simple.kafka.queue")
                .description("indicates number of message read form the kafka queue")
                .register(registry);
    }

    @Async
    @Timed("analysis.simple.process.timed")
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    public void process(List<String> messages) {
        log.debug("process {} messages", messages.size());
        messagesCounter.record(messages.size());
        var orderBooks = messages.stream().map(o -> dtoUtils.fromDTO(o, ExchangeOrderBook.class)).collect(Collectors.toList());
        orderService.processSimpleExchangeArbitrage(simpleExchangeArbitrage.process(orderBooks));
    }
}
