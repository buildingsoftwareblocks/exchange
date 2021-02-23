package com.btb.exchange.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DTOUtils {

    private final StringDTOUtils stringDTOUtils;
    private final ByteDTOUtils byteDTOUtils;
    private final boolean stringResponse;

    public DTOUtils(ObjectMapper objectMapper, boolean stringResponse) {
        stringDTOUtils = new StringDTOUtils(objectMapper);
        byteDTOUtils = new ByteDTOUtils();
        this.stringResponse = stringResponse;
    }

    public <T> T from(Object content, Class<T> valueType) {
        if (stringResponse) {
            return stringDTOUtils.from((String) content, valueType);
        } else {
            return byteDTOUtils.from((byte[]) content, valueType);
        }
    }

    public Object to(Object content) {
        if (stringResponse) {
            return stringDTOUtils.to(content);
        } else {
            return byteDTOUtils.to(content);
        }
    }
}
