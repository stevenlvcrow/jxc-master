package com.boboboom.jxc.config;

import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_MESSAGE_ATTR = TokenAuthenticationFilter.class.getName() + ".AUTH_FAILURE_MESSAGE";

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
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
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getHeader("X-Auth-Token");
    }
}
