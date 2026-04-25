package com.boboboom.jxc.identity.application.auth;

import org.springframework.stereotype.Component;

/** 身份与权限守卫组件，负责请求权限Guard校验。 */
@Component("requestPermissionGuard")
public class RequestPermissionGuard {

    /** 校验当前请求是否已认证。 */
    public boolean authenticated() {
        LoginSession session = AuthContextHolder.get();
        return session != null && session.getUserId() != null;
    }
}
