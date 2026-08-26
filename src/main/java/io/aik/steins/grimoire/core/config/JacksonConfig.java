package io.aik.steins.grimoire.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
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

        //anchor 雪花 ID Long 精度保护：序列化为 String，避免前端 Number 精度丢失
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, new JsonSerializer<Long>() {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value != null ? value.toString() : null);
            }
        });
        longModule.addSerializer(Long.TYPE, new JsonSerializer<Long>() {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value != null ? value.toString() : null);
            }
        });
        mapper.registerModule(longModule);

        return mapper;
    }
}
