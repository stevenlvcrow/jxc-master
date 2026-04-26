package com.boboboom.jxc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.boboboom.jxc.common.logging.ApiAccessLogInterceptor;
import com.boboboom.jxc.identity.interfaces.web.AuthInterceptor;

/** Web MVC 配置类，注册拦截器和跨域规则。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final long DEFAULT_CORS_MAX_AGE_SECONDS = 3600L;

    private final ApiAccessLogInterceptor apiAccessLogInterceptor;
    private final AuthInterceptor authInterceptor;
    private final CorsProperties corsProperties;

    /** Web MVC 配置类，注册拦截器和跨域规则。 */
    public WebMvcConfig(ApiAccessLogInterceptor apiAccessLogInterceptorValue,
                        AuthInterceptor authInterceptorValue,
                        CorsProperties corsPropertiesValue) {
        this.apiAccessLogInterceptor = apiAccessLogInterceptorValue;
        this.authInterceptor = authInterceptorValue;
        this.corsProperties = corsPropertiesValue;
    }

    /** 注册 Web 请求拦截器。 */
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

    /** 配置跨域访问规则。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(DEFAULT_CORS_MAX_AGE_SECONDS);
    }
}
