package com.btb.exchange.backend;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCommonComponents
@EnableAsync
@Generated // prevent JoCoCo from complaining
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
