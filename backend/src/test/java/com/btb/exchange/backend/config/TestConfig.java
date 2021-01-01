package com.btb.exchange.backend.config;

import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"com.btb.exchange.backend.config"})
@Import(KafkaAutoConfiguration.class)
class TestConfig {
}