package com.boboboom.jxc.identity.application.dto;

/** 身份与权限类型，负责作用域选项传输对象相关处理。 */
public class ScopeOptionDTO {

    private String scopeType;
    private Long scopeId;
    private String scopeName;
    private String roleCode;
    private String roleName;

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
}
