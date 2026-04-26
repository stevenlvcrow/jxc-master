package com.boboboom.jxc.identity.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class UserRoleAssignmentService {

    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String ADMIN_USERNAME = "admin";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final String GROUP_MEMBER_ROLE_CODE = "GROUP_MEMBER";

    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final DictionaryLookupService dictionaryLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public UserRoleAssignmentService(RoleRepository roleRepositoryValue,
                                     StoreRepository storeRepositoryValue,
                                     UserAccountRepository userAccountRepositoryValue,
                                     UserRoleRelRepository userRoleRelRepositoryValue,
                                     DictionaryLookupService dictionaryLookupServiceValue) {
        this.roleRepository = roleRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.userAccountRepository = userAccountRepositoryValue;
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 分配用户角色。 */
    public void assignUserRoles(Long targetUserId,
                                Long operatorId,
                                boolean platformAdmin,
                                Set<Long> managedGroupIds,
                                Set<Long> managedStoreIds,
                                List<UserRoleAssignRequest.UserRoleAssignment> assignments) {
        UserAccountDO targetUser = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!platformAdmin) {
            ensureCanManageUser(targetUserId, managedGroupIds, managedStoreIds);
        }

        List<UserRoleRelDO> toInsert = new ArrayList<>();
        LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
        Map<Long, Long> storeGroupIds = new HashMap<>();
        List<UserRoleAssignRequest.UserRoleAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        boolean containsPlatformSuperAdminRole = false;
        for (UserRoleAssignRequest.UserRoleAssignment assignment : safeAssignments) {
            RoleDO role = requireAssignableRole(assignment.getRoleId(), platformAdmin);
            if (isPlatformSuperAdminRole(role)) {
                containsPlatformSuperAdminRole = true;
                ensureAdminAccount(targetUser);
            }
            String scopeType = normalizeScopeType(assignment.getScopeType(), role.getRoleType());
            Long scopeId = normalizeScopeId(scopeType, assignment.getScopeId());
            ensureRoleScopeMatches(role, scopeType);
            ensureAllowedAssignmentScope(platformAdmin, scopeType, scopeId, managedGroupIds, managedStoreIds);
            if (SCOPE_STORE.equals(scopeType)) {
                storeGroupIds.put(scopeId, requireStoreGroupId(scopeId));
            }
            String key = buildAssignmentKey(scopeType, scopeId, assignment.getRoleId());
            if (!seenKeys.add(key)) {
                throw new BusinessException("同一用户同一门店只能分配一个角色");
            }

            UserRoleRelDO rel = new UserRoleRelDO();
            rel.setUserId(targetUserId);
            rel.setRoleId(assignment.getRoleId());
            rel.setScopeType(scopeType);
            rel.setScopeId(scopeId);
            rel.setAssignedBy(operatorId);
            rel.setStatus(enabledStatus());
            toInsert.add(rel);
        }

        appendRequiredGroupMemberAssignments(toInsert, storeGroupIds, platformAdmin, managedGroupIds, seenKeys, operatorId, targetUserId);

        if (ADMIN_USERNAME.equalsIgnoreCase(targetUser.getUsername()) && !containsPlatformSuperAdminRole) {
            throw new BusinessException("admin 账号必须保留 PLATFORM_SUPER_ADMIN");
        }

        replaceExistingAssignments(targetUserId, platformAdmin, managedGroupIds, managedStoreIds);

        for (UserRoleRelDO rel : toInsert) {
            userRoleRelRepository.save(rel);
        }
    }

    private RoleDO requireAssignableRole(Long roleId, boolean platformAdmin) {
        if (roleId == null) {
            throw new BusinessException("角色ID不能为空");
        }
        RoleDO role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (!platformAdmin && SCOPE_PLATFORM.equals(role.getRoleType())) {
            throw new BusinessException("当前账号无平台角色授权权限");
        }
        return role;
    }

    private boolean isPlatformSuperAdminRole(RoleDO role) {
        return PLATFORM_SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode());
    }

    private void ensureAdminAccount(UserAccountDO targetUser) {
        if (!ADMIN_USERNAME.equalsIgnoreCase(targetUser.getUsername())) {
            throw new BusinessException("PLATFORM_SUPER_ADMIN 仅允许绑定 admin 账号");
        }
    }

    private void ensureAllowedAssignmentScope(boolean platformAdmin,
                                              String scopeType,
                                              Long scopeId,
                                              Set<Long> managedGroupIds,
                                              Set<Long> managedStoreIds) {
        if (!platformAdmin && !isAllowedScope(scopeType, scopeId, managedGroupIds, managedStoreIds)) {
            throw new BusinessException("包含无权限授权范围");
        }
    }

    private void replaceExistingAssignments(Long targetUserId,
                                            boolean platformAdmin,
                                            Set<Long> managedGroupIds,
                                            Set<Long> managedStoreIds) {
        if (platformAdmin) {
            userRoleRelRepository.deleteByUserId(targetUserId);
            return;
        }
        if (!managedGroupIds.isEmpty()) {
            userRoleRelRepository.deleteByUserIdAndGroupScopes(targetUserId, managedGroupIds);
        }
        if (!managedStoreIds.isEmpty()) {
            userRoleRelRepository.deleteByUserIdAndStoreScopes(targetUserId, managedStoreIds);
        }
    }

    private String normalizeScopeType(String rawScopeType, String roleType) {
        String scopeType = trimToNull(rawScopeType);
        if (scopeType != null) {
            return scopeType;
        }
        String roleTypeValue = trimToNull(roleType);
        if (SCOPE_GROUP.equals(roleTypeValue)) {
            return SCOPE_GROUP;
        }
        if (SCOPE_STORE.equals(roleTypeValue)) {
            return SCOPE_STORE;
        }
        return SCOPE_PLATFORM;
    }

    private Long normalizeScopeId(String scopeType, Long rawScopeId) {
        if (SCOPE_PLATFORM.equals(scopeType)) {
            if (rawScopeId != null) {
                throw new BusinessException("平台角色不支持指定作用域ID");
            }
            return null;
        }
        if (SCOPE_GROUP.equals(scopeType)) {
            if (rawScopeId == null) {
                throw new BusinessException("集团角色必须指定集团作用域");
            }
            return rawScopeId;
        }
        if (SCOPE_STORE.equals(scopeType)) {
            if (rawScopeId == null) {
                throw new BusinessException("门店角色必须指定门店作用域");
            }
            return rawScopeId;
        }
        return rawScopeId;
    }

    private void ensureRoleScopeMatches(RoleDO role, String scopeType) {
        String roleType = trimToNull(role.getRoleType());
        if (SCOPE_PLATFORM.equals(roleType) && SCOPE_PLATFORM.equals(scopeType)) {
            return;
        }
        if (SCOPE_GROUP.equals(roleType) && SCOPE_GROUP.equals(scopeType)) {
            return;
        }
        if (SCOPE_STORE.equals(roleType) && SCOPE_STORE.equals(scopeType)) {
            return;
        }
        throw new BusinessException("角色类型与授权范围不匹配");
    }

    private boolean isAllowedScope(String scopeType,
                                   Long scopeId,
                                   Set<Long> managedGroupIds,
                                   Set<Long> managedStoreIds) {
        if (SCOPE_GROUP.equals(scopeType)) {
            return scopeId != null && managedGroupIds.contains(scopeId);
        }
        if (SCOPE_STORE.equals(scopeType)) {
            return scopeId != null && managedStoreIds.contains(scopeId);
        }
        return false;
    }

    private Long requireStoreGroupId(Long storeId) {
        StoreDO store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException("门店不存在"));
        Long groupId = store.getGroupId();
        if (groupId == null || groupId <= 0) {
            throw new BusinessException("门店未绑定集团");
        }
        return groupId;
    }

    private void appendRequiredGroupMemberAssignments(List<UserRoleRelDO> toInsert,
                                                       Map<Long, Long> storeGroupIds,
                                                       boolean platformAdmin,
                                                       Set<Long> managedGroupIds,
                                                       Set<String> seenKeys,
                                                       Long operatorId,
                                                       Long targetUserId) {
        if (storeGroupIds.isEmpty()) {
            return;
        }
        for (Long groupId : new LinkedHashSet<>(storeGroupIds.values())) {
            if (!platformAdmin && (managedGroupIds == null || !managedGroupIds.contains(groupId))) {
                throw new BusinessException("门店角色必须同时具备所属集团授权");
            }
            RoleDO groupMemberRole = roleRepository.findByTenantGroupIdAndRoleCode(groupId, GROUP_MEMBER_ROLE_CODE)
                    .orElseThrow(() -> new BusinessException("集团成员角色未初始化"));
            String key = buildAssignmentKey(SCOPE_GROUP, groupId, groupMemberRole.getId());
            if (!seenKeys.add(key)) {
                continue;
            }
            UserRoleRelDO rel = new UserRoleRelDO();
            rel.setUserId(targetUserId);
            rel.setRoleId(groupMemberRole.getId());
            rel.setScopeType(SCOPE_GROUP);
            rel.setScopeId(groupId);
            rel.setAssignedBy(operatorId);
            rel.setStatus(enabledStatus());
            toInsert.add(rel);
        }
    }

    private void ensureCanManageUser(Long targetUserId,
                                     Set<Long> managedGroupIds,
                                     Set<Long> managedStoreIds) {
        if ((managedGroupIds == null || managedGroupIds.isEmpty())
                && (managedStoreIds == null || managedStoreIds.isEmpty())) {
            throw new BusinessException("当前账号无可管理用户范围");
        }

        if (!hasEnabledRoleAssignments(targetUserId)) {
            throw new BusinessException("当前账号无该用户操作权限");
        }

        Long matched = userRoleRelRepository.countByUserAndScopedRoles(targetUserId, enabledStatus(), managedGroupIds, managedStoreIds);
        if (matched == null || matched == 0) {
            throw new BusinessException("当前账号无该用户操作权限");
        }
    }

    private boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelRepository.countByUserIdAndStatus(userId, enabledStatus());
        return count != null && count > 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildAssignmentKey(String scopeType, Long scopeId, Long roleId) {
        if (SCOPE_STORE.equals(scopeType)) {
            return SCOPE_STORE + ":" + String.valueOf(scopeId);
        }
        return String.valueOf(roleId) + ":" + scopeType + ":" + String.valueOf(scopeId);
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }
}
