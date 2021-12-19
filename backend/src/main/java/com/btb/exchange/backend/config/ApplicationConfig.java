package com.btb.exchange.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationConfig {

    @Value("${backend.recording:false}")
    private boolean recording;
    @Value("${backend.replay:false}")
    private boolean replay;
    @Value("${backend.es:true}")
    private boolean es;
    @Value("${backend.orders.max:5}")
    private int maxOrders;
    @Value("${backend.testing:false}")
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
