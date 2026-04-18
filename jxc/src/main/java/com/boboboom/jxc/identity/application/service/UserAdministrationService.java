package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.identity.domain.repository.AccountImportRecordRepository;
import com.boboboom.jxc.identity.domain.repository.AuditLogRepository;
import com.boboboom.jxc.identity.domain.repository.LoginLogRepository;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserPasswordLogRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";

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

    public UserAdministrationService(UserAccountRepository userAccountRepository,
                                     UserRoleRelRepository userRoleRelRepository,
                                     StoreRepository storeRepository,
                                     RoleRepository roleRepository,
                                     StoreAdminRelRepository storeAdminRelRepository,
                                     UserPasswordLogRepository userPasswordLogRepository,
                                     AccountImportRecordRepository accountImportRecordRepository,
                                     LoginLogRepository loginLogRepository,
                                     AuditLogRepository auditLogRepository,
                                     IdentityAccessControlService identityAccessControlService,
                                     IdentityAdminLookupService identityAdminLookupService) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRelRepository = userRoleRelRepository;
        this.storeRepository = storeRepository;
        this.roleRepository = roleRepository;
        this.storeAdminRelRepository = storeAdminRelRepository;
        this.userPasswordLogRepository = userPasswordLogRepository;
        this.accountImportRecordRepository = accountImportRecordRepository;
        this.loginLogRepository = loginLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.identityAccessControlService = identityAccessControlService;
        this.identityAdminLookupService = identityAdminLookupService;
    }

    @Transactional
    public UserAccountDO createUser(UserUpsertRequest request, String phone) {
        if (userAccountRepository.findByPhone(phone).isPresent()) {
            throw new com.boboboom.jxc.common.BusinessException("手机号已存在");
        }

        UserAccountDO user = new UserAccountDO();
        user.setUsername(phone);
        user.setRealName(identityAdminLookupService.trim(request.getRealName()));
        user.setPhone(phone);
        user.setPasswordHash(PasswordCodec.encode("123654"));
        user.setPasswordSalt(null);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        user.setSourceType("MANUAL");
        user.setFirstLoginChangedPwd(Boolean.FALSE);
        userAccountRepository.save(user);
        return user;
    }

    @Transactional
    public UserAccountDO updateUserStatus(Long id, StatusUpdateRequest request) {
        UserAccountDO user = identityAdminLookupService.requireUser(id);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        userAccountRepository.update(user);
        return user;
    }

    @Transactional
    public UserAccountDO updateUser(Long id, UserUpsertRequest request) {
        UserAccountDO user = identityAdminLookupService.requireUser(id);
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        boolean phoneExists = userAccountRepository.findByPhone(phone)
                .map(UserAccountDO::getId)
                .filter(existingId -> !existingId.equals(id))
                .isPresent();
        if (phoneExists) {
            throw new com.boboboom.jxc.common.BusinessException("手机号已存在");
        }
        user.setRealName(identityAdminLookupService.trim(request.getRealName()));
        user.setPhone(phone);
        user.setUsername(phone);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        userAccountRepository.update(user);
        return user;
    }

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
            identityAdminLookupService.requireUser(userId);
            if (!platformAdmin && hasEnabledRoleAssignments(userId)) {
                ensureCanManageUser(userId, operatorId);
            }
            deleteUserRelations(userId);
            userAccountRepository.deleteById(userId);
        }
    }

    public List<UserAdminSnapshot> listUsers(Long operatorId, boolean platformAdmin) {
        List<UserAccountDO> users;
        if (platformAdmin) {
            users = userAccountRepository.findAllOrdered();
        } else {
            List<Long> managedGroupIds = userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(operatorId, "GROUP", STATUS_ENABLED)
                    .stream()
                    .map(UserRoleRelDO::getScopeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (managedGroupIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> managedStoreIds = storeRepository.findByGroupIds(managedGroupIds)
                    .stream()
                    .map(store -> store.getId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<Long> scopedUserIds = userRoleRelRepository.findByStatusAndGroupOrStoreScopes(
                            STATUS_ENABLED, new LinkedHashSet<>(managedGroupIds), new LinkedHashSet<>(managedStoreIds))
                    .stream()
                    .map(UserRoleRelDO::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            LinkedHashSet<Long> visibleUserIds = new LinkedHashSet<>(scopedUserIds);
            if (visibleUserIds.isEmpty()) {
                return Collections.emptyList();
            }

            users = userAccountRepository.findByIdsOrdered(new java.util.ArrayList<>(visibleUserIds));
        }

        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(UserAccountDO::getId).toList();
        Map<Long, List<UserRoleView>> userRolesMap = userAccountRepository.findUserRolesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleView::getUserId));

        return users.stream()
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

    public void ensureCanManageUser(Long targetUserId, Long operatorId) {
        LinkedHashSet<Long> managedGroupIds = new LinkedHashSet<>(identityAccessControlService.listManagedGroupIds(operatorId));
        LinkedHashSet<Long> managedStoreIds = new LinkedHashSet<>(identityAccessControlService.listManagedStoreIds(managedGroupIds));
        if (managedGroupIds.isEmpty() && managedStoreIds.isEmpty()) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无可管理用户范围");
        }

        Long matched = userRoleRelRepository.countByUserAndScopedRoles(
                targetUserId, STATUS_ENABLED, managedGroupIds, managedStoreIds
        );
        if (matched == null || matched == 0) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无该用户操作权限");
        }
    }

    public boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelRepository.countByUserIdAndStatus(userId, STATUS_ENABLED);
        return count != null && count > 0;
    }

    public record UserAdminSnapshot(Long id,
                                    String username,
                                    String realName,
                                    String phone,
                                    String status,
                                    LocalDateTime createdAt,
                                    List<RoleAssignmentSnapshot> roles) {
    }

    public record RoleAssignmentSnapshot(Long roleId,
                                         String roleCode,
                                         String roleName,
                                         String roleType,
                                         String scopeType,
                                         Long scopeId,
                                         String scopeName,
                                         boolean builtin) {
    }

    public record SalesmanCandidateSnapshot(Long userId,
                                            String realName,
                                            String phone) {
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
                    String description = role.getDescription();
                    if ("GROUP_ADMIN".equals(roleCode) || "STORE_ADMIN".equals(roleCode)) {
                        return true;
                    }
                    return "GROUP_ROLE_TEMPLATE".equals(description);
                })
                .orElse(false);
    }

    private void deleteUserRelations(Long userId) {
        userRoleRelRepository.deleteByUserId(userId);
        storeAdminRelRepository.deleteByUserId(userId);
        userPasswordLogRepository.deleteByUserId(userId);
        accountImportRecordRepository.deleteByUserId(userId);
        loginLogRepository.deleteByUserId(userId);
        auditLogRepository.deleteByOperatorUserId(userId);
    }
}
