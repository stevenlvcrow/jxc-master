package com.boboboom.jxc.identity.application.dto;

import java.util.List;

/** 身份与权限类型，负责当前用户传输对象相关处理。 */
public class CurrentUserDTO {

    private Long userId;
    private String phone;
    private String realName;
    private List<ScopeOptionDTO> scopes;

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

    /** 获取Scopes。 */
    public List<ScopeOptionDTO> getScopes() {
        return scopes;
    }

    /** 设置Scopes。 */
    public void setScopes(List<ScopeOptionDTO> scopesValue) {
        this.scopes = scopesValue;
    }
}
