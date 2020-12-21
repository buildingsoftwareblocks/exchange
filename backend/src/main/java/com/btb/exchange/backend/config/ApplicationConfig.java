package com.btb.exchange.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backend")
@Data
public class ApplicationConfig {

    private boolean recording;
    private boolean replay;
}
