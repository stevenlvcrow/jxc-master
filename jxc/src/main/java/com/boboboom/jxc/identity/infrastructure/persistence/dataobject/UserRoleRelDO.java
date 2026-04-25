package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_user_role_rel")
public class UserRoleRelDO extends BaseIdDO {

    private Long userId;
    private Long roleId;
    private String scopeType;
    private Long scopeId;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private String status;

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取RoleId。 */
    public Long getRoleId() {
        return roleId;
    }

    /** 设置RoleId。 */
    public void setRoleId(Long roleIdValue) {
        this.roleId = roleIdValue;
    }

    /** 获取ScopeType。 */
    public String getScopeType() {
        return scopeType;
    }

    /** 设置ScopeType。 */
    public void setScopeType(String scopeTypeValue) {
        this.scopeType = scopeTypeValue;
    }

    /** 获取ScopeId。 */
    public Long getScopeId() {
        return scopeId;
    }

    /** 设置ScopeId。 */
    public void setScopeId(Long scopeIdValue) {
        this.scopeId = scopeIdValue;
    }

    /** 获取AssignedBy。 */
    public Long getAssignedBy() {
        return assignedBy;
    }

    /** 设置AssignedBy。 */
    public void setAssignedBy(Long assignedByValue) {
        this.assignedBy = assignedByValue;
    }

    /** 获取AssignedAt。 */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    /** 设置AssignedAt。 */
    public void setAssignedAt(LocalDateTime assignedAtValue) {
        this.assignedAt = assignedAtValue;
    }

    /** 获取ExpiresAt。 */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /** 设置ExpiresAt。 */
    public void setExpiresAt(LocalDateTime expiresAtValue) {
        this.expiresAt = expiresAtValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }
}

