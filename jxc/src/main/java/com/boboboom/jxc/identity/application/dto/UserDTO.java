package com.boboboom.jxc.identity.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 身份与权限类型，负责用户传输对象相关处理。 */
public class UserDTO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String status;
    private String sourceType;
    private Boolean firstLoginChangedPwd;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScopeOptionDTO> roles;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

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

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置UpdatedAt。 */
    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }

    /** 获取Roles。 */
    public List<ScopeOptionDTO> getRoles() {
        return roles;
    }

    /** 设置Roles。 */
    public void setRoles(List<ScopeOptionDTO> rolesValue) {
        this.roles = rolesValue;
    }
}
