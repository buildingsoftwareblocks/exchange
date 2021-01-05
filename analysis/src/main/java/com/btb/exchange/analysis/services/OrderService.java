package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.CurrencyPairOpportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

import static com.btb.exchange.shared.utils.TopicUtils.OPPORTUNITIES;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

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
        var validOpportunitiesBuilder = CurrencyPairOpportunities.builder();
        validOpportunitiesBuilder.currencyPair(cpo.getCurrencyPair());
        validOpportunitiesBuilder.created(cpo.getCreated());

        cpo.getOpportunities().forEach(opportunity -> {
            var factor = amount.divide(opportunity.getBid(), MathContext.DECIMAL64);

            var profit = opportunity.getBid().multiply(factor)
                    .subtract(opportunity.getAsk().multiply(factor))
                    .subtract(transactionService.transactionBuyFees(amount, opportunity.getFrom(), opportunity.getCurrencyPair()))
                    .subtract(transactionService.transactionSellFees(amount, opportunity.getTo(),  opportunity.getCurrencyPair()));
            // if profit
            if (profit.compareTo(BigDecimal.ZERO) == 1) {
                validOpportunitiesBuilder.opportunity(opportunity);
            }
        });
        var validOpportunities = validOpportunitiesBuilder.build();
        if (!validOpportunities.getOpportunities().isEmpty()) {
            try {
                log.info("profit action: {}", cpo);
                kafkaTemplate.send(OPPORTUNITIES, objectMapper.writeValueAsString(validOpportunities));
            } catch (JsonProcessingException e) {
                log.error("Exception", e);
            }
        }
    }
}
