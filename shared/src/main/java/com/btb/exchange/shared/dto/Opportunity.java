package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchange.currency.CurrencyPair;

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

    public Opportunity(
            CurrencyPair currencyPair,
            ExchangeEnum from,
            BigDecimal ask,
            ExchangeEnum to,
            BigDecimal bid,
            LocalTime timestamp) {
        this(currencyPair, BigDecimal.ZERO, BigDecimal.ZERO, from, ask, to, bid, timestamp);
    }
}
