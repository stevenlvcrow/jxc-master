package com.boboboom.jxc.identity.application.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.domain.repository.AccountImportRecordRepository;
import com.boboboom.jxc.identity.domain.repository.AuditLogRepository;
import com.boboboom.jxc.identity.domain.repository.LoginLogRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserPasswordLogRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;

/** 用户管理业务服务，负责账号创建、编辑、状态和角色关联维护。 */
@Service
public class UserAdministrationService {

    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String PLATFORM_ADMIN_ROLE_CODE = "PLATFORM_ADMIN";
    private static final String ADMIN_USERNAME = "admin";
    private static final String SALESMAN_ROLE_CODE = "SALESMAN";
    private static final String GROUP_MEMBER_ROLE_CODE = "GROUP_MEMBER";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_STORE = "STORE";

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreRepository storeRepository;
    private final RoleRepository roleRepository;
    private final StoreAdminRelRepository storeAdminRelRepository;
    private final UserPasswordLogRepository userPasswordLogRepository;
    private final AccountImportRecordRepository accountImportRecordRepository;
    private final LoginLogRepository loginLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdentityAccessControlService identityAccessControlService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final UserCodeGenerator userCodeGenerator;

    /** 用户管理业务服务，负责账号创建、编辑、状态和角色关联维护。 */
    public UserAdministrationService(UserAccountRepository userAccountRepositoryValue,
                                     UserRoleRelRepository userRoleRelRepositoryValue,
                                     StoreRepository storeRepositoryValue,
                                     RoleRepository roleRepositoryValue,
                                     StoreAdminRelRepository storeAdminRelRepositoryValue,
                                     UserPasswordLogRepository userPasswordLogRepositoryValue,
                                     AccountImportRecordRepository accountImportRecordRepositoryValue,
                                     LoginLogRepository loginLogRepositoryValue,
                                     AuditLogRepository auditLogRepositoryValue,
                                     IdentityAccessControlService identityAccessControlServiceValue,
                                     IdentityAdminLookupService identityAdminLookupServiceValue,
                                     UserCodeGenerator userCodeGeneratorValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.storeAdminRelRepository = storeAdminRelRepositoryValue;
        this.userPasswordLogRepository = userPasswordLogRepositoryValue;
        this.accountImportRecordRepository = accountImportRecordRepositoryValue;
        this.loginLogRepository = loginLogRepositoryValue;
        this.auditLogRepository = auditLogRepositoryValue;
        this.identityAccessControlService = identityAccessControlServiceValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
        this.userCodeGenerator = userCodeGeneratorValue;
    }

    /** 创建用户。 */
    @Transactional
    public UserAccountDO createUser(UserUpsertRequest request,
                                    String phone,
                                    String createdScopeType,
                                    Long createdScopeId,
                                    Long operatorId) {
        if (userAccountRepository.findByPhone(phone).isPresent()) {
            throw new com.boboboom.jxc.common.BusinessException("手机号已存在");
        }

        String realName = identityAdminLookupService.trim(request.getRealName());
        UserAccountDO user = new UserAccountDO();
        user.setUsername(userCodeGenerator.generate(realName, phone));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setPasswordHash(PasswordCodec.encode("123654"));
        user.setPasswordSalt(null);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        user.setSourceType("MANUAL");
        user.setCreatedScopeType(createdScopeType);
        user.setCreatedScopeId(createdScopeId);
        user.setFirstLoginChangedPwd(Boolean.FALSE);
        userAccountRepository.save(user);
        assignDefaultRole(user.getId(), createdScopeType, createdScopeId, operatorId);
        return user;
    }

    /** 更新用户状态。 */
    @Transactional
    public UserAccountDO updateUserStatus(Long id, StatusUpdateRequest request) {
        UserAccountDO user = identityAdminLookupService.requireUser(id);
        ensureNotPlatformSuperAdminUser(user);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        userAccountRepository.update(user);
        return user;
    }

