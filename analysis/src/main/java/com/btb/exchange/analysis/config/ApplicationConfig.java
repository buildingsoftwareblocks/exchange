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

  private boolean replay = false;
  private double buyfees = 0.0015;
  private double sellfees = 0.0015;
  private double transportfees = 0.1;
}
