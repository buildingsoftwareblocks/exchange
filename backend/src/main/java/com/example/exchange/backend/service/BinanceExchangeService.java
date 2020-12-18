package com.example.exchange.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.ExchangeSpecification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.example.exchange.shared.dto.ExchangeEnum.BINANCE;

@Service
@Slf4j
public class BinanceExchangeService extends AbstractExchangeService {

    private final StreamingExchange exchange;

    public BinanceExchangeService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
        var exchangeSpecification = new ExchangeSpecification(BinanceStreamingExchange.class);
        exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
    }

    @PostConstruct
    void init() {
        super.init(exchange, BINANCE);
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
