package com.btb.exchange.shared.dto;

import lombok.*;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ExchangeOrderBook {
    ExchangeEnum exchange;
    CurrencyPair currencyPair;
    OrderBook orderBook;
}
