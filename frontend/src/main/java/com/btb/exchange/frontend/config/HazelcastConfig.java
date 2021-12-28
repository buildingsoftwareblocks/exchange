package com.btb.exchange.frontend.config;

import com.btb.exchange.frontend.hazelcast.ExchangeDataSerializableFactory;
import com.btb.exchange.frontend.service.ExchangeService;
import com.hazelcast.config.Config;
import com.hazelcast.config.cp.SemaphoreConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HazelcastConfig {

    @Value("${frontend.multicast.enabled:true}")
    private boolean multicast;

    @Bean
    Config hazelCastConfig() {
        Config config = new Config().setClusterName("frontend-hz");
        config.getSerializationConfig().addDataSerializableFactory(ExchangeDataSerializableFactory.FACTORY_ID, new ExchangeDataSerializableFactory());
        config.getCPSubsystemConfig()
                // TODO check if still needed
                .addSemaphoreConfig(new SemaphoreConfig(ExchangeService.HAZELCAST_ORDERBOOKS, true, 1))
                .addSemaphoreConfig(new SemaphoreConfig(ExchangeService.HAZELCAST_OPPORTUNITIES, true, 1));
        if (multicast) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        } else {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
        }
        return config;
    }

    @Bean
    @Primary
    HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }
}
