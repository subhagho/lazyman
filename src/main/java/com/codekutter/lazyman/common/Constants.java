package com.codekutter.lazyman.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class Constants {
    public static final int DEFAULT_STEP_INCREMENT_FACTOR = 100;
    public static final String DEFAULT_WORKING_DIR = String.format("%s/CK/Salesman", System.getProperty("java.io.tmpdir"));

    public static final String CONFIG_WORKING_DIR = "salesman.dir.work";


    /**
     * Get a new instance of the JSON Object mapper.
     *
     * @return - JSON Object mapper.
     */
    public static ObjectMapper getJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
