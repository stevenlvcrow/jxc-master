package com.boboboom.jxc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 跨域配置属性，承载允许访问的前端来源。 */
@ConfigurationProperties(prefix = "server.cors")
public class CorsProperties {

    private String[] allowedOriginPatterns = new String[0];

    /** 获取AllowedOriginPatterns。 */
    public String[] getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    /** 设置AllowedOriginPatterns。 */
    public void setAllowedOriginPatterns(String[] allowedOriginPatternsValue) {
        this.allowedOriginPatterns = allowedOriginPatternsValue;
    }
}
