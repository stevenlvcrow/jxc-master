package com.boboboom.jxc.config;

import com.boboboom.jxc.common.logging.ApiAccessLogInterceptor;
import com.boboboom.jxc.identity.interfaces.web.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiAccessLogInterceptor apiAccessLogInterceptor;
    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(ApiAccessLogInterceptor apiAccessLogInterceptor, AuthInterceptor authInterceptor) {
        this.apiAccessLogInterceptor = apiAccessLogInterceptor;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessLogInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/auth/me", "/auth/logout", "/org/**")
                .addPathPatterns("/api/identity/auth/me", "/api/identity/auth/logout", "/api/identity/org/**")
                .addPathPatterns("/api/identity/**")
                .addPathPatterns("/api/items/**")
                .addPathPatterns("/api/inventory/**")
                .addPathPatterns("/api/workflow/**")
                .addPathPatterns("/api/users/**")
                .excludePathPatterns(
                        "/api/identity/auth/login",
                        "/api/identity/auth/refresh",
                        "/auth/login",
                        "/auth/refresh"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
