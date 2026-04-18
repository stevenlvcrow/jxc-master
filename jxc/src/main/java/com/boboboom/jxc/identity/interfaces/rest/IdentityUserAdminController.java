package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.UserAdministrationService;
import com.boboboom.jxc.identity.application.service.UserRoleAssignmentService;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest.UserBatchDeleteRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/identity/admin/users")
/**
 * 用户管理接口，负责用户查询、创建、编辑、状态控制以及角色分配。
 */
public class IdentityUserAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final UserAdministrationService userAdministrationService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;
    private final OrgScopeService orgScopeService;

    /**
     * 构造用户管理接口。
     *
     * @param identityAccessControlService 组织权限控制服务
     * @param userAdministrationService 用户管理服务
     * @param userRoleAssignmentService 用户角色分配服务
     * @param identityAdminLookupService 用户查询辅助服务
     * @param identityAdminSupport 当前登录管理员辅助服务
     * @param orgScopeService 组织范围解析服务
     */
    public IdentityUserAdminController(IdentityAccessControlService identityAccessControlService,
                                       UserAdministrationService userAdministrationService,
                                       UserRoleAssignmentService userRoleAssignmentService,
                                       IdentityAdminLookupService identityAdminLookupService,
                                       IdentityAdminSupport identityAdminSupport,
                                       OrgScopeService orgScopeService) {
        this.identityAccessControlService = identityAccessControlService;
        this.userAdministrationService = userAdministrationService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.identityAdminLookupService = identityAdminLookupService;
        this.identityAdminSupport = identityAdminSupport;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping
    /**
     * 查询用户列表。
     *
     * @return 用户列表响应
     */
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
                                        role.scopeName(),
                                        role.builtin()
                                ))
                                .toList()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/salesmen")
    /**
     * 查询可选销售员候选人。
     *
     * @param orgId 机构标识
     * @return 销售员候选人列表响应
     */
    public CodeDataResponse<List<SalesmanCandidateView>> listSalesmen(@org.springframework.web.bind.annotation.RequestParam String orgId) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        if (!OrgScopeService.SCOPE_STORE.equals(scope.scopeType())) {
            throw new BusinessException("请先选择门店机构");
        }
        List<SalesmanCandidateView> result = userAdministrationService.listStoreSalesmen(scope.scopeId()).stream()
                .map(candidate -> new SalesmanCandidateView(
                        candidate.userId(),
                        candidate.realName(),
                        candidate.phone()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建用户。
     *
     * @param request 用户新增请求
     * @return 新建结果
     */
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

    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新用户信息。
     *
     * @param id 用户主键
     * @param request 用户更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateUser(@PathVariable Long id,
                                             @Valid @RequestBody UserUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        if (!platformAdmin) {
            userAdministrationService.ensureCanManageUser(id, operatorId);
        }
        userAdministrationService.updateUser(id, request);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新用户状态。
     *
     * @param id 用户主键
     * @param request 状态更新请求
     * @return 空响应
     */
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

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除单个用户。
     *
     * @param id 用户主键
     * @return 空响应
     */
    public CodeDataResponse<Void> deleteUser(@PathVariable Long id) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        userAdministrationService.deleteUsers(operatorId, platformAdmin, List.of(id));
        return CodeDataResponse.ok();
    }

    @DeleteMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 批量删除用户。
     *
     * @param request 批量删除请求
     * @return 空响应
     */
    public CodeDataResponse<Void> batchDeleteUsers(@Valid @RequestBody UserBatchDeleteRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        userAdministrationService.deleteUsers(operatorId, platformAdmin, request.getIds());
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 为用户分配角色。
     *
     * @param id 用户主键
     * @param request 角色分配请求
     * @return 空响应
     */
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
