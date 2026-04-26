package com.boboboom.jxc.identity.application.dto;

/** 身份与权限类型，负责菜单传输对象相关处理。 */
public class MenuDTO {

    private Long id;
    private String menuCode;
    private String menuName;
    private Long parentId;
    private String menuType;
    private String routePath;
    private String permissionCode;
    private String icon;
    private Integer sortNo;
    private Boolean visible;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

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
}
