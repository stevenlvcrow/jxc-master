package com.boboboom.jxc.identity.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class RoleUpsertRequest {

    private String roleCode;

    @NotBlank
    private String roleName;

    private Boolean builtin;

    @NotBlank
    private String roleType;

    @NotBlank
    private String dataScopeType;

    private String description;

    private String status;

    private List<Long> menuIds;

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

    /** 获取MenuIds。 */
    public List<Long> getMenuIds() {
        return menuIds;
    }

    /** 设置MenuIds。 */
    public void setMenuIds(List<Long> menuIdsValue) {
        this.menuIds = menuIdsValue;
    }
}
