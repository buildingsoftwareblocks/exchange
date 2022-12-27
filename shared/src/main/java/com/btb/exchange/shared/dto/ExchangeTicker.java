package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ExchangeTicker {
  long order;

  @JsonFormat(pattern = "HH:mm:ss.SSS")
  LocalTime timestamp;

  ExchangeEnum exchange;
  String id;
  CurrencyPair currencyPair;
  Ticker ticker;
}
