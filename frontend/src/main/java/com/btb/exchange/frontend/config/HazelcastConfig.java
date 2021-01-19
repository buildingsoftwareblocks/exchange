package com.btb.exchange.frontend.config;

import com.btb.exchange.frontend.service.ExchangeService;
import com.hazelcast.config.Config;
import com.hazelcast.config.cp.SemaphoreConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    Config hazelCastConfig() {
        Config config = new Config().setInstanceName("frontend-hz");
        config.getCPSubsystemConfig()
                .addSemaphoreConfig(new SemaphoreConfig(ExchangeService.HAZELCAST_ORDERBOOKS, true, 1))
                .addSemaphoreConfig(new SemaphoreConfig(ExchangeService.HAZELCAST_OPPORTUNITIES, true, 1));
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
        return config;
    }
}
