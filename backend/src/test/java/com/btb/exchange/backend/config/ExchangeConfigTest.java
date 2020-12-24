package com.btb.exchange.backend.config;

import com.btb.exchange.shared.dto.ExchangeEnum;
import info.bitrich.xchangestream.core.StreamingExchange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
class ExchangeConfigTest {

    @Autowired
    private BeanFactory factory;

    @Test
    void exchangeBeans() {
        Arrays.stream(ExchangeEnum.values()).forEach(e -> {
            var bean = BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, StreamingExchange.class, e.name().toLowerCase());
            assertThat(String.format("Find bean with qualifier: %s", e), bean, is(notNullValue()));
        });
    }
}