    /** 更新用户。 */
    @Transactional
    public UserAccountDO updateUser(Long id, UserUpsertRequest request) {
        UserAccountDO user = identityAdminLookupService.requireUser(id);
        ensureNotPlatformSuperAdminUser(user);
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        String realName = identityAdminLookupService.trim(request.getRealName());
        boolean phoneExists = userAccountRepository.findByPhone(phone)
                .map(UserAccountDO::getId)
                .filter(existingId -> !existingId.equals(id))
                .isPresent();
        if (phoneExists) {
            throw new com.boboboom.jxc.common.BusinessException("手机号已存在");
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setUsername(userCodeGenerator.generate(realName, phone));
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        userAccountRepository.update(user);
        return user;
    }

    /** 删除用户。 */
    @Transactional
    public void deleteUsers(Long operatorId, boolean platformAdmin, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>(userIds);
        for (Long userId : distinctIds) {
            if (userId == null) {
                continue;
            }
            UserAccountDO user = identityAdminLookupService.requireUser(userId);
            ensureNotPlatformSuperAdminUser(user);
            if (!platformAdmin) {
                ensureCanManageUser(userId, operatorId);
            }
            deleteUserRelations(userId);
            userAccountRepository.deleteById(userId);
        }
    }

    /** 查询用户列表。 */
    public List<UserAdminSnapshot> listUsers(Long operatorId, boolean platformAdmin) {
        List<UserAccountDO> users;
        if (platformAdmin) {
            users = userAccountRepository.findAllOrdered();
        } else {
            List<Long> managedGroupIds = identityAccessControlService.listManagedGroupIds(operatorId);
            if (managedGroupIds.isEmpty()) {
                return Collections.emptyList();
            }
            LinkedHashSet<Long> managedStoreIds = new LinkedHashSet<>(identityAccessControlService.listManagedStoreIds(new LinkedHashSet<>(managedGroupIds)));
            List<Long> userIds = userRoleRelRepository.findByStatusAndGroupOrStoreScopes(
                            identityAdminLookupService.enabledStatus(),
                            new LinkedHashSet<>(managedGroupIds),
                            managedStoreIds
                    )
                    .stream()
                    .map(UserRoleRelDO::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            users = userAccountRepository.findByIdsOrdered(userIds);
        }

        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(UserAccountDO::getId).toList();
        Map<Long, List<UserRoleView>> userRolesMap = userAccountRepository.findUserRolesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleView::getUserId));

        return users.stream()
                .filter(user -> !ADMIN_USERNAME.equalsIgnoreCase(user.getUsername()))
                .filter(user -> !hasPlatformSuperAdminRole(userRolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                .map(user -> new UserAdminSnapshot(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        normalizeRoleSnapshots(userRolesMap.getOrDefault(user.getId(), Collections.emptyList())).stream()
                                .map(role -> new RoleAssignmentSnapshot(
                                        role.getRoleId(),
                                        role.getRoleCode(),
                                        role.getRoleName(),
                                        role.getRoleType(),
                                        role.getScopeType(),
                                        role.getScopeId(),
                                        role.getScopeName(),
                                        isBuiltinRole(role.getRoleId())
                                ))
                                .toList()
                ))
                .toList();
    }

    /** 查询集团用户Options列表。 */
    public List<UserOptionSnapshot> listGroupUserOptions(Long groupId) {
        List<UserAccountDO> users = userAccountRepository.findByGroupScope(groupId);
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(UserAccountDO::getId).toList();
        Map<Long, List<UserRoleView>> userRolesMap = userAccountRepository.findUserRolesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleView::getUserId));
        return users.stream()
                .filter(user -> !ADMIN_USERNAME.equalsIgnoreCase(user.getUsername()))
                .filter(user -> !hasPlatformSuperAdminRole(userRolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                .map(user -> new UserOptionSnapshot(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getStatus()
                ))
                .toList();
    }

    /** 查询门店Salesmen列表。 */
    public List<SalesmanCandidateSnapshot> listStoreSalesmen(Long storeId) {
        if (storeId == null || storeId <= 0) {
            return Collections.emptyList();
        }
        Map<Long, UserRoleView> uniqueUsers = new LinkedHashMap<>();
        for (UserRoleView role : userAccountRepository.findUsersByRoleAndScope("SALESMAN", "STORE", storeId)) {
            if (role == null || role.getUserId() == null) {
                continue;
            }
            uniqueUsers.putIfAbsent(role.getUserId(), role);
        }
        return uniqueUsers.values().stream()
                .map(role -> new SalesmanCandidateSnapshot(
                        role.getUserId(),
                        role.getRealName(),
                        role.getPhone()
                ))
                .toList();
    }

    /** 校验并保证CanManage用户满足业务规则。 */
    public void ensureCanManageUser(Long targetUserId, Long operatorId) {
        LinkedHashSet<Long> managedGroupIds = new LinkedHashSet<>(identityAccessControlService.listManagedGroupIds(operatorId));
        LinkedHashSet<Long> managedStoreIds = new LinkedHashSet<>(identityAccessControlService.listManagedStoreIds(managedGroupIds));
        if (managedGroupIds.isEmpty() && managedStoreIds.isEmpty()) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无可管理用户范围");
        }

        if (!hasEnabledRoleAssignments(targetUserId)) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无该用户操作权限");
        }

        Long matched = userRoleRelRepository.countByUserAndScopedRoles(
                targetUserId, identityAdminLookupService.enabledStatus(), managedGroupIds, managedStoreIds
        );
        if (matched == null || matched == 0) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无该用户操作权限");
        }
    }

    /** 判断是否具备Enabled角色分配关系。 */
    public boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelRepository.countByUserIdAndStatus(userId, identityAdminLookupService.enabledStatus());
        return count != null && count > 0;
    }

    /** 身份与权限快照模型，承载用户管理快照查询结果。 */
    public record UserAdminSnapshot(Long id,
                                    String username,
                                    String realName,
                                    String phone,
                                    String status,
                                    LocalDateTime createdAt,
                                    List<RoleAssignmentSnapshot> roles) {
    }

    /** 身份与权限快照模型，承载角色分配快照查询结果。 */
    public record RoleAssignmentSnapshot(Long roleId,
                                         String roleCode,
                                         String roleName,
                                         String roleType,
                                         String scopeType,
                                         Long scopeId,
                                         String scopeName,
                                         boolean builtin) {
    }

    /** 身份与权限快照模型，承载Salesman候选项快照查询结果。 */
    public record SalesmanCandidateSnapshot(Long userId,
                                            String realName,
                                            String phone) {
    }

    /** 身份与权限快照模型，承载用户选项快照查询结果。 */
    public record UserOptionSnapshot(Long userId,
                                     String username,
                                     String realName,
                                     String phone,
                                     String status) {
    }

    private List<UserRoleView> normalizeRoleSnapshots(List<UserRoleView> roleSnapshots) {
        if (roleSnapshots == null || roleSnapshots.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserRoleView> uniqueRoles = new LinkedHashMap<>();
        for (UserRoleView role : roleSnapshots) {
            if (role == null) {
                continue;
            }
            String key = buildRoleSnapshotKey(role);
            uniqueRoles.putIfAbsent(key, role);
        }
        return new java.util.ArrayList<>(uniqueRoles.values());
    }

    private String buildRoleSnapshotKey(UserRoleView role) {
        String scopeType = role.getScopeType();
        Long scopeId = role.getScopeId();
        if ("STORE".equals(scopeType)) {
            return "STORE:" + String.valueOf(scopeId);
        }
        return role.getRoleId() + ":" + scopeType + ":" + String.valueOf(scopeId);
    }

    private boolean isBuiltinRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        return roleRepository.findById(roleId)
                .map(role -> {
                    String roleCode = role.getRoleCode();
                    if ("PLATFORM_SUPER_ADMIN".equals(roleCode)) {
                        return true;
                    }
                    if ("PLATFORM".equals(role.getRoleType())
                            && role.getTenantGroupId() != null
                            && role.getTenantGroupId() == 0L) {
                        return true;
                    }
                    return roleCode != null && !roleCode.startsWith("JSBM");
                })
                .orElse(false);
    }

    private void ensureNotPlatformSuperAdminUser(UserAccountDO user) {
        if (user == null) {
            return;
        }
        if (ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new com.boboboom.jxc.common.BusinessException("admin 超管账号不允许在用户管理中维护");
        }
        if (hasPlatformSuperAdminRole(userAccountRepository.findUserRoles(user.getId()))) {
            throw new com.boboboom.jxc.common.BusinessException("平台超管账号不允许在用户管理中维护");
        }
    }

    private boolean hasPlatformSuperAdminRole(List<UserRoleView> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> role != null
                && PLATFORM_SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode()));
    }

    private void deleteUserRelations(Long userId) {
        userRoleRelRepository.deleteByUserId(userId);
        storeAdminRelRepository.deleteByUserId(userId);
        userPasswordLogRepository.deleteByUserId(userId);
        accountImportRecordRepository.deleteByUserId(userId);
        loginLogRepository.deleteByUserId(userId);
        auditLogRepository.deleteByOperatorUserId(userId);
    }

    private void assignDefaultRole(Long userId, String createdScopeType, Long createdScopeId, Long operatorId) {
        if (SCOPE_PLATFORM.equals(createdScopeType)) {
            RoleDO platformAdminRole = roleRepository.findByTenantGroupIdAndRoleCode(0L, PLATFORM_ADMIN_ROLE_CODE)
                    .orElseThrow(() -> new com.boboboom.jxc.common.BusinessException("平台管理员角色未初始化"));
            saveRoleRel(userId, platformAdminRole.getId(), SCOPE_PLATFORM, null, operatorId);
            return;
        }
        if (SCOPE_GROUP.equals(createdScopeType)) {
            Long groupId = createdScopeId;
            if (groupId == null || groupId <= 0) {
                throw new com.boboboom.jxc.common.BusinessException("集团机构非法");
            }
            identityAdminLookupService.ensureGroupBuiltinRoles(groupId, operatorId);
            RoleDO groupMemberRole = roleRepository.findByTenantGroupIdAndRoleCode(groupId, GROUP_MEMBER_ROLE_CODE)
                    .orElseThrow(() -> new com.boboboom.jxc.common.BusinessException("集团成员角色未初始化"));
            RoleDO salesmanRole = roleRepository.findByTenantGroupIdAndRoleCode(groupId, SALESMAN_ROLE_CODE)
                    .orElseThrow(() -> new com.boboboom.jxc.common.BusinessException("门店业务员角色未初始化"));
            List<com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO> groupStores = storeRepository.findByGroupId(groupId);
            if (groupStores.isEmpty()) {
                throw new com.boboboom.jxc.common.BusinessException("集团下没有门店，无法默认分配门店业务员角色");
            }
            saveRoleRel(userId, groupMemberRole.getId(), SCOPE_GROUP, groupId, operatorId);
            for (com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO store : groupStores) {
                saveRoleRel(userId, salesmanRole.getId(), SCOPE_STORE, store.getId(), operatorId);
            }
            return;
        }
        throw new com.boboboom.jxc.common.BusinessException("用户创建机构非法");
    }

    private void saveRoleRel(Long userId, Long roleId, String scopeType, Long scopeId, Long operatorId) {
        UserRoleRelDO rel = new UserRoleRelDO();
        rel.setUserId(userId);
        rel.setRoleId(roleId);
        rel.setScopeType(scopeType);
        rel.setScopeId(scopeId);
        rel.setAssignedBy(operatorId);
        rel.setAssignedAt(LocalDateTime.now());
        rel.setStatus(identityAdminLookupService.enabledStatus());
        userRoleRelRepository.save(rel);
    }
}
