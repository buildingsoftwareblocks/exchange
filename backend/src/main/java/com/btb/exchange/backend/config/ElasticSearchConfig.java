package com.btb.exchange.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

// @Configuration
@Slf4j
public class ElasticSearchConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris:localhost:9200}")
  private String elasticsearchHost;

  @Override
  public @NotNull ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder().connectedTo(elasticsearchHost).build();
  }
}
