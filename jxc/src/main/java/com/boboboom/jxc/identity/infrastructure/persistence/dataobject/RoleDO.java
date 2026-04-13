package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("dev.sys_role")
public class RoleDO extends BaseAuditDO {

    private Long tenantGroupId;
    private String roleCode;
    private String roleName;
    private String roleType;
    private String dataScopeType;
    private String description;
    private String status;
    private Long createdBy;

    public Long getTenantGroupId() {
        return tenantGroupId;
    }

    public void setTenantGroupId(Long tenantGroupId) {
        this.tenantGroupId = tenantGroupId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getDataScopeType() {
        return dataScopeType;
    }

    public void setDataScopeType(String dataScopeType) {
        this.dataScopeType = dataScopeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

