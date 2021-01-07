package com.btb.exchange.shared.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalTime;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Opportunity {
    ExchangeEnum from;
    ExchangeEnum to;
    CurrencyPair currencyPair;
    BigDecimal ask;
    BigDecimal bid;
    LocalTime created;
}