package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class CurrentUserAccountChangeRequest {

    @NotBlank
    private String account;

    /** 获取Account。 */
    public String getAccount() {
        return account;
    }

    /** 设置Account。 */
    public void setAccount(String accountValue) {
        this.account = accountValue;
    }
}
