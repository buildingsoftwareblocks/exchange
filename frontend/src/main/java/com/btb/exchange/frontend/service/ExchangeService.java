package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

    @Synchronized
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*")
    void process(String orderBook) throws JsonProcessingException {
        log.debug("Order book: {}", orderBook);
        var message = objectMapper.readValue(orderBook, ExchangeOrderBook.class);
        if (message.getCurrencyPair().equals(CurrencyPair.BTC_USDT.toString())) {
            template.convertAndSend("/topic/orderbook", orderBook);
            sent.onNext(orderBook);
        }
    }

    Observable<String> subscribe() {
        return sent;
    }

}
