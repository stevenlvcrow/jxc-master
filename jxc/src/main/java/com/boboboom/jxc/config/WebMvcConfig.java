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
    private final CorsProperties corsProperties;

    public WebMvcConfig(ApiAccessLogInterceptor apiAccessLogInterceptor,
                        AuthInterceptor authInterceptor,
                        CorsProperties corsProperties) {
        this.apiAccessLogInterceptor = apiAccessLogInterceptor;
        this.authInterceptor = authInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessLogInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/auth/**", "/api/**")
                .excludePathPatterns(
                        "/api/identity/auth/login",
                        "/api/identity/auth/refresh",
                        "/auth/login",
                        "/auth/refresh",
                        "/error"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
