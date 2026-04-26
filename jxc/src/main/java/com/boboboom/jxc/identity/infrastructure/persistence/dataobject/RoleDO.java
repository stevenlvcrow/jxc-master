package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_role")
public class RoleDO extends BaseAuditDO {

    private Long tenantGroupId;
    private String roleCode;
    private String roleName;
    private Boolean builtin;
    private String roleType;
    private String dataScopeType;
    private String description;
    private String status;
    private Long createdBy;

    /** 获取TenantGroupId。 */
    public Long getTenantGroupId() {
        return tenantGroupId;
    }

    /** 设置TenantGroupId。 */
    public void setTenantGroupId(Long tenantGroupIdValue) {
        this.tenantGroupId = tenantGroupIdValue;
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

    /** 获取Builtin。 */
    public Boolean getBuiltin() {
        return builtin;
    }

    /** 设置Builtin。 */
    public void setBuiltin(Boolean builtinValue) {
        this.builtin = builtinValue;
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

    /** 获取Description。 */
    public String getDescription() {
        return description;
    }

    /** 设置Description。 */
    public void setDescription(String descriptionValue) {
        this.description = descriptionValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取CreatedBy。 */
    public Long getCreatedBy() {
        return createdBy;
    }

    /** 设置CreatedBy。 */
    public void setCreatedBy(Long createdByValue) {
        this.createdBy = createdByValue;
    }
}

