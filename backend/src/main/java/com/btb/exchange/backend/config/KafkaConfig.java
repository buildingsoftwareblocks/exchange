package com.btb.exchange.backend.config;

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

  @Value("${backend.kafka.partitions:10}")
  private int partitions;

  @Value("${backend.kafka.concurrency:5}")
  private int concurrency;

  @Value("${backend.kafka.replication:1}")
  private short replication;

  @Bean
  public NewTopic orderbook() {
    return new NewTopic(TopicUtils.INPUT_ORDERBOOK, partitions, replication);
  }

  @Bean
  public NewTopic ticker() {
    return new NewTopic(TopicUtils.INPUT_TICKER, partitions, replication);
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
