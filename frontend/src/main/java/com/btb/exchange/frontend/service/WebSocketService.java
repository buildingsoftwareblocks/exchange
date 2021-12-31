package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.annotation.PostConstruct;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private static final String WEBSOCKET_ORDERBOOK = "/topic/orderbook";
    private static final String WEBSOCKET_OPPORTUNITIES = "/topic/opportunities";
    private static final String WEBSOCKET_EXCHANGES = "/topic/exchanges";

    @Value("${frontend.refreshrate:1000}")
    private int refreshRate;

    private final ExchangeService exchangeService;
    private final SimpMessagingTemplate template;
    private final SimpUserRegistry simpUserRegistry;

    private final Map<String, ExchangeEnum> exchanges = new ConcurrentHashMap<>();
    private final Map<String, CurrencyPair> currencies = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        // Send data to web sessions on a regular interval
        Observable.interval(refreshRate, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribe(e -> simpUserRegistry.getUsers().forEach(u -> send(u.getPrincipal())));
    }

    public void register(Principal principal, ExchangeEnum exchange) {
        exchanges.put(principal.getName(), exchange);
        send(principal);
    }

    public void register(Principal principal, CurrencyPair cp) {
        currencies.put(principal.getName(), cp);
        send(principal);
    }

    @EventListener
    public void sessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser().getName();
        log.info("Connect: {}", userId);
    }

    @EventListener
    public void sessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser().getName();
        log.info("Disconnect: {}", userId);
        exchanges.remove(userId);
        currencies.remove(userId);
    }

    void send(Principal principal) {
        String userId = principal.getName();
        sendOpportunities(userId);
        sendExchanges(userId);

        ExchangeEnum exchange = exchanges.get(userId);
        CurrencyPair cp = currencies.get(userId);
        if (exchange != null && cp != null) {
            sendOrderBook(userId, exchange, cp);
        }
    }

    void sendExchanges(String userId) {
        exchangeService.exchangesData().ifPresent(message -> template.convertAndSendToUser(userId, WEBSOCKET_EXCHANGES, message));
    }

    void sendOpportunities(String userId) {
        exchangeService.opportunitiesData().ifPresent(message -> {
            log.debug("Send opportunities: '{}'", message);
            template.convertAndSendToUser(userId, WEBSOCKET_OPPORTUNITIES, message);
        });
    }

    void sendOrderBook(String userId, ExchangeEnum exchange, CurrencyPair currencyPair) {
        exchangeService.orderbookData(exchange, currencyPair).ifPresent(message -> {
            log.debug("Send orderbook: '{}/{}'", exchange, currencyPair);
            template.convertAndSendToUser(userId, WEBSOCKET_ORDERBOOK, message);
        });
    }
}
