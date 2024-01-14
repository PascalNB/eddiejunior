package com.thefatrat.eddiejunior.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperProvider {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .findAndRegisterModules()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

}
