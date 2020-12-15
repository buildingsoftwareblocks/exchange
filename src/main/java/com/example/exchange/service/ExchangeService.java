package com.example.exchange.service;

import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
@Slf4j
public class ExchangeService {

    private final StreamingExchange exchange;
    private final SimpMessagingTemplate template;

    public ExchangeService(SimpMessagingTemplate template) {
        this.template = template;
        // Use StreamingExchangeFactory instead of ExchangeFactory
        exchange = StreamingExchangeFactory.INSTANCE.createExchange(BitstampStreamingExchange.class);
    }

    @PostConstruct
    void init() {
        // Connect to the Exchange WebSocket API. Here we use a blocking wait.
        exchange.connect().blockingAwait();
        // Subscribe order book data with the reference to the subscription.
        exchange.getStreamingMarketDataService()
                .getOrderBook(CurrencyPair.BTC_USD)
                .subscribe(orderBook -> process(orderBook));
    }

    void process(OrderBook orderBook) {
        log.info("Order book: {}", orderBook);
        // send orderbook to webbrowser
        template.convertAndSend("/topic/orderbook", orderBook);
    }

    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
