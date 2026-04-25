package com.boboboom.jxc.identity.interfaces.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.UserAdministrationService;
import com.boboboom.jxc.identity.application.service.UserRoleAssignmentService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest.UserBatchDeleteRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;

import jakarta.validation.Valid;

/**
 * 用户管理接口，负责用户查询、创建、编辑、状态控制以及角色分配。
 */
@Validated
@RestController
@RequestMapping("/api/identity/admin/users")
public class IdentityUserAdminController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final IdentityAccessControlService identityAccessControlService;
    private final UserAdministrationService userAdministrationService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;
    private final OrgScopeService orgScopeService;

    /**
     * 构造用户管理接口。
     *
     * @param identityAccessControlServiceValue 组织权限控制服务
     * @param userAdministrationServiceValue 用户管理服务
     * @param userRoleAssignmentServiceValue 用户角色分配服务
     * @param identityAdminLookupServiceValue 用户查询辅助服务
     * @param identityAdminSupportValue 当前登录管理员辅助服务
     * @param orgScopeServiceValue 组织范围解析服务
     */
    public IdentityUserAdminController(IdentityAccessControlService identityAccessControlServiceValue,
                                       UserAdministrationService userAdministrationServiceValue,
                                       UserRoleAssignmentService userRoleAssignmentServiceValue,
                                       IdentityAdminLookupService identityAdminLookupServiceValue,
                                       IdentityAdminSupport identityAdminSupportValue,
                                       OrgScopeService orgScopeServiceValue) {
        this.identityAccessControlService = identityAccessControlServiceValue;
        this.userAdministrationService = userAdministrationServiceValue;
        this.userRoleAssignmentService = userRoleAssignmentServiceValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
        this.identityAdminSupport = identityAdminSupportValue;
        this.orgScopeService = orgScopeServiceValue;
    }

    /**
     * 查询用户列表。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 用户列表响应
     */
    @GetMapping
    public CodeDataResponse<PageData<UserAdminView>> listUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
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
                                .filter(role -> OrgScopeService.SCOPE_GROUP.equals(role.scopeType()))
                                .filter(role -> role.scopeId() != null)
                                .map(role -> new UserGroupScopeView(role.scopeId(), role.scopeName()))
                                .distinct()
                                .toList(),
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
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 查询可选销售员候选人。
     *
     * @param orgId 机构标识
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 销售员候选人列表响应
     */
    @GetMapping("/salesmen")
    public CodeDataResponse<PageData<SalesmanCandidateView>> listSalesmen(@RequestParam String orgId,
                                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                                          @RequestParam(defaultValue = "10") Integer pageSize) {
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
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 新建用户。
     *
     * @param request 用户新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createUser(@Valid @RequestBody UserUpsertRequest request,
                                                  @RequestParam(required = false) String orgId) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        if (!platformAdmin && identityAccessControlService.listManagedGroupIds(operatorId).isEmpty()) {
            throw new BusinessException("当前账号无用户创建权限");
        }
        OrgScopeService.AccessibleScope scope = resolveCreateUserScope(operatorId, platformAdmin, orgId);
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        UserAccountDO user = userAdministrationService.createUser(request, phone, scope.scopeType(), scope.scopeId(), operatorId);
        return CodeDataResponse.ok(new IdPayload(user.getId()));
    }

    /**
     * 更新用户信息。
     *
     * @param id 用户主键
     * @param request 用户更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
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

    /**
     * 更新用户状态。
     *
     * @param id 用户主键
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
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

    /**
     * 删除单个用户。
     *
     * @param id 用户主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteUser(@PathVariable Long id) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        userAdministrationService.deleteUsers(operatorId, platformAdmin, List.of(id));
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除用户。
     *
     * @param request 批量删除请求
     * @return 空响应
     */
    @DeleteMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDeleteUsers(@Valid @RequestBody UserBatchDeleteRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        userAdministrationService.deleteUsers(operatorId, platformAdmin, request.getIds());
        return CodeDataResponse.ok();
    }

    /**
     * 为用户分配角色。
     *
     * @param id 用户主键
     * @param request 角色分配请求
     * @return 空响应
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
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

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }

    private OrgScopeService.AccessibleScope resolveCreateUserScope(Long operatorId,
                                                                   boolean platformAdmin,
                                                                   String orgId) {
        String normalizedOrgId = StringUtils.hasText(orgId) ? orgId.trim() : null;
        if (!StringUtils.hasText(normalizedOrgId)) {
            if (platformAdmin) {
                return new OrgScopeService.AccessibleScope(OrgScopeService.SCOPE_PLATFORM, 0L, 0L);
            }
            throw new BusinessException("请先选择集团机构");
        }

        if ("platform".equalsIgnoreCase(normalizedOrgId)) {
            if (platformAdmin) {
                return new OrgScopeService.AccessibleScope(OrgScopeService.SCOPE_PLATFORM, 0L, 0L);
            }
            throw new BusinessException("请先选择集团机构");
        }

        if (normalizedOrgId.startsWith("group-")) {
            return resolveCreateUserGroupScope(operatorId, normalizedOrgId);
        }

        if (normalizedOrgId.startsWith("store-")) {
            return resolveCreateUserStoreScope(operatorId, normalizedOrgId);
        }

        throw new BusinessException("机构参数非法");
    }

    private OrgScopeService.AccessibleScope resolveCreateUserGroupScope(Long operatorId, String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        if (!OrgScopeService.SCOPE_GROUP.equals(scope.scopeType())) {
            throw new BusinessException("请先选择集团机构");
        }
        return new OrgScopeService.AccessibleScope(OrgScopeService.SCOPE_GROUP, scope.scopeId(), scope.groupId());
    }

    private OrgScopeService.AccessibleScope resolveCreateUserStoreScope(Long operatorId, String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        Long groupId = scope.groupId();
        if (groupId == null || groupId <= 0) {
            throw new BusinessException("门店未绑定集团");
        }
        return new OrgScopeService.AccessibleScope(OrgScopeService.SCOPE_GROUP, groupId, groupId);
    }
}
