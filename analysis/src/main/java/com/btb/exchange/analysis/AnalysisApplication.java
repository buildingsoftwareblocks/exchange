package com.btb.exchange.analysis;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCommonComponents
@EnableAsync
@Generated // prevent JoCoCo from complaining
public class AnalysisApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnalysisApplication.class, args);
  }
}
