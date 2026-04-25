package com.boboboom.jxc.identity.interfaces.rest.response;

/** 身份与权限结果模型，承载业务处理结果。 */
public class AuthRefreshResult {

    private String accessToken;
    private String refreshToken;

    /** 获取AccessToken。 */
    public String getAccessToken() {
        return accessToken;
    }

    /** 设置AccessToken。 */
    public void setAccessToken(String accessTokenValue) {
        this.accessToken = accessTokenValue;
    }

    /** 获取RefreshToken。 */
    public String getRefreshToken() {
        return refreshToken;
    }

    /** 设置RefreshToken。 */
    public void setRefreshToken(String refreshTokenValue) {
        this.refreshToken = refreshTokenValue;
    }
}
