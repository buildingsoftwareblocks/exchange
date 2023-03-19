package com.btb.exchange.backend.config;

import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongodbConfig {

    /**
     * Make CurrencyPair storable in the MongoDB database
     */
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        var list =
                List.of(CurrencyPairToStringConverter.INSTANCE, StringToCurrencyPairConverter.INSTANCE);
        return new MongoCustomConversions(list);
    }

    @WritingConverter
    enum CurrencyPairToStringConverter implements Converter<CurrencyPair, String> {
        INSTANCE;

        CurrencyPairToStringConverter() {
        }

        public String convert(CurrencyPair source) {
            return source.toString();
        }
    }

    @ReadingConverter
    enum StringToCurrencyPairConverter implements Converter<String, CurrencyPair> {
        INSTANCE;

        StringToCurrencyPairConverter() {
        }

        public CurrencyPair convert(String source) {
            return new CurrencyPair(source);
        }
    }
}
