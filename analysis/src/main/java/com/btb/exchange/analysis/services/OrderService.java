package com.btb.exchange.analysis.services;

import com.btb.exchange.analysis.simple.SimpleExchangeArbitrage;
import com.btb.exchange.shared.dto.Opportunities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

import static com.btb.exchange.shared.utils.TopicUtils.OPPORTUNITIES;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;
    private final SimpleExchangeArbitrage simpleExchangeArbitrage;


    @PostConstruct
    void init() {
        simpleExchangeArbitrage.getOpportunities().subscribe(this::processSimpleExchangeArbitrage);
    }

    private void processSimpleExchangeArbitrage(Opportunities opportunities) {
        opportunities.getValues().forEach(o -> {
            var amount = BigDecimal.valueOf(10000);
            var profit = o.getBid()
                    .subtract(o.getAsk())
                    .subtract(transactionService.transactionBuyFees(amount, o.getFrom(), o.getCurrencyPair()))
                    .subtract(transactionService.transactionSellFees(amount, o.getTo(),  o.getCurrencyPair()));
            // if profit
            if (profit.compareTo(BigDecimal.ZERO) == 1) {
                try {
                    log.info("profit action: {}", opportunities);
                    kafkaTemplate.send(OPPORTUNITIES, objectMapper.writeValueAsString(opportunities));
                } catch (JsonProcessingException e) {
                    log.error("Exception", e);
                }
            }
        });

    }
}
