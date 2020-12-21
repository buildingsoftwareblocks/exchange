package com.btb.exchange.backend.config;

import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExchangeConfig {

    @Bean
    public StreamingExchange bitstampExchange() {
        return StreamingExchangeFactory.INSTANCE.createExchange(BitstampStreamingExchange.class);
    }
}
