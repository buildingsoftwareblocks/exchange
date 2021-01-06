package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Currency;
import java.util.Date;

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
