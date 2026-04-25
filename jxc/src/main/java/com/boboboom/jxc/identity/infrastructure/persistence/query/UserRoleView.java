package com.boboboom.jxc.identity.infrastructure.persistence.query;

import java.time.LocalDateTime;

/** 身份与权限视图模型，承载页面展示数据。 */
public class UserRoleView {

    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String userStatus;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String roleType;
    private String dataScopeType;
    private String scopeType;
    private Long scopeId;
    private String scopeName;
    private LocalDateTime assignedAt;

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
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

    /** 获取UserStatus。 */
    public String getUserStatus() {
        return userStatus;
    }

    /** 设置UserStatus。 */
    public void setUserStatus(String userStatusValue) {
        this.userStatus = userStatusValue;
    }

    /** 获取RoleId。 */
    public Long getRoleId() {
        return roleId;
    }

    /** 设置RoleId。 */
    public void setRoleId(Long roleIdValue) {
        this.roleId = roleIdValue;
    }

    /** 获取RoleCode。 */
    public String getRoleCode() {
        return roleCode;
    }

    /** 设置RoleCode。 */
    public void setRoleCode(String roleCodeValue) {
        this.roleCode = roleCodeValue;
    }

    /** 获取RoleName。 */
    public String getRoleName() {
        return roleName;
    }

    /** 设置RoleName。 */
    public void setRoleName(String roleNameValue) {
        this.roleName = roleNameValue;
    }

    /** 获取RoleType。 */
    public String getRoleType() {
        return roleType;
    }

    /** 设置RoleType。 */
    public void setRoleType(String roleTypeValue) {
        this.roleType = roleTypeValue;
    }

    /** 获取DataScopeType。 */
    public String getDataScopeType() {
        return dataScopeType;
    }

    /** 设置DataScopeType。 */
    public void setDataScopeType(String dataScopeTypeValue) {
        this.dataScopeType = dataScopeTypeValue;
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

    /** 获取ScopeName。 */
    public String getScopeName() {
        return scopeName;
    }

    /** 设置ScopeName。 */
    public void setScopeName(String scopeNameValue) {
        this.scopeName = scopeNameValue;
    }

    /** 获取AssignedAt。 */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    /** 设置AssignedAt。 */
    public void setAssignedAt(LocalDateTime assignedAtValue) {
        this.assignedAt = assignedAtValue;
    }
}

