package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("dev.sys_login_log")
public class LoginLogDO extends BaseIdDO {

    private Long userId;
    private String phone;
    private String loginResult;
    private String failureReason;
    private String clientIp;
    private String userAgent;
    private String selectedScope;
    private Long selectedScopeId;
    private LocalDateTime loginAt;

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

    public String getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(String loginResult) {
        this.loginResult = loginResult;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSelectedScope() {
        return selectedScope;
    }

    public void setSelectedScope(String selectedScope) {
        this.selectedScope = selectedScope;
    }

    public Long getSelectedScopeId() {
        return selectedScopeId;
    }

    public void setSelectedScopeId(Long selectedScopeId) {
        this.selectedScopeId = selectedScopeId;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }
}

