package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.knowm.xchange.currency.CurrencyPair;

import java.time.LocalTime;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ExchangeOrderBook {
    long order;
    @JsonFormat(pattern = "HH:mm:ss.SSS")
    LocalTime timestamp;
    ExchangeEnum exchange;
    String id;
    CurrencyPair currencyPair;
    Orders orders;
}
