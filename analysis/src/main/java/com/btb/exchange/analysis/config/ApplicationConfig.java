package com.btb.exchange.analysis.config;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "analysis")
@Data
@Builder
public class ApplicationConfig {

    private boolean replay;
    private double buyfees;
    private double sellfees;
    private double transportfees;
}
