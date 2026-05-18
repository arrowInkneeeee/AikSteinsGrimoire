package io.aik.steins.grimoire.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.TimeZone;

/**
 * Jackson 配置 -anchor
 *
 * @author a I k .
 */
@Configuration
public class JacksonConfig {

    /**
     * 定制 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        //anchor 统一时区
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        //anchor 注册 Java 8 时间模块，支持 LocalDateTime 等
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //anchor 忽略 null 值字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        //anchor 遇到未知字段不报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
