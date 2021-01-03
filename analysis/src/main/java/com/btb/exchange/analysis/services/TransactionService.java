package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.ExchangeEnum;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {

    // TODO differentiate for exchange / currency pai
    public BigDecimal transactionBuyFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.2%
        return amount.multiply(BigDecimal.valueOf(0.002));
    }

    // TODO differentiate for exchange / currency pai
    public BigDecimal transactionSellFees(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.2%
        return amount.multiply(BigDecimal.valueOf(0.002));
    }
}
