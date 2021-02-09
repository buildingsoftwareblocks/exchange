package com.btb.exchange.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class ZookeeperConfig {

    @Value("${backend.zookeeper.host:localhost:2182}")
    private String zookeeperHost;
    @Value("${backend.zookeeper.timeout:PT5s}")
    private Duration timeout;
    @Value("${backend.zookeeper.connection.timeout:PT5s}")
    private Duration connectionTimeout;

    @Bean
    public CuratorFramework newClient() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperHost)
                .sessionTimeoutMs((int) timeout.toMillis())
                .connectionTimeoutMs((int) connectionTimeout.toMillis())
                .retryPolicy(retryPolicy)
                .namespace("backend")
                .build();

        client.start();
        return client;
    }
}
