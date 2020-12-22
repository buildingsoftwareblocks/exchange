package com.btb.exchange.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "backend")
@Data
public class ApplicationConfig {

    private boolean recording;
    private boolean replay;

    @PostConstruct
    void validate() {
        if (recording && replay) {
            throw new IllegalArgumentException("Recording AND replaying is not supported!");
        }
    }
}
