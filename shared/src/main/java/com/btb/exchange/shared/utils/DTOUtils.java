package com.btb.exchange.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DTOUtils {
    private final ObjectMapper objectMapper;

    public DTOUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public <T> T fromDTO(String content, Class<T> valueType) {
        return objectMapper.readValue(content, valueType);
    }

    @SneakyThrows
    public String toDTO(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
