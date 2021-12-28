package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WebSocketService {

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";
    private static final String WEBSOCKET_EXCHANGES = "/topic/exchanges";

    @Value("${frontend.refreshrate:1000}")
    private int refreshRate;

    private final ExchangeService exchangeService;
    private final SimpMessagingTemplate template;
    private final DistributionSummary wsMessagesCounter;

    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, FilterValue> filters = new ConcurrentHashMap<>();

    // for testing purposes, to subscribe to the event that send to the websocket
    private final Subject<String> sent = PublishSubject.create();

    /**
     *
     */
    public WebSocketService(ExchangeService exchangeService, SimpMessagingTemplate template, MeterRegistry registry) {
        this.exchangeService = exchangeService;
        this.template = template;

        wsMessagesCounter = DistributionSummary.builder("frontend.ws.queue")
                .description("indicates the size of message send to the web socket")
                .baseUnit("bytes")
                .register(registry);
    }

    @PostConstruct
    void init() {
        // Send data to web sessions on a regular interval
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> {
                    filters.forEach((key, value) -> {
                        String userId = sessions.get(key);
                        if (userId != null) {
                            sendOrderBook(userId, value);
                            sendOpportunities(userId);
                            sendExchanges(userId);
                        } else {
                            log.warn("No user ID for session: {}", key);
                        }
                    });
                });
    }

    /**
     * register HTTP session with selection criteria
     */
    public void register(String sessionId, Optional<ExchangeEnum> exchange, Optional<CurrencyPair> currencyPair) {
        if (exchange.isPresent() && currencyPair.isPresent()) {
            filters.put(sessionId, new FilterValue(exchange.get(), currencyPair.get()));
        }
    }

    public void register(String sessionId, String userId) {
        sessions.put(sessionId, userId);
    }


//    public void init(String sessionId) {
//        log.info("init() : {}", sessionId);
//        this.sessionId = sessionId;
//
//        sendOrderBook();
//        sendOpportunities();
//        sendExchanges();
//
//        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
//                .subscribe(e -> {
//                    sendOrderBook();
//                    sendOpportunities();
//                    sendExchanges();
//                });
//    }

    void sendExchanges(String userId) {
        exchangeService.exchangesData().ifPresent(message -> {
            wsMessagesCounter.record(message.length());
            template.convertAndSendToUser(userId, WEBSOCKET_EXCHANGES, message);
        });
    }

    void sendOpportunities(String userId) {
        exchangeService.opportunitiesData().ifPresent(message -> {
            log.debug("Send opportunities: '{}'", message);
            wsMessagesCounter.record(message.length());
            template.convertAndSendToUser(userId, WEBSOCKET_OPPORTUNITIES, message);
        });
    }

    void sendOrderBook(String userId, FilterValue filter) {
        exchangeService.orderbookData(filter.exchange, filter.currencyPair).ifPresent(message -> {
            log.debug("Send orderbook: '{}/{}'", filter.exchange, filter.currencyPair);
            wsMessagesCounter.record(message.length());
            template.convertAndSendToUser(userId, WEBSOCKET_ORDERBOOK, message);
        });
    }

    Observable<String> subscribe() {
        return sent;
    }

    @Data
    @AllArgsConstructor
    public static class FilterValue {
        private ExchangeEnum exchange;
        private CurrencyPair currencyPair;
    }
}
