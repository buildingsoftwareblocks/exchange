package com.btb.exchange.analysis.data;

import com.btb.exchange.shared.dto.Opportunity;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.List;

@Data
@Builder
public class CurrencyPairOpportunities {
    CurrencyPair currencyPair;
    @Singular
    List<Opportunity> opportunities;
}
