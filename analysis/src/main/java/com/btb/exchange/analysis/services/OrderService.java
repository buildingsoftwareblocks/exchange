package com.btb.exchange.analysis.services;

import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

import static com.btb.exchange.shared.utils.TopicUtils.OPPORTUNITIES;

@Service
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ExchangeService exchangeService;
    private final IAtomicLong counter;

    public OrderService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, ExchangeService exchangeService, HazelcastInstance hazelcastInstance) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.exchangeService = exchangeService;
        this.counter = hazelcastInstance.getCPSubsystem().getAtomicLong("counter");
    }

    /**
     *
     */
    public void processSimpleExchangeArbitrage(Opportunities currencyPairOpportunities) {
        // TODO better asset management
        processSimpleExchangeArbitrage(BigDecimal.valueOf(100000), currencyPairOpportunities);
    }

    /**
     *
     */
    void processSimpleExchangeArbitrage(BigDecimal amount, Opportunities opportunities) {
        var opportunitiesBuilder = Opportunities.builder();

        opportunities.getValues().forEach(opportunity -> {
            // TODO we assume that all these assets are available in the ask order!
            var factor = amount.divide(opportunity.getAsk(), MathContext.DECIMAL64);

            var profit = opportunity.getBid().multiply(factor)
                    .subtract(opportunity.getAsk().multiply(factor))
                    .subtract(exchangeService.transactionBuyFees(amount))
                    .subtract(exchangeService.transportationFees(opportunity.getCurrencyPair()))
                    .subtract(exchangeService.transactionSellFees(amount));
            // if profit
            if (profit.compareTo(BigDecimal.ZERO) > 0) {
                opportunity.setAmount(factor);
                opportunity.setProfit(profit);
                opportunitiesBuilder.value(opportunity);
            }
        });

        try {
            opportunitiesBuilder.order(counter.getAndIncrement());
            var message = opportunitiesBuilder.build();
            log.debug("Send opportunities: {}", message);
            kafkaTemplate.send(OPPORTUNITIES, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
        }
    }
}
