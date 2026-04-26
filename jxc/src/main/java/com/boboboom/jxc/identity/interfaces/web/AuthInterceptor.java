package com.boboboom.jxc.identity.interfaces.web;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.TokenService;
import com.boboboom.jxc.identity.application.auth.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 身份与权限拦截器，负责请求上下文和访问控制处理。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    /** 身份与权限拦截器，负责请求上下文和访问控制处理。 */
    public AuthInterceptor(TokenService tokenServiceValue) {
        this.tokenService = tokenServiceValue;
    }

    /** 请求进入业务处理前记录访问上下文。 */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("缺少认证令牌");
        }
        LoginSession session = tokenService.getSession(token);
        if (session == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        AuthContextHolder.set(session);
        return true;
    }

    /** 请求完成后记录响应状态和耗时。 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return request.getHeader("X-Auth-Token");
    }
}
