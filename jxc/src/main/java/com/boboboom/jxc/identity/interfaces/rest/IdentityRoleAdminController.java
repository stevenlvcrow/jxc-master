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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.RoleAdministrationService;
import com.boboboom.jxc.identity.application.service.RoleMenuAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleMenuAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;

import jakarta.validation.Valid;

/**
 * 角色与菜单管理接口，负责角色维护、菜单授权以及角色可见性查询。
 */
@Validated
@RestController
@RequestMapping("/api/identity/admin")
public class IdentityRoleAdminController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final RoleAdministrationService roleAdministrationService;
    private final RoleMenuAdministrationService roleMenuAdministrationService;
    private final IdentityAccessControlService identityAccessControlService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造角色与菜单管理接口。
     *
     * @param roleAdministrationServiceValue 角色管理服务
     * @param roleMenuAdministrationServiceValue 角色菜单管理服务
     * @param identityAccessControlServiceValue 组织权限控制服务
     * @param identityAdminSupportValue 当前登录管理员辅助服务
     */
    public IdentityRoleAdminController(RoleAdministrationService roleAdministrationServiceValue,
                                       RoleMenuAdministrationService roleMenuAdministrationServiceValue,
                                       IdentityAccessControlService identityAccessControlServiceValue,
                                       IdentityAdminSupport identityAdminSupportValue) {
        this.roleAdministrationService = roleAdministrationServiceValue;
        this.roleMenuAdministrationService = roleMenuAdministrationServiceValue;
        this.identityAccessControlService = identityAccessControlServiceValue;
        this.identityAdminSupport = identityAdminSupportValue;
    }

    /**
     * 查询角色列表。
     *
     * @param orgId 机构标识
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 角色列表响应
     */
    @GetMapping("/roles")
    public CodeDataResponse<PageData<RoleAdminView>> listRoles(@RequestParam(required = false) String orgId,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        List<RoleAdminView> result = roleAdministrationService.listRoles(operatorId, platformAdmin, orgId).stream()
                .map(role -> new RoleAdminView(
                        role.id(),
                        role.roleCode(),
                        role.tenantGroupId(),
                        role.tenantGroupName(),
                        role.roleName(),
                        role.roleType(),
                        role.dataScopeType(),
                        role.description(),
                        role.status(),
                        role.menuIds(),
                        role.builtin(),
                        role.editable()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 新建角色。
     *
     * @param request 角色新增请求
     * @param orgId 机构标识
     * @return 新建结果
     */
    @PostMapping("/roles")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createRole(@Valid @RequestBody RoleUpsertRequest request,
                                                  @RequestParam(required = false) String orgId) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.createRole(
                request,
                operatorId,
                identityAdminSupport.isPlatformAdmin(operatorId),
                orgId
        );
        return CodeDataResponse.ok(new IdPayload(role.getId()));
    }

    /**
     * 更新角色信息。
     *
     * @param id 角色主键
     * @param orgId 机构标识
     * @param request 角色更新请求
     * @return 空响应
     */
    @PutMapping("/roles/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateRole(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody RoleUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        roleAdministrationService.updateRole(role, request, operatorId, identityAdminSupport.isPlatformAdmin(operatorId), orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 更新角色状态。
     *
     * @param id 角色主键
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/roles/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateRoleStatus(@PathVariable Long id,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        roleAdministrationService.updateRoleStatus(role, request.getStatus(), operatorId);
        return CodeDataResponse.ok();
    }

    /**
     * 删除角色。
     *
     * @param id 角色主键
     * @param orgId 机构标识
     * @return 空响应
     */
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteRole(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        RoleDO role = roleAdministrationService.requireRole(id);
        roleAdministrationService.deleteRole(role, operatorId, platformAdmin, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 为角色分配菜单。
     *
     * @param id 角色主键
     * @param request 菜单分配请求
     * @return 空响应
     */
    @PutMapping("/roles/{id}/menus")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> assignRoleMenus(@PathVariable Long id,
                                                  @Valid @RequestBody RoleMenuAssignRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        identityAccessControlService.ensureRoleMenuAssignable(operatorId, role);
        roleMenuAdministrationService.saveRoleMenus(role, request.getMenuIds());
        return CodeDataResponse.ok();
    }

    /**
     * 查询当前可分配的菜单列表。
     *
     * @param orgId 机构标识
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 菜单列表响应
     */
    @GetMapping("/menus")
    public CodeDataResponse<PageData<MenuAdminView>> listMenus(@RequestParam(required = false) String orgId,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        roleMenuAdministrationService.ensureGroupMgmtMenusForGroupAdmin();
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        List<MenuAdminView> result = roleMenuAdministrationService.listAssignableMenus(
                        operatorId,
                        platformAdmin,
                        orgId
                ).stream()
                .map(menu -> new MenuAdminView(
                        menu.id(),
                        menu.menuCode(),
                        menu.menuName(),
                        menu.parentId(),
                        menu.menuType(),
                        menu.routePath(),
                        menu.componentKey(),
                        menu.permissionCode(),
                        menu.status(),
                        menu.sortNo()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 查询角色已绑定的菜单主键列表。
     *
     * @param id 角色主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 菜单主键列表响应
     */
    @GetMapping("/roles/{id}/menu-ids")
    public CodeDataResponse<PageData<Long>> listRoleMenuIds(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        return CodeDataResponse.ok(paginate(roleMenuAdministrationService.listRoleMenuIds(id), pageNum, pageSize));
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }
}
