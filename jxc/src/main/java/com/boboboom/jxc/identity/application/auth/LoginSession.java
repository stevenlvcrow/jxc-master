package com.boboboom.jxc.identity.application.auth;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 身份与权限类型，负责Login会话相关处理。 */
public class LoginSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String refreshToken;
    private Long userId;
    private String phone;
    private String realName;
    private LocalDateTime loginAt;

    /** 获取Token。 */
    public String getToken() {
        return token;
    }

    /** 设置Token。 */
    public void setToken(String tokenValue) {
        this.token = tokenValue;
    }

    /** 获取RefreshToken。 */
    public String getRefreshToken() {
        return refreshToken;
    }

    /** 设置RefreshToken。 */
    public void setRefreshToken(String refreshTokenValue) {
        this.refreshToken = refreshTokenValue;
    }

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }

    /** 获取RealName。 */
    public String getRealName() {
        return realName;
    }

    /** 设置RealName。 */
    public void setRealName(String realNameValue) {
        this.realName = realNameValue;
    }

    /** 获取LoginAt。 */
    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    /** 设置LoginAt。 */
    public void setLoginAt(LocalDateTime loginAtValue) {
        this.loginAt = loginAtValue;
    }
}
