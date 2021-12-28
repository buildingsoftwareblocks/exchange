package com.btb.exchange.frontend.service;

import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@SessionScope
@Slf4j
@RequiredArgsConstructor
public class UserState {

    private final ExchangeService exchangeService;
    private final WebSocketService webSocketService;

    private Optional<ExchangeEnum> selectedExchange;
    private Optional<CurrencyPair> selectedCurrencyPair;
    // HTTP session ID
    private String sessionId;

    @PostConstruct
    void postConstruct() {
        // set default values
        selectedExchange = exchangeService.activeExchanges().stream().findFirst();
        selectedCurrencyPair = getFirstCP(selectedExchange);
    }

    public List<CurrencyPair> activeCurrencies() {
        if (selectedExchange.isPresent()) {
            return exchangeService.activeCurrencies(selectedExchange.get());
        } else {
            return Collections.emptyList();
        }
    }

    public void changeExchange(ExchangeEnum exchange) {
        // if value is changed
        if (!selectedExchange.equals(Optional.ofNullable(exchange))) {
            selectedExchange = Optional.ofNullable(exchange);
            getFirstCP(selectedExchange);
            webSocketService.register(sessionId, selectedExchange, selectedCurrencyPair);
        }
    }

    Optional<CurrencyPair> getFirstCP(Optional<ExchangeEnum> exchange) {
        return exchange.flatMap(exchangeEnum -> exchangeService.activeCurrencies(exchangeEnum).stream().findFirst());
    }

    public void changeCurrency(CurrencyPair currencyPair) {
        selectedCurrencyPair = Optional.ofNullable(currencyPair);
        webSocketService.register(sessionId, selectedExchange, selectedCurrencyPair);
    }

    public void init(String sessionId) {
        this.sessionId = sessionId;
        webSocketService.register(sessionId, selectedExchange, selectedCurrencyPair);
    }
}
