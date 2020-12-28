package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.btb.exchange.shared.dto.ExchangeEnum.KRAKEN;
import static com.btb.exchange.shared.utils.CurrencyPairUtils.getFirstCurrencyPair;

/**
 * Handle Exchanges
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<String> sent = PublishSubject.create();

    private final LinkedBlockingDeque<String> events = new LinkedBlockingDeque<>();

    @Synchronized
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void process(String message) {
        log.debug("Order book received: {}", message);
        try {
            ExchangeOrderBook exchangeOrderBook = objectMapper.readValue(message, ExchangeOrderBook.class);
            if (exchangeOrderBook.getExchange().equals(KRAKEN) && (exchangeOrderBook.getCurrencyPair().equals(getFirstCurrencyPair().toString()))) {
                events.add(message);
            }
        } catch (JsonProcessingException e) {
            log.error("Exception({}) with message: {}", e, message);
        }
    }

    Observable<String> subscribe() {
        return sent;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sentData() {
        Observable.interval(500, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!events.isEmpty()) {
                        // pick the last message and remove older messages from the queue
                        var message = events.peekLast();
                        log.debug("Tick : {}", message);
                        if (events.size() > 0) {
                            log.info("data removed: {}", events.size());
                        }
                        events.clear();
                        var exchangeOrderBook = objectMapper.readValue(message, ExchangeOrderBook.class);
                        template.convertAndSend("/topic/orderbook", objectMapper.writeValueAsString(exchangeOrderBook.getOrderBook()));
                        sent.onNext(message);
                    }
                });
    }

}
