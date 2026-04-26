package com.boboboom.jxc.identity.application.dto;

import java.util.List;

/** 身份与权限响应模型，承载接口返回数据。 */
public class LoginResponse {

    private String token;
    private Long userId;
    private String phone;
    private String realName;
    private Boolean firstLoginChangedPwd;
    private List<ScopeOptionDTO> scopes;

    /** 获取Token。 */
    public String getToken() {
        return token;
    }

    /** 设置Token。 */
    public void setToken(String tokenValue) {
        this.token = tokenValue;
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

    /** 获取FirstLoginChangedPwd。 */
    public Boolean getFirstLoginChangedPwd() {
        return firstLoginChangedPwd;
    }

    /** 设置FirstLoginChangedPwd。 */
    public void setFirstLoginChangedPwd(Boolean firstLoginChangedPwdValue) {
        this.firstLoginChangedPwd = firstLoginChangedPwdValue;
    }

    /** 获取Scopes。 */
    public List<ScopeOptionDTO> getScopes() {
        return scopes;
    }

    /** 设置Scopes。 */
    public void setScopes(List<ScopeOptionDTO> scopesValue) {
        this.scopes = scopesValue;
    }
}
