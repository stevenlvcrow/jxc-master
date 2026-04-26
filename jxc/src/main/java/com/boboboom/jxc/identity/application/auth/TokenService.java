package com.boboboom.jxc.identity.application.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.infrastructure.support.RedisOperator;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class TokenService {

    private static final String ACCESS_TOKEN_PREFIX = "jxc:login:access:";
    private static final String REFRESH_TOKEN_PREFIX = "jxc:login:refresh:";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(12);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final RedisOperator redisOperator;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public TokenService(RedisOperator redisOperatorValue) {
        this.redisOperator = redisOperatorValue;
    }

    /** 创建登录会话。 */
    public LoginSession createSession(Long userId, String phone, String realName) {
        String accessToken = nextToken();
        String refreshToken = nextToken();
        LoginSession session = new LoginSession();
        session.setToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setUserId(userId);
        session.setPhone(phone);
        session.setRealName(realName);
        session.setLoginAt(LocalDateTime.now());
        redisOperator.set(accessTokenKey(accessToken), session, ACCESS_TOKEN_TTL);
        redisOperator.set(refreshTokenKey(refreshToken), session, REFRESH_TOKEN_TTL);
        return session;
    }

    /** 获取Session。 */
    public LoginSession getSession(String accessToken) {
        return readSession(accessTokenKey(accessToken));
    }

    /** 获取SessionByRefreshToken。 */
    public LoginSession getSessionByRefreshToken(String refreshToken) {
        LoginSession session = readSession(refreshTokenKey(refreshToken));
        if (session != null) {
            return session;
        }
        LoginSession legacySession = readSession(accessTokenKey(refreshToken));
        if (legacySession == null) {
            return null;
        }
        String storedRefreshToken = legacySession.getRefreshToken();
        if (storedRefreshToken == null || storedRefreshToken.isBlank() || refreshToken.equals(legacySession.getToken())) {
            return legacySession;
        }
        return null;
    }

    /** 移除访问令牌会话。 */
    public void removeSession(String accessToken) {
        LoginSession session = getSession(accessToken);
        if (session != null) {
            removeTokens(session.getToken(), session.getRefreshToken());
            return;
        }
        deleteQuietly(accessTokenKey(accessToken));
    }

    /** 移除刷新令牌会话。 */
    public void removeRefreshSession(String refreshToken) {
        LoginSession session = getSessionByRefreshToken(refreshToken);
        if (session != null) {
            removeTokens(session.getToken(), session.getRefreshToken());
            return;
        }
        deleteQuietly(refreshTokenKey(refreshToken));
    }

    private LoginSession readSession(String key) {
        Object value = redisOperator.get(key);
        if (value instanceof LoginSession session) {
            return session;
        }
        return null;
    }

    private void removeTokens(String accessToken, String refreshToken) {
        deleteQuietly(accessTokenKey(accessToken));
        if (refreshToken != null && !refreshToken.isBlank()) {
            deleteQuietly(refreshTokenKey(refreshToken));
        }
    }

    private void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        redisOperator.delete(key);
    }

    private String accessTokenKey(String token) {
        return ACCESS_TOKEN_PREFIX + token;
    }

    private String refreshTokenKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }

    private String nextToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
