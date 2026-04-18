package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.RoleAdministrationService;
import com.boboboom.jxc.identity.application.service.RoleMenuAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleMenuAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin")
public class IdentityRoleAdminController {

    private final RoleAdministrationService roleAdministrationService;
    private final RoleMenuAdministrationService roleMenuAdministrationService;
    private final IdentityAccessControlService identityAccessControlService;
    private final IdentityAdminSupport identityAdminSupport;

    public IdentityRoleAdminController(RoleAdministrationService roleAdministrationService,
                                       RoleMenuAdministrationService roleMenuAdministrationService,
                                       IdentityAccessControlService identityAccessControlService,
                                       IdentityAdminSupport identityAdminSupport) {
        this.roleAdministrationService = roleAdministrationService;
        this.roleMenuAdministrationService = roleMenuAdministrationService;
        this.identityAccessControlService = identityAccessControlService;
        this.identityAdminSupport = identityAdminSupport;
    }

    @GetMapping("/roles")
    public CodeDataResponse<List<RoleAdminView>> listRoles(@RequestParam(required = false) String orgId) {
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
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/roles")
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

    @PutMapping("/roles/{id}")
    public CodeDataResponse<Void> updateRole(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody RoleUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        roleAdministrationService.updateRole(role, request, operatorId, identityAdminSupport.isPlatformAdmin(operatorId), orgId);
        return CodeDataResponse.ok();
    }

    @PutMapping("/roles/{id}/status")
    public CodeDataResponse<Void> updateRoleStatus(@PathVariable Long id,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        roleAdministrationService.updateRoleStatus(role, request.getStatus(), operatorId);
        return CodeDataResponse.ok();
    }

    @PutMapping("/roles/{id}/menus")
    public CodeDataResponse<Void> assignRoleMenus(@PathVariable Long id,
                                                  @Valid @RequestBody RoleMenuAssignRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        identityAccessControlService.ensureRoleMenuAssignable(operatorId, role);
        roleMenuAdministrationService.saveRoleMenus(role, request.getMenuIds());
        return CodeDataResponse.ok();
    }

    @GetMapping("/menus")
    public CodeDataResponse<List<MenuAdminView>> listMenus(@RequestParam(required = false) String orgId) {
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
                        menu.permissionCode(),
                        menu.status(),
                        menu.sortNo()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/roles/{id}/menu-ids")
    public CodeDataResponse<List<Long>> listRoleMenuIds(@PathVariable Long id) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        RoleDO role = roleAdministrationService.requireRole(id);
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        return CodeDataResponse.ok(roleMenuAdministrationService.listRoleMenuIds(id));
    }
}
