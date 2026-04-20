package com.boboboom.jxc.identity.interfaces.rest.response;

public class AuthLoginResult {

    private String accessToken;
    private String refreshToken;
    private String userName;
    private String account;
    private String phone;
    private Boolean platformAdmin;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getPlatformAdmin() {
        return platformAdmin;
    }

    public void setPlatformAdmin(Boolean platformAdmin) {
        this.platformAdmin = platformAdmin;
    }
}
