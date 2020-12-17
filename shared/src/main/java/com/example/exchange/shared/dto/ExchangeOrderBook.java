package com.example.exchange.shared.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;

@Value
@AllArgsConstructor
@Jacksonized @Builder
public class ExchangeOrderBook {
    ExchangeEnum exchange;
    OrderBook orderBook;
}
