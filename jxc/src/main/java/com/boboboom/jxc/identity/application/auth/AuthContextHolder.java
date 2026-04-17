package com.boboboom.jxc.identity.application.auth;

import com.boboboom.jxc.common.BusinessException;

public final class AuthContextHolder {

    private static final ThreadLocal<LoginSession> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(LoginSession session) {
        HOLDER.set(session);
    }

    public static LoginSession get() {
        return HOLDER.get();
    }

    public static LoginSession require() {
        LoginSession session = HOLDER.get();
        if (session == null) {
            throw new UnauthorizedException("未登录或登录已失效");
        }
        return session;
    }

    public static Long requireUserId(String message) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getUserId() == null) {
            throw new BusinessException(message);
        }
        return session.getUserId();
    }

    public static Long userIdOr(Long fallback) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getUserId() == null) {
            return fallback;
        }
        return session.getUserId();
    }

    public static String userNameOr(String fallback) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getRealName() == null || session.getRealName().isBlank()) {
            return fallback;
        }
        return session.getRealName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
