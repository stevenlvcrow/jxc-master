package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_user")
public class UserAccountDO extends BaseAuditDO {

    private String username;
    private String realName;
    private String phone;
    private String passwordHash;
    private String passwordSalt;
    private String status;
    private String sourceType;
    private String createdScopeType;
    private Long createdScopeId;
    private Boolean firstLoginChangedPwd;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;

    /** 获取Username。 */
    public String getUsername() {
        return username;
    }

    /** 设置Username。 */
    public void setUsername(String usernameValue) {
        this.username = usernameValue;
    }

    /** 获取RealName。 */
    public String getRealName() {
        return realName;
    }

    /** 设置RealName。 */
    public void setRealName(String realNameValue) {
        this.realName = realNameValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }

    /** 获取PasswordHash。 */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** 设置PasswordHash。 */
    public void setPasswordHash(String passwordHashValue) {
        this.passwordHash = passwordHashValue;
    }

    /** 获取PasswordSalt。 */
    public String getPasswordSalt() {
        return passwordSalt;
    }

    /** 设置PasswordSalt。 */
    public void setPasswordSalt(String passwordSaltValue) {
        this.passwordSalt = passwordSaltValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取SourceType。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 设置SourceType。 */
    public void setSourceType(String sourceTypeValue) {
        this.sourceType = sourceTypeValue;
    }

    /** 获取CreatedScopeType。 */
    public String getCreatedScopeType() {
        return createdScopeType;
    }

    /** 设置CreatedScopeType。 */
    public void setCreatedScopeType(String createdScopeTypeValue) {
        this.createdScopeType = createdScopeTypeValue;
    }

    /** 获取CreatedScopeId。 */
    public Long getCreatedScopeId() {
        return createdScopeId;
    }

    /** 设置CreatedScopeId。 */
    public void setCreatedScopeId(Long createdScopeIdValue) {
        this.createdScopeId = createdScopeIdValue;
    }

    /** 获取FirstLoginChangedPwd。 */
    public Boolean getFirstLoginChangedPwd() {
        return firstLoginChangedPwd;
    }

    /** 设置FirstLoginChangedPwd。 */
    public void setFirstLoginChangedPwd(Boolean firstLoginChangedPwdValue) {
        this.firstLoginChangedPwd = firstLoginChangedPwdValue;
    }

    /** 获取LastLoginAt。 */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /** 设置LastLoginAt。 */
    public void setLastLoginAt(LocalDateTime lastLoginAtValue) {
        this.lastLoginAt = lastLoginAtValue;
    }

    /** 获取LastLoginIp。 */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /** 设置LastLoginIp。 */
    public void setLastLoginIp(String lastLoginIpValue) {
        this.lastLoginIp = lastLoginIpValue;
    }
}

