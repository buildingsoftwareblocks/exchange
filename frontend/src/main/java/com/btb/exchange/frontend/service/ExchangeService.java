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

import static com.btb.exchange.shared.dto.ExchangeEnum.BINANCE;

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
    void process(String message) throws JsonProcessingException {
        log.info("Order book: {}", message);
        var exchangeOrderBook = objectMapper.readValue(message, ExchangeOrderBook.class);
        if (exchangeOrderBook.getExchange().equals(BINANCE) && (exchangeOrderBook.getCurrencyPair().equals(CurrencyPair.ETH_BTC.toString()))) {
            template.convertAndSend("/topic/orderbook", exchangeOrderBook.getOrderBook());
        }
    }

    Observable<String> subscribe() {
        return sent;
    }
}
