package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.btb.exchange.shared.dto.ExchangeEnum.BITFINEX;
import static com.btb.exchange.shared.dto.ExchangeEnum.BITSTAMP;

@Service
@Slf4j
public class BitfinexExchangeService extends AbstractExchangeService {

    public BitfinexExchangeService(@Qualifier("bitfinex") StreamingExchange exchange,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper, ApplicationConfig config) {
        super(exchange, BITFINEX, kafkaTemplate, objectMapper, config);
    }
}

