package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_role_menu_rel")
public class RoleMenuRelDO extends BaseCreateDO {

    private Long roleId;
    private Long menuId;

    /** 获取RoleId。 */
    public Long getRoleId() {
        return roleId;
    }

    /** 设置RoleId。 */
    public void setRoleId(Long roleIdValue) {
        this.roleId = roleIdValue;
    }

    /** 获取MenuId。 */
    public Long getMenuId() {
        return menuId;
    }

    /** 设置MenuId。 */
    public void setMenuId(Long menuIdValue) {
        this.menuId = menuIdValue;
    }
}

