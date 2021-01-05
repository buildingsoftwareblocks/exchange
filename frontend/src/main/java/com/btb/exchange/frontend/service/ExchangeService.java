package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.CurrencyPairOpportunities;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;

    @Value("${frontend.refreshrate:500}")
    private int refreshRate;

    @Setter
    private ExchangeEnum exchange = KRAKEN;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<ExchangeOrderBook> sent = PublishSubject.create();

    private final LinkedBlockingDeque<ExchangeOrderBook> orderBooks = new LinkedBlockingDeque<>();
    private final Map<CurrencyPair, CurrencyPairOpportunities> opportunitiesMap = new HashMap<>();
    private ExchangeOrderBook lastMessage = null;

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_PREFIX}.*", containerFactory = "batchFactory")
    void processOrderBooks(List<String> messages) {
        log.debug("process {} messages", messages.size());
        messages.forEach(message -> {
            log.debug("Order book received: {}", message.length());
            try {
                ExchangeOrderBook exchangeOrderBook = objectMapper.readValue(message, ExchangeOrderBook.class);
                if (exchangeOrderBook.getExchange().equals(exchange) && (exchangeOrderBook.getCurrencyPair().equals(getFirstCurrencyPair()))) {
                    orderBooks.add(exchangeOrderBook);
                }
            } catch (JsonProcessingException e) {
                log.error("Exception({}) with message: {}", e, message);
            }
        });
    }

    @Async
    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).OPPORTUNITIES}", containerFactory = "batchFactory")
    void processOpportunities(List<String> messages) {
        messages.forEach(message -> {
            log.info("Opportunities received: {}", message);
            try {
            var cpo = objectMapper.readValue(message, CurrencyPairOpportunities.class);
                opportunitiesMap.put(cpo.getCurrencyPair(), cpo);
                var builder = Opportunities.builder();
                opportunitiesMap.forEach((k,v) -> builder.value(v));
                var result = builder.build();
                template.convertAndSend(WEBSOCKET_OPPORTUNITIES, objectMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                log.error("Exception({}) with message: {}", e, message);
            }
        });
    }

    Observable<ExchangeOrderBook> subscribe() {
        return sent;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sentData() {
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!orderBooks.isEmpty()) {
                        // pick the last message and remove older messages from the queue
                        var message = orderBooks.peekLast();
                        log.debug("Tick : {}", message);
                        if (!orderBooks.isEmpty()) {
                            log.debug("data removed: {}", orderBooks.size());
                        }
                        orderBooks.clear();
                        lastMessage = message;
                        template.convertAndSend(WEBSOCKET_ORDERBOOK, objectMapper.writeValueAsString(lastMessage));
                        sent.onNext(message);
                    }
                });
    }

    /**
     * Show something even the replay of events is over.
     */
    @SneakyThrows
    @EventListener
    public void handleSessionConnected(SessionSubscribeEvent event) {
        log.info("Session connected: {}", event);
        if (lastMessage != null) {
            template.convertAndSend(WEBSOCKET_ORDERBOOK, objectMapper.writeValueAsString(lastMessage.getOrderBook()));
        }
    }
}
