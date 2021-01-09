package com.btb.exchange.analysis.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "analysis")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationConfig {

    private boolean replay;
    private float buyfees;
    private float sellfees;
    private float transportfees;
}
