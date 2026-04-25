package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

/** 身份与权限请求参数，承载接口入参。 */
public class StoreAdminAssignRequest {

    @NotNull
    private Long adminUserId;

    /** 获取AdminUserId。 */
    public Long getAdminUserId() {
        return adminUserId;
    }

    /** 设置AdminUserId。 */
    public void setAdminUserId(Long adminUserIdValue) {
        this.adminUserId = adminUserIdValue;
    }
}
