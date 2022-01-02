package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.config.ApplicationConfig;
import com.btb.exchange.analysis.hazelcast.ExchangeCPKey;
import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private final ApplicationConfig config;

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionBuyFees(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(config.getBuyfees()));
    }

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionSellFees(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(config.getSellfees()));
    }

    public BigDecimal transportationFees(CurrencyPair currencyPair) {
        // just a fixed number to start with
        return switch (currencyPair.base.getCurrencyCode()) {
            case "BTC" -> BigDecimal.valueOf(config.getTransportfees() * 5);
            case "ETH" -> BigDecimal.valueOf(config.getTransportfees() * 4);
            default -> BigDecimal.valueOf(config.getTransportfees());
        };
    }

    public boolean validData(@NonNull ExchangeEnum exchange, @NonNull CurrencyPair currencyPair, LocalTime time) {
        return validData(LocalTime.now(), time, new ExchangeCPKey(exchange, currencyPair));
    }

    /**
     * max acceptable delay in ms of received data
     */
    // TODO better config per exchange/currencypair
    public boolean validData(@NonNull LocalTime now, LocalTime time, ExchangeCPKey exchangeCPKey) {
        if (config.isReplay()) {
            return true;
        } else {
            if (time != null) {
                return now.minus(2, ChronoUnit.MINUTES).isBefore(time);
            } else {
                return true;
            }
        }
    }
}
