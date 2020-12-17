package com.example.exchange.backend.config;

import com.example.exchange.backend.service.AbstractExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
class KafkaConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void currencyPairTopis() {
        AbstractExchangeService.CurrencyPairs.stream().forEach(cp -> {
            var bean = context.getBean(String.format("topic.%s", cp));
            assertThat(bean, is(notNullValue()));
        });
    }
}