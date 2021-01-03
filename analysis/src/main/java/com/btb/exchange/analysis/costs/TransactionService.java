package com.btb.exchange.analysis.costs;

import com.btb.exchange.shared.dto.ExchangeEnum;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.BitSet;

@Service
public class TransactionService {

    // TODO differentiate for exchange / currency pai
    public BigDecimal transactionFeesBuy(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.2%
        return amount.multiply(BigDecimal.valueOf(0.002));
    }

    // TODO differentiate for exchange / currency pai
    public BigDecimal transactionFeesSell(BigDecimal amount, ExchangeEnum exchange, CurrencyPair currencyPair) {
        // 0.2%
        return amount.multiply(BigDecimal.valueOf(0.002));
    }
}
