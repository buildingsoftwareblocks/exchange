package com.btb.exchange.backend.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "backend")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationConfig {

    private boolean recording;
    private boolean replay;
    private boolean es;
    private int maxOrders;
    private boolean testing;

    @PostConstruct
    void validate() {
        if (recording && replay) {
            throw new IllegalArgumentException("Recording AND replaying is not supported!");
        }
        if (maxOrders < 0) {
            throw new IllegalArgumentException("maxOrders must be >= 0");
        }
    }
}
