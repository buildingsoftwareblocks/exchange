package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.config.ApplicationConfig;
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
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final ApplicationConfig config;

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionBuyFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
       return amount.multiply(BigDecimal.valueOf(config.getBuyfee()));
    }

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionSellFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        return amount.multiply(BigDecimal.valueOf(config.getSellfee()));
    }

    public BigDecimal transportationFees(BigDecimal amount, ExchangeEnum from, ExchangeEnum to, CurrencyPair currencyPair) {
        // just a fixed number to start with
        return switch (currencyPair.base.getCurrencyCode()) {
            case "BTC" -> BigDecimal.valueOf(5);
            case "ETH" -> BigDecimal.valueOf(4);
            default -> BigDecimal.valueOf(1);
        };
    }

    public boolean validData( @NonNull ExchangeEnum exchange,  @NonNull CurrencyPair currencyPair,  @NonNull LocalTime time) {
        return validData(exchange, currencyPair, LocalTime.now(), time);
    }

    /**
     * max acceptable delay in ms of received data
     */
    // TODO differentiate on exchange / currency pair
    public boolean validData( @NonNull ExchangeEnum exchange,  @NonNull CurrencyPair currencyPair, @NonNull LocalTime now,  @NonNull LocalTime time) {
        if (config.isReplay()) {
            return true;
        } else {
            return now.minus(5000, ChronoUnit.MILLIS).isBefore(time);
        }
    }
}
