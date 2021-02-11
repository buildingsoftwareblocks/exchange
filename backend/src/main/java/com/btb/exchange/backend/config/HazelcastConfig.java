package com.btb.exchange.backend.config;

import com.btb.exchange.backend.data.DatabaseService;
import com.hazelcast.config.Config;
import com.hazelcast.config.cp.SemaphoreConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Value("${frontend.multicast.enabled:true}")
    private boolean multicast;

    @Bean
    Config hazelCastConfig() {
        Config config = new Config().setClusterName("backend-hz");
        config.getCPSubsystemConfig().addSemaphoreConfig(new SemaphoreConfig(DatabaseService.HAZELCAST_DB, true, 1));
        if (multicast) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        } else {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
        }
        return config;
    }
}
