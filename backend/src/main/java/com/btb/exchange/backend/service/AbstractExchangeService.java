package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.utils.TopicUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.knowm.xchange.currency.CurrencyPair.*;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractExchangeService {

    public static final List<CurrencyPair> CurrencyPairs = Arrays.asList(BTC_USDT, ETH_BTC, DASH_USDT);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig config;

    /**
     * Initialize the connection with the exchange
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init(StreamingExchange exchange, ExchangeEnum exchangeEnum) {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            var subscription = CurrencyPairs.stream()
                    .reduce(ProductSubscription.create(),
                            ProductSubscription.ProductSubscriptionBuilder::addOrderbook,
                            (psb1, psb2) -> {
                        throw new UnsupportedOperationException();
                    }).build();

            exchange.connect(subscription).blockingAwait();

            // Subscribe order book data with the reference to the currency pair.
            CurrencyPairs.forEach(cp -> subscribe(exchange, cp, exchangeEnum, TopicUtils.orderBook(cp)));
        }
    }

    final void subscribe(StreamingExchange exchange, CurrencyPair currencyPair, ExchangeEnum exchangeEnum, String topic) {
        exchange.getStreamingMarketDataService().getOrderBook(currencyPair).subscribe(orderBook -> process(orderBook, exchangeEnum, topic));
    }

    protected void process(OrderBook orderBook, ExchangeEnum exchange, String topic) throws JsonProcessingException {
        log.info("Order book: {}", orderBook);
        kafkaTemplate.send(topic, objectMapper.writeValueAsString(new ExchangeOrderBook(exchange, orderBook)));
    }
}
