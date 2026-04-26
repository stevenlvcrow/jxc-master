package com.boboboom.jxc.identity.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 身份与权限类型，负责角色传输对象相关处理。 */
public class RoleDTO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String roleType;
    private String dataScopeType;
    private String description;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> menuIds;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
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

    /** 获取MenuIds。 */
    public List<Long> getMenuIds() {
        return menuIds;
    }

    /** 设置MenuIds。 */
    public void setMenuIds(List<Long> menuIdsValue) {
        this.menuIds = menuIdsValue;
    }
}
