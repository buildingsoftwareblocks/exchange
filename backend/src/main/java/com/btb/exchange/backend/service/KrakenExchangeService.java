package com.btb.exchange.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.ExchangeSpecification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.btb.exchange.shared.dto.ExchangeEnum.KRAKEN;

@Service
@Slf4j
public class KrakenExchangeService extends AbstractExchangeService {

    private final StreamingExchange exchange;

    public KrakenExchangeService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
        var exchangeSpecification = new ExchangeSpecification(KrakenStreamingExchange.class);
        exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
    }

    @PostConstruct
    void init() {
        super.init(exchange, KRAKEN);
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
