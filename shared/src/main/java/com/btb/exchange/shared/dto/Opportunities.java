package com.btb.exchange.shared.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.List;

@Data
@Builder
@Jacksonized
public class Opportunities {
    @Singular
    List<Opportunity> values;
}
