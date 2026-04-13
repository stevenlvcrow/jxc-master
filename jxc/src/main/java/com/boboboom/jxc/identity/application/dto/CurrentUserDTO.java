package com.boboboom.jxc.identity.application.dto;

import java.util.List;

public class CurrentUserDTO {

    private Long userId;
    private String phone;
    private String realName;
    private List<ScopeOptionDTO> scopes;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public List<ScopeOptionDTO> getScopes() {
        return scopes;
    }

    public void setScopes(List<ScopeOptionDTO> scopes) {
        this.scopes = scopes;
    }
}
