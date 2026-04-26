package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 身份与权限请求参数，承载接口入参。 */
public class MenuUpsertRequest {

    @NotBlank(message = "菜单编码不能为空")
    private String menuCode;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private Long parentId;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "DIRECTORY|MENU|BUTTON|API", message = "菜单类型不正确")
    private String menuType;

    private String routePath;

    private String componentKey;

    private String permissionCode;

    private String icon;

    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

    @NotNull(message = "是否可见不能为空")
    private Boolean visible;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不正确")
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

    /** 获取ComponentKey。 */
    public String getComponentKey() {
        return componentKey;
    }

    /** 设置ComponentKey。 */
    public void setComponentKey(String componentKeyValue) {
        this.componentKey = componentKeyValue;
    }

    /** 获取PermissionCode。 */
    public String getPermissionCode() {
        return permissionCode;
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
