package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

public class StoreAdminAssignRequest {

    @NotNull
    private Long adminUserId;

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }
}
