package com.boboboom.jxc.identity.application.auth;

import org.springframework.stereotype.Component;

@Component("requestPermissionGuard")
public class RequestPermissionGuard {

    public boolean authenticated() {
        LoginSession session = AuthContextHolder.get();
        return session != null && session.getUserId() != null;
    }
}
