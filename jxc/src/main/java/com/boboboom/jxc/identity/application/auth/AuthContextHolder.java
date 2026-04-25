package com.boboboom.jxc.identity.application.auth;

import com.boboboom.jxc.common.BusinessException;

/** 身份与权限类型，负责AuthContextHolder相关处理。 */
public final class AuthContextHolder {

    private static final ThreadLocal<LoginSession> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    /** 写入 Redis 缓存值。 */
    public static void set(LoginSession session) {
        HOLDER.set(session);
    }

    /** 读取 Redis 缓存值。 */
    public static LoginSession get() {
        return HOLDER.get();
    }

    /** 处理require。 */
    public static LoginSession require() {
        LoginSession session = HOLDER.get();
        if (session == null) {
            throw new UnauthorizedException("未登录或登录已失效");
        }
        return session;
    }

    /** 查询并校验用户标识存在。 */
    public static Long requireUserId(String message) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getUserId() == null) {
            throw new BusinessException(message);
        }
        return session.getUserId();
    }

    /** 处理用户标识Or。 */
    public static Long userIdOr(Long fallback) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getUserId() == null) {
            return fallback;
        }
        return session.getUserId();
    }

    /** 处理用户名称Or。 */
    public static String userNameOr(String fallback) {
        LoginSession session = HOLDER.get();
        if (session == null || session.getRealName() == null || session.getRealName().isBlank()) {
            return fallback;
        }
        return session.getRealName();
    }

    /** 处理clear。 */
    public static void clear() {
        HOLDER.remove();
    }
}
