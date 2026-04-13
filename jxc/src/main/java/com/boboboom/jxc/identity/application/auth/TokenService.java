package com.boboboom.jxc.identity.application.auth;

import com.boboboom.jxc.infrastructure.support.RedisOperator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "jxc:login:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final RedisOperator redisOperator;

    public TokenService(RedisOperator redisOperator) {
        this.redisOperator = redisOperator;
    }

    public LoginSession createSession(Long userId, String phone, String realName) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LoginSession session = new LoginSession();
        session.setToken(token);
        session.setUserId(userId);
        session.setPhone(phone);
        session.setRealName(realName);
        session.setLoginAt(LocalDateTime.now());
        redisOperator.set(tokenKey(token), session, TOKEN_TTL);
        return session;
    }

    public LoginSession getSession(String token) {
        Object value = redisOperator.get(tokenKey(token));
        if (value instanceof LoginSession session) {
            return session;
        }
        return null;
    }

    public void removeSession(String token) {
        redisOperator.delete(tokenKey(token));
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }
}
