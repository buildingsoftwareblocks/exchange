package com.btb.exchange.shared.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;

import java.time.LocalTime;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ExchangeOrderBook {
    long order;
    LocalTime timestamp;
    ExchangeEnum exchange;
    CurrencyPair currencyPair;
    OrderBook orderBook;
}
