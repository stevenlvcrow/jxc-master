package com.boboboom.jxc.config;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 令牌认证过滤器，负责从请求中解析登录令牌并写入安全上下文。 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    public static final String AUTH_FAILURE_MESSAGE_ATTR = TokenAuthenticationFilter.class.getName() + ".AUTH_FAILURE_MESSAGE";

    private final TokenService tokenService;

    /** 令牌认证过滤器，负责从请求中解析登录令牌并写入安全上下文。 */
    public TokenAuthenticationFilter(TokenService tokenServiceValue) {
        this.tokenService = tokenServiceValue;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!HttpMethod.OPTIONS.matches(request.getMethod())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);
            if (token == null || token.isBlank()) {
                request.setAttribute(AUTH_FAILURE_MESSAGE_ATTR, "缺少认证令牌");
            } else {
                LoginSession session = tokenService.getSession(token);
                if (session == null || session.getUserId() == null) {
                    request.setAttribute(AUTH_FAILURE_MESSAGE_ATTR, "登录已过期，请重新登录");
                } else {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    session.getUserId(),
                                    token,
                                    AuthorityUtils.createAuthorityList("ROLE_API_USER")
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return request.getHeader("X-Auth-Token");
    }
}
