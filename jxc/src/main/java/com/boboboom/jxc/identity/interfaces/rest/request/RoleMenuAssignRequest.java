package com.boboboom.jxc.identity.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/** 身份与权限请求参数，承载接口入参。 */
public class RoleMenuAssignRequest {

    @NotNull
    private List<Long> menuIds;

    /** 获取MenuIds。 */
    public List<Long> getMenuIds() {
        return menuIds;
    }

    /** 设置MenuIds。 */
    public void setMenuIds(List<Long> menuIdsValue) {
        this.menuIds = menuIdsValue;
    }
}
