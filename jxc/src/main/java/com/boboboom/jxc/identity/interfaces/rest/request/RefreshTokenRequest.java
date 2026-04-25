package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;

    /** 获取RefreshToken。 */
    public String getRefreshToken() {
        return refreshToken;
    }

    /** 设置RefreshToken。 */
    public void setRefreshToken(String refreshTokenValue) {
        this.refreshToken = refreshTokenValue;
    }
}
