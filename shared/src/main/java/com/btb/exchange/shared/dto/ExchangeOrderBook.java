package com.btb.exchange.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.knowm.xchange.dto.marketdata.OrderBook;

@Value
@AllArgsConstructor
public class ExchangeOrderBook {
    ExchangeEnum exchange;
    String currencyPair;
    OrderBook orderBook;
}
