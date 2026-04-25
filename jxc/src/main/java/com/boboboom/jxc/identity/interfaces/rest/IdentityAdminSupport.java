package com.boboboom.jxc.identity.interfaces.rest;

import org.springframework.stereotype.Component;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;

/** 身份管理接口支撑组件，封装当前操作人和平台管理员校验。 */
@Component
public class IdentityAdminSupport {

    private final OrgScopeService orgScopeService;

    /** 创建身份管理接口支撑组件。 */
    public IdentityAdminSupport(OrgScopeService orgScopeServiceValue) {
        this.orgScopeService = orgScopeServiceValue;
    }

    /** 获取当前操作人标识。 */
    public Long currentOperatorId() {
        return AuthContextHolder.requireUserId("登录已失效，请重新登录");
    }

    /** 获取当前操作人账号。 */
    public String currentOperatorUsername() {
        return AuthContextHolder.userNameOr("system");
    }

    /** 判断指定用户是否为平台管理员。 */
    public boolean isPlatformAdmin(Long userId) {
        return orgScopeService.isPlatformAdmin(userId);
    }

    /** 校验当前操作人必须是平台管理员。 */
    public void requirePlatformAdmin() {
        if (!isPlatformAdmin(currentOperatorId())) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
    }
}
