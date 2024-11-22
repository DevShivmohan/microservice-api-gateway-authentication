package com.micro.common.model.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperFactor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String writeValuesAsString(Object data){
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
