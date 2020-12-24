package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.btb.exchange.shared.dto.ExchangeEnum.KRAKEN;

@Service
public class KrakenExchangeService extends AbstractExchangeService {

    public KrakenExchangeService(@Qualifier("kraken") StreamingExchange exchange,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper, ApplicationConfig config) {
        super(exchange, KRAKEN, kafkaTemplate, objectMapper, config);
    }
}
