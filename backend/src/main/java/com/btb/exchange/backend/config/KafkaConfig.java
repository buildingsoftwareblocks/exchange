package com.btb.exchange.backend.config;

import com.btb.exchange.shared.utils.CurrencyPairUtils;
import com.btb.exchange.shared.utils.TopicUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;

import javax.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final GenericApplicationContext ac;
    private final ConsumerFactory<String, String> consumerFactory;

    @PostConstruct
    public void init() {
        // iterate over currency pairs and register new beans
        CurrencyPairUtils.CurrencyPairs.forEach(cp ->
                ac.registerBean(String.format("topic.%s", cp), NewTopic.class, () -> TopicBuilder.name(TopicUtils.orderBook(cp)).build()));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
