package com.btb.exchange.shared.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringDTOUtils {
    private final ObjectMapper objectMapper;

    public StringDTOUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T from(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
            throw new RuntimeException(e.getCause());
        }
    }

    public String to(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
            throw new RuntimeException(e.getCause());
        }
    }
}
