package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.MenuAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.interfaces.rest.request.MenuSortRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.MenuUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;

import jakarta.validation.Valid;

/** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
@Validated
@RestController
@RequestMapping("/api/identity/admin/menu-maintenance")
public class IdentityMenuAdminController {

    private final MenuAdministrationService menuAdministrationService;
    private final IdentityAdminSupport identityAdminSupport;

    /** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public IdentityMenuAdminController(MenuAdministrationService menuAdministrationServiceValue,
                                       IdentityAdminSupport identityAdminSupportValue) {
        this.menuAdministrationService = menuAdministrationServiceValue;
        this.identityAdminSupport = identityAdminSupportValue;
    }

    /** 查询菜单维护列表。 */
    @GetMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<List<MenuMaintenanceView>> listMenus() {
        identityAdminSupport.requirePlatformAdmin();
        return CodeDataResponse.ok(menuAdministrationService.listMenus().stream()
                .map(this::toView)
                .toList());
    }

    /** 创建业务记录。 */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@Valid @RequestBody MenuUpsertRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        MenuDO menu = menuAdministrationService.create(request);
        return CodeDataResponse.ok(new IdPayload(menu.getId()));
    }

    /** 更新业务记录。 */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @Valid @RequestBody MenuUpsertRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        menuAdministrationService.update(id, request);
        return CodeDataResponse.ok();
    }

    /** 删除业务记录。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> delete(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        menuAdministrationService.delete(id);
        return CodeDataResponse.ok();
    }

    /** 保存菜单排序结果。 */
    @PutMapping("/sort")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> sort(@Valid @RequestBody MenuSortRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        menuAdministrationService.sort(request);
        return CodeDataResponse.ok();
    }

    private MenuMaintenanceView toView(MenuDO menu) {
        return new MenuMaintenanceView(
                menu.getId(),
                menu.getMenuCode(),
                menu.getMenuName(),
                menu.getParentId(),
                menu.getMenuType(),
                menu.getRoutePath(),
                menu.getComponentKey(),
                menu.getPermissionCode(),
                menu.getIcon(),
                menu.getSortNo(),
                menu.getVisible(),
                menu.getStatus()
        );
    }
}
