package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_menu")
public class MenuDO extends BaseAuditDO {

    private String menuCode;
    private String menuName;
    private Long parentId;
    private String menuType;
    private String routePath;
    private String componentKey;
    private String permissionCode;
    private String icon;
    private Integer sortNo;
    private Boolean visible;
    private String status;

    /** 获取MenuCode。 */
    public String getMenuCode() {
        return menuCode;
    }

    /** 设置MenuCode。 */
    public void setMenuCode(String menuCodeValue) {
        this.menuCode = menuCodeValue;
    }

    /** 获取MenuName。 */
    public String getMenuName() {
        return menuName;
    }

    /** 设置MenuName。 */
    public void setMenuName(String menuNameValue) {
        this.menuName = menuNameValue;
    }

    /** 获取ParentId。 */
    public Long getParentId() {
        return parentId;
    }

    /** 设置ParentId。 */
    public void setParentId(Long parentIdValue) {
        this.parentId = parentIdValue;
    }

    /** 获取MenuType。 */
    public String getMenuType() {
        return menuType;
    }

    /** 设置MenuType。 */
    public void setMenuType(String menuTypeValue) {
        this.menuType = menuTypeValue;
    }

    /** 获取RoutePath。 */
    public String getRoutePath() {
        return routePath;
    }

    /** 设置RoutePath。 */
    public void setRoutePath(String routePathValue) {
        this.routePath = routePathValue;
    }

    /** 获取PermissionCode。 */
    public String getPermissionCode() {
        return permissionCode;
    }

    /** 获取ComponentKey。 */
    public String getComponentKey() {
        return componentKey;
    }

    /** 设置ComponentKey。 */
    public void setComponentKey(String componentKeyValue) {
        this.componentKey = componentKeyValue;
    }

    /** 设置PermissionCode。 */
    public void setPermissionCode(String permissionCodeValue) {
        this.permissionCode = permissionCodeValue;
    }

    /** 获取Icon。 */
    public String getIcon() {
        return icon;
    }

    /** 设置Icon。 */
    public void setIcon(String iconValue) {
        this.icon = iconValue;
    }

    /** 获取SortNo。 */
    public Integer getSortNo() {
        return sortNo;
    }

    /** 设置SortNo。 */
    public void setSortNo(Integer sortNoValue) {
        this.sortNo = sortNoValue;
    }

    /** 获取Visible。 */
    public Boolean getVisible() {
        return visible;
    }

    /** 设置Visible。 */
    public void setVisible(Boolean visibleValue) {
        this.visible = visibleValue;
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

