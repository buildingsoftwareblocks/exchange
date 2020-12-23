package com.btb.exchange.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.ExchangeSpecification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.btb.exchange.shared.dto.ExchangeEnum.BITSTAMP;

@Service
@Slf4j
public class BitstampExchangeService extends AbstractExchangeService {

    private final StreamingExchange exchange;

    public BitstampExchangeService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
        var exchangeSpecification = new ExchangeSpecification(BitstampStreamingExchange.class);
        exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
    }

    @PostConstruct
    void init() {
        super.init(exchange, BITSTAMP);
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
