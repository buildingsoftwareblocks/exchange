package com.btb.exchange.shared.dto;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class DTOUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T fromDTO(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
           log.error("Exception", e);
           throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static String toDTO(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
