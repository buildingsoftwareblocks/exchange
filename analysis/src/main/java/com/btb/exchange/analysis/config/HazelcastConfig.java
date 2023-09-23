package com.btb.exchange.analysis.config;

import com.btb.exchange.analysis.hazelcast.ExchangeDataSerializableFactory;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

  @Value("${analysis.hazelcast.multicast.enabled:true}")
  private boolean multicast;

  @Value("${analysis.hazelcast.cluster.name:dev}")
  private String clusterName;

  @Bean
  public Config hazelCastConfig() {
    Config config = new Config().setClusterName(clusterName);
    config
        .getSerializationConfig()
        .addDataSerializableFactory(
            ExchangeDataSerializableFactory.FACTORY_ID, new ExchangeDataSerializableFactory());

    if (multicast) {
      config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
    } else {
      config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
      config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
    }
    return config;
  }

  @Bean
  public HazelcastInstance hazelcastInstance(Config config) {
    return Hazelcast.newHazelcastInstance(config);
  }
}
