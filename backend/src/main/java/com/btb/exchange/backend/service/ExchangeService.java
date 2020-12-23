package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.backend.data.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final StreamingExchange exchange;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NewTopic topic;
    private final ApplicationConfig config;

    /**
     *  for testing purposes, to subscribe to the event are broadcasted.
     *  It's 'BehaviorSubject' so we can proces the events, even if  the service is already started via the 'Application Ready event'
      */
    private final Subject<String> messageSent = BehaviorSubject.createDefault("");

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // only realtime data if we are not replaying database content
        if (!config.isReplay()) {
            // Connect to the Exchange WebSocket API. Here we use a blocking wait.
            exchange.connect().blockingAwait();
            // Subscribe order book data with the reference to the subscription.
            exchange.getStreamingMarketDataService()
                    .getOrderBook(CurrencyPair.BTC_USD)
                    .subscribe(this::process);
        }
    }

    Observable<String> subscribe() {
        return messageSent;
    }

    public void process(OrderBook orderBook) throws JsonProcessingException {
        log.trace("Order book: {}", orderBook);
        var future = kafkaTemplate.send(topic.name(), objectMapper.writeValueAsString(orderBook));

        future.addCallback(new ListenableFutureCallback<>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                messageSent.onNext(result.getRecordMetadata().topic());
            }

            @Override
            public void onFailure(@NonNull Throwable e) {
                log.error("Exception", e);
            }
        });
    }

    @PreDestroy
    void teardown() {
        // Disconnect from exchange (blocking again)
        exchange.disconnect().blockingAwait();
    }
}
