package com.btb.exchange.frontend;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCommonComponents
@Generated // prevent JoCoCo from complaining
public class FrontendApplication {

  public static void main(String[] args) {
    SpringApplication.run(FrontendApplication.class, args);
  }
}
