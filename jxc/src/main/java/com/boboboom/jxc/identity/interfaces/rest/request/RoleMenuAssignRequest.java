package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RoleMenuAssignRequest {

    @NotNull
    private List<Long> menuIds;

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
