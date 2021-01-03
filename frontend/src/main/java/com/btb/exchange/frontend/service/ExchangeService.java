package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

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

    private static final String WEBSOCKET_DESTINATION = "/topic/orderbook";

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;

    @Value("${frontend.refreshrate:500}")
    private int refreshRate;

    @Setter
    private ExchangeEnum exchange = KRAKEN;

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<ExchangeOrderBook> sent = PublishSubject.create();

    private final LinkedBlockingDeque<ExchangeOrderBook> events = new LinkedBlockingDeque<>();
    private ExchangeOrderBook lastMessage = null;

    @KafkaListener(topicPattern = "#{ T(com.btb.exchange.shared.utils.TopicUtils).ORDERBOOK_INPUT_FULL_PREFIX}.*")
    void process(String message) {
            log.debug("Order book received: {}", message);
            try {
                ExchangeOrderBook exchangeOrderBook = objectMapper.readValue(message, ExchangeOrderBook.class);
                if (exchangeOrderBook.getExchange().equals(exchange) && (exchangeOrderBook.getCurrencyPair().equals(getFirstCurrencyPair()))) {
                    events.add(exchangeOrderBook);
                }
            } catch (JsonProcessingException e) {
                log.error("Exception({}) with message: {}", e, message);
            }
    }

    Observable<ExchangeOrderBook> subscribe() {
        return sent;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sentData() {
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    if (!events.isEmpty()) {
                        // pick the last message and remove older messages from the queue
                        var message = events.peekLast();
                        log.debug("Tick : {}", message);
                        if (!events.isEmpty()) {
                            log.info("data removed: {}", events.size());
                        }
                        events.clear();
                        lastMessage = message;
                        template.convertAndSend(WEBSOCKET_DESTINATION, objectMapper.writeValueAsString(lastMessage));
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
            template.convertAndSend(WEBSOCKET_DESTINATION, objectMapper.writeValueAsString(lastMessage.getOrderBook()));
        }
    }
}
