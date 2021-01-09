package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Opportunity {
    private CurrencyPair currencyPair;
    private BigDecimal amount;
    private BigDecimal profit;
    private ExchangeEnum from;
    private BigDecimal ask;
    private ExchangeEnum to;
    private BigDecimal bid;
    @JsonFormat(pattern = "HH:mm:ss.SSS")
    private LocalTime created;

    public Opportunity(CurrencyPair currencyPair, BigDecimal profit, ExchangeEnum from, BigDecimal ask, ExchangeEnum to, BigDecimal bid) {
        this(currencyPair, BigDecimal.ONE, profit, from, ask, to, bid, LocalTime.now());
    }
}
