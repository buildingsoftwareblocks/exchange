package com.btb.exchange.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.knowm.xchange.dto.marketdata.OrderBook;

@Value
@AllArgsConstructor
@Jacksonized
@Builder
public class ExchangeOrderBook {
    ExchangeEnum exchange;
    String currencyPair;
    OrderBook orderBook;
}
