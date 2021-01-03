package com.btb.exchange.shared.dto;

import lombok.*;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.util.Currency;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Opportunity {
    ExchangeEnum from;
    ExchangeEnum to;
    CurrencyPair currencyPair;
    BigDecimal ask;
    BigDecimal bid;
}
