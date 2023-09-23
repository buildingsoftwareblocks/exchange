package com.btb.exchange.analysis.config;

import com.btb.exchange.shared.utils.TopicUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final ConsumerFactory<String, String> consumerFactory;

    @Value("${analysis.kafka.partitions:10}")
    private int partitions;

    @Value("${analysis.kafka.concurrency:1}")
    private int concurrency;

    @Value("${analysis.kafka.replication:1}")
    private short replication;

    @Bean
    public NewTopic opportunities() {
        return new NewTopic(TopicUtils.OPPORTUNITIES, partitions, replication);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        return factory;
    }
}
