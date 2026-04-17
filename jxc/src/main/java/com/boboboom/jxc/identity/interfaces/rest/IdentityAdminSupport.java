package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import org.springframework.stereotype.Component;

@Component
public class IdentityAdminSupport {

    private final OrgScopeService orgScopeService;

    public IdentityAdminSupport(OrgScopeService orgScopeService) {
        this.orgScopeService = orgScopeService;
    }

    public Long currentOperatorId() {
        return AuthContextHolder.requireUserId("登录已失效，请重新登录");
    }

    public String currentOperatorUsername() {
        return AuthContextHolder.userNameOr("system");
    }

    public boolean isPlatformAdmin(Long userId) {
        return orgScopeService.isPlatformAdmin(userId);
    }

    public void requirePlatformAdmin() {
        if (!isPlatformAdmin(currentOperatorId())) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
    }
}
