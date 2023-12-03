package com.btb.exchange.analysis.simple;

import com.btb.exchange.analysis.services.OrderService;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.DTOUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MessageHandler {

    private final SimpleExchangeArbitrage simpleExchangeArbitrage;
    private final OrderService orderService;
    private final DTOUtils dtoUtils;
    private final DistributionSummary messagesCounter;

    // for testing purposes
    private final Subject<String> processed = PublishSubject.create();

    /**
     *
     */
    public MessageHandler(
            ObjectMapper objectMapper,
            SimpleExchangeArbitrage simpleExchangeArbitrage,
            OrderService orderService,
            MeterRegistry registry) {
        this.simpleExchangeArbitrage = simpleExchangeArbitrage;
        this.orderService = orderService;
        this.dtoUtils = new DTOUtils(objectMapper);
        this.messagesCounter = DistributionSummary.builder("analysis.simple.kafka.queue")
                .description("indicates number of message read form the kafka queue")
                .register(registry);
    }

    @KafkaListener(id = "orderBooks", topics = TopicUtils.INPUT_ORDERBOOK, containerFactory = "batchFactory", groupId = "analysis")
    public void process(List<String> messages) {
        log.debug("process {} messages", messages.size());
        messagesCounter.record(messages.size());
        var orderBooks = messages.stream()
                .map(o -> dtoUtils.fromDTO(o, ExchangeOrderBook.class))
                .toList();
        orderService.processSimpleExchangeArbitrage(simpleExchangeArbitrage.process(orderBooks));
        messages.forEach(processed::onNext);
    }

    /**
     * for testing purposes
     */
    Observable<String> subscribe() {
        return processed;
    }
}
