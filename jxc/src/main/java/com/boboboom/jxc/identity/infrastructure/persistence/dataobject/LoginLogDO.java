package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_login_log")
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

    /** 获取LoginResult。 */
    public String getLoginResult() {
        return loginResult;
    }

    /** 设置LoginResult。 */
    public void setLoginResult(String loginResultValue) {
        this.loginResult = loginResultValue;
    }

    /** 获取FailureReason。 */
    public String getFailureReason() {
        return failureReason;
    }

    /** 设置FailureReason。 */
    public void setFailureReason(String failureReasonValue) {
        this.failureReason = failureReasonValue;
    }

    /** 获取ClientIp。 */
    public String getClientIp() {
        return clientIp;
    }

    /** 设置ClientIp。 */
    public void setClientIp(String clientIpValue) {
        this.clientIp = clientIpValue;
    }

    /** 获取UserAgent。 */
    public String getUserAgent() {
        return userAgent;
    }

    /** 设置UserAgent。 */
    public void setUserAgent(String userAgentValue) {
        this.userAgent = userAgentValue;
    }

    /** 获取SelectedScope。 */
    public String getSelectedScope() {
        return selectedScope;
    }

    /** 设置SelectedScope。 */
    public void setSelectedScope(String selectedScopeValue) {
        this.selectedScope = selectedScopeValue;
    }

    /** 获取SelectedScopeId。 */
    public Long getSelectedScopeId() {
        return selectedScopeId;
    }

    /** 设置SelectedScopeId。 */
    public void setSelectedScopeId(Long selectedScopeIdValue) {
        this.selectedScopeId = selectedScopeIdValue;
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

