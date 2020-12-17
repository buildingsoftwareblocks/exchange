package com.example.exchange.backend.config;

import com.example.exchange.backend.service.AbstractExchangeService;
import com.example.exchange.shared.utils.TopicUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.config.TopicBuilder;

import javax.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
@Order(0)
public class KafkaConfig {

    private final GenericApplicationContext ac;

    @PostConstruct
    public void init() {
        // iterate over currency pairs and register new beans
        AbstractExchangeService.CurrencyPairs.stream().forEach(cp ->
            ac.registerBean(String.format("topic.%s", cp), NewTopic.class, () -> TopicBuilder.name(TopicUtils.orderBook(cp)).build()));
    }
}
