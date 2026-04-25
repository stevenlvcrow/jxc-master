package com.boboboom.jxc.common.logging;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 请求体缓存过滤器，负责让访问日志可重复读取请求体。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiBodyCachingFilter extends OncePerRequestFilter {

    private static final int BYTES_PER_MIB = 1024 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, BYTES_PER_MIB);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        boolean completed = false;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            completed = true;
        } finally {
            if (completed) {
                wrappedResponse.copyBodyToResponse();
            }
        }
    }
}
