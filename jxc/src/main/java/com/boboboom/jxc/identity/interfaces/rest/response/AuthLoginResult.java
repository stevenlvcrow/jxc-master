package com.boboboom.jxc.identity.interfaces.rest.response;

/** 身份与权限结果模型，承载业务处理结果。 */
public class AuthLoginResult {

    private String accessToken;
    private String refreshToken;
    private String userName;
    private String account;
    private String phone;
    private Boolean platformAdmin;

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

    /** 获取UserName。 */
    public String getUserName() {
        return userName;
    }

    /** 设置UserName。 */
    public void setUserName(String userNameValue) {
        this.userName = userNameValue;
    }

    /** 获取Account。 */
    public String getAccount() {
        return account;
    }

    /** 设置Account。 */
    public void setAccount(String accountValue) {
        this.account = accountValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }

    /** 获取PlatformAdmin。 */
    public Boolean getPlatformAdmin() {
        return platformAdmin;
    }

    /** 设置PlatformAdmin。 */
    public void setPlatformAdmin(Boolean platformAdminValue) {
        this.platformAdmin = platformAdminValue;
    }
}
