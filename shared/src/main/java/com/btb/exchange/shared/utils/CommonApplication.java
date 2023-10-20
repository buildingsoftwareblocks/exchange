package com.btb.exchange.shared.utils;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class CommonApplication {

    public static ConfigurableApplicationContext run(Class<?> primary, String[] args) {
        SpringApplication springApplication = new SpringApplication(primary);
        springApplication.addListeners(new PropertyLogger());
        return springApplication.run(args);
    }
}
