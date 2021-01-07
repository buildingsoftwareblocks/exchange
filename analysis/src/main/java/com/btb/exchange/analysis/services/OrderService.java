package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.data.CurrencyPairOpportunities;
import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import static com.btb.exchange.shared.utils.TopicUtils.OPPORTUNITIES;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    private final Map<CurrencyPair, Opportunities> opportunitiesMap = new HashMap<>();

    /**
     *
     */
    public void processSimpleExchangeArbitrage(CurrencyPairOpportunities currencyPairOpportunities) {
        // TODO better asset management
        processSimpleExchangeArbitrage(BigDecimal.valueOf(100000), currencyPairOpportunities);
    }

    /**
     *
     */
    void processSimpleExchangeArbitrage(BigDecimal amount, CurrencyPairOpportunities cpo) {
        var validOpportunitiesBuilder = Opportunities.builder();

        cpo.getOpportunities().forEach(opportunity -> {
            var factor = amount.divide(opportunity.getBid(), MathContext.DECIMAL64);

            var profit = opportunity.getBid().multiply(factor)
                    .subtract(opportunity.getAsk().multiply(factor))
                    .subtract(transactionService.transactionBuyFees(amount, opportunity.getFrom(), opportunity.getCurrencyPair()))
                    .subtract(transactionService.transportationFees(amount, opportunity.getFrom(), opportunity.getTo(), opportunity.getCurrencyPair()))
                    .subtract(transactionService.transactionSellFees(amount, opportunity.getTo(),  opportunity.getCurrencyPair()));
            // if profit
            if (profit.compareTo(BigDecimal.ZERO) > 0) {
                validOpportunitiesBuilder.value(opportunity);
            }
        });
        opportunitiesMap.put(cpo.getCurrencyPair(), validOpportunitiesBuilder.build());

        var builder = Opportunities.builder();
        opportunitiesMap.forEach((k,v) -> v.getValues().forEach(builder::value));
        var result = builder.build();

        try {
            log.info("profit action: {}", result);
            kafkaTemplate.send(OPPORTUNITIES, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
    }
}
