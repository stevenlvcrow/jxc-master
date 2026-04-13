package com.boboboom.jxc.identity.application.auth;

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

    public static void clear() {
        HOLDER.remove();
    }
}
