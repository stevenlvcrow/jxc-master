package com.boboboom.jxc.identity.application.dto;

import java.time.LocalDateTime;
import java.util.List;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Boolean getFirstLoginChangedPwd() {
        return firstLoginChangedPwd;
    }

    public void setFirstLoginChangedPwd(Boolean firstLoginChangedPwd) {
        this.firstLoginChangedPwd = firstLoginChangedPwd;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ScopeOptionDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<ScopeOptionDTO> roles) {
        this.roles = roles;
    }
}
