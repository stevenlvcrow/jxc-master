package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.UserAdministrationService;
import com.boboboom.jxc.identity.application.service.UserRoleAssignmentService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/identity/admin/users")
public class IdentityUserAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final UserAdministrationService userAdministrationService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    public IdentityUserAdminController(IdentityAccessControlService identityAccessControlService,
                                       UserAdministrationService userAdministrationService,
                                       UserRoleAssignmentService userRoleAssignmentService,
                                       IdentityAdminLookupService identityAdminLookupService,
                                       IdentityAdminSupport identityAdminSupport) {
        this.identityAccessControlService = identityAccessControlService;
        this.userAdministrationService = userAdministrationService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.identityAdminLookupService = identityAdminLookupService;
        this.identityAdminSupport = identityAdminSupport;
    }

    @GetMapping
    public CodeDataResponse<List<UserAdminView>> listUsers() {
        Long operatorId = identityAdminSupport.currentOperatorId();
        List<UserAdminView> result = userAdministrationService
                .listUsers(operatorId, identityAdminSupport.isPlatformAdmin(operatorId))
                .stream()
                .map(user -> new UserAdminView(
                        user.id(),
                        user.username(),
                        user.realName(),
                        user.phone(),
                        user.status(),
                        user.createdAt(),
                        user.roles().stream()
                                .map(role -> new RoleAssignmentView(
                                        role.roleId(),
                                        role.roleCode(),
                                        role.roleName(),
                                        role.roleType(),
                                        role.scopeType(),
                                        role.scopeId(),
                                        role.scopeName()
                                ))
                                .toList()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> createUser(@Valid @RequestBody UserUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        if (!platformAdmin && identityAccessControlService.listManagedGroupIds(operatorId).isEmpty()) {
            throw new BusinessException("当前账号无用户创建权限");
        }
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        UserAccountDO user = userAdministrationService.createUser(request, phone);
        return CodeDataResponse.ok(new IdPayload(user.getId()));
    }

    @PutMapping("/{id}/status")
    @Transactional
    public CodeDataResponse<Void> updateUserStatus(@PathVariable Long id,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        if (!platformAdmin) {
            userAdministrationService.ensureCanManageUser(id, operatorId);
        }
        userAdministrationService.updateUserStatus(id, request);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/roles")
    @Transactional
    public CodeDataResponse<Void> assignUserRoles(@PathVariable Long id,
                                                  @Valid @RequestBody UserRoleAssignRequest request) {
        identityAdminLookupService.requireUser(id);
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        Set<Long> managedGroupIds = platformAdmin
                ? Collections.emptySet()
                : new HashSet<>(identityAccessControlService.listManagedGroupIds(operatorId));
        Set<Long> managedStoreIds = platformAdmin
                ? Collections.emptySet()
                : new HashSet<>(identityAccessControlService.listManagedStoreIds(managedGroupIds));
        if (!platformAdmin && userAdministrationService.hasEnabledRoleAssignments(id)) {
            userAdministrationService.ensureCanManageUser(id, operatorId);
        }
        userRoleAssignmentService.assignUserRoles(
                id,
                operatorId,
                platformAdmin,
                managedGroupIds,
                managedStoreIds,
                request.getAssignments()
        );
        return CodeDataResponse.ok();
    }
}
