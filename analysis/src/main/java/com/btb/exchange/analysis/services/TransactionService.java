package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.ExchangeEnum;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionBuyFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.15%
       return amount.multiply(BigDecimal.valueOf(0.0015));
    }

    // TODO differentiate on exchange / currency pair
    public BigDecimal transactionSellFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.15%
        return amount.multiply(BigDecimal.valueOf(0.0015));
    }

    public BigDecimal transportationFees(BigDecimal amount, ExchangeEnum from, ExchangeEnum to, CurrencyPair currencyPair) {
        // just a fixed number to start with
        return switch (currencyPair.base.getCurrencyCode()) {
            case "BTC" -> BigDecimal.valueOf(5);
            case "ETH" -> BigDecimal.valueOf(4);
            default -> BigDecimal.valueOf(1);
        };
    }
}
