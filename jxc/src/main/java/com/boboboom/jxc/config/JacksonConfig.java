package com.boboboom.jxc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON 配置类，统一后端序列化和反序列化行为。 */
@Configuration
public class JacksonConfig {

    /** 配置 JSON 序列化组件。 */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
