package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.config.ApplicationConfig;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

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

    public boolean validData(LocalTime time) {
        return validData(LocalTime.now(), time);
    }

    /**
     * max acceptable delay in ms of received data
     */
    // TODO better config per exchange/currencypair
    public boolean validData(@NonNull LocalTime now, LocalTime time) {
        if (config.isReplay()) {
            return true;
        } else {
            if (time != null) {
                return now.minusMinutes(2).isBefore(time);
            } else {
                return true;
            }
        }
    }
}
