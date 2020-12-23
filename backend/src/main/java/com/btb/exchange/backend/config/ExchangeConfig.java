package com.btb.exchange.backend.config;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExchangeConfig {

    @Bean
    @Qualifier("bitstamp")
    public StreamingExchange bitstampExchange() {
        return StreamingExchangeFactory.INSTANCE.createExchange(BitstampStreamingExchange.class);
    }

    @Bean
    @Qualifier("kraken")
    public StreamingExchange krakenExchange() {
        return StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
    }

    @Bean
    @Qualifier("binance")
    public StreamingExchange binanceExchange() {
        return StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class);
    }
}
