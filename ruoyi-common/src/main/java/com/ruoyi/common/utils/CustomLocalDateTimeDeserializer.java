package com.ruoyi.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 将json 转化成jdk8的时间
 */
public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final String DATE_TIME_FORMATTER_MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final String DATE_TIME_FORMATTER_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_TIME_FORMATTER_DATE = "yyyy-MM-dd";

    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN);

    private static DateTimeFormatter DATE_TIME_FORMATTER_MILLI = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_MILLI_PATTERN);

    private static DateTimeFormatter DATE_TIME_FORMATTER_SIMPLY = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_DATE);


    @Override
    public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            String str = jp.getText().trim();
            if (DATE_TIME_FORMATTER_PATTERN.length() == str.length()) {
                return LocalDateTime.parse(str, DATE_TIME_FORMATTER);
            } else if (DATE_TIME_FORMATTER_MILLI_PATTERN.length() == str.length()) {
                return LocalDateTime.parse(str, DATE_TIME_FORMATTER_MILLI);
            }else if (DATE_TIME_FORMATTER_DATE.length() == str.length()){
                str += " 00:00:00";
                return LocalDateTime.parse(str, DATE_TIME_FORMATTER);
            }
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return new Timestamp(jp.getLongValue()).toLocalDateTime();
        }
        throw ctxt.mappingException(handledType());
    }

}
