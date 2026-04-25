package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserRoleAssignmentService {

    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String ADMIN_USERNAME = "admin";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final DictionaryLookupService dictionaryLookupService;

    public UserRoleAssignmentService(RoleRepository roleRepository,
                                     UserAccountRepository userAccountRepository,
                                     UserRoleRelRepository userRoleRelRepository,
                                     DictionaryLookupService dictionaryLookupService) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRoleRelRepository = userRoleRelRepository;
        this.dictionaryLookupService = dictionaryLookupService;
    }

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
        List<UserRoleAssignRequest.UserRoleAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        boolean containsPlatformSuperAdminRole = false;
        for (UserRoleAssignRequest.UserRoleAssignment assignment : safeAssignments) {
            if (assignment.getRoleId() == null) {
                throw new BusinessException("角色ID不能为空");
            }
            RoleDO role = roleRepository.findById(assignment.getRoleId()).orElse(null);
            if (role == null) {
                throw new BusinessException("角色不存在");
            }
            if (!platformAdmin && SCOPE_PLATFORM.equals(role.getRoleType())) {
                throw new BusinessException("当前账号无平台角色授权权限");
            }
            if (PLATFORM_SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
                containsPlatformSuperAdminRole = true;
                if (!ADMIN_USERNAME.equalsIgnoreCase(targetUser.getUsername())) {
                    throw new BusinessException("PLATFORM_SUPER_ADMIN 仅允许绑定 admin 账号");
                }
            }
            String scopeType = normalizeScopeType(assignment.getScopeType(), role.getRoleType());
            Long scopeId = normalizeScopeId(scopeType, assignment.getScopeId());
            if (!platformAdmin && !isAllowedScope(scopeType, scopeId, managedGroupIds, managedStoreIds)) {
                throw new BusinessException("包含无权限授权范围");
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

        if (ADMIN_USERNAME.equalsIgnoreCase(targetUser.getUsername()) && !containsPlatformSuperAdminRole) {
            throw new BusinessException("admin 账号必须保留 PLATFORM_SUPER_ADMIN");
        }

        if (platformAdmin) {
            userRoleRelRepository.deleteByUserId(targetUserId);
        } else {
            if (!managedGroupIds.isEmpty() && !managedStoreIds.isEmpty()) {
                userRoleRelRepository.deleteByUserIdAndGroupScopes(targetUserId, managedGroupIds);
                userRoleRelRepository.deleteByUserIdAndStoreScopes(targetUserId, managedStoreIds);
            } else if (!managedGroupIds.isEmpty()) {
                userRoleRelRepository.deleteByUserIdAndGroupScopes(targetUserId, managedGroupIds);
            } else {
                userRoleRelRepository.deleteByUserIdAndStoreScopes(targetUserId, managedStoreIds);
            }
        }

        for (UserRoleRelDO rel : toInsert) {
            userRoleRelRepository.save(rel);
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

    private void ensureCanManageUser(Long targetUserId,
                                     Set<Long> managedGroupIds,
                                     Set<Long> managedStoreIds) {
        if ((managedGroupIds == null || managedGroupIds.isEmpty())
                && (managedStoreIds == null || managedStoreIds.isEmpty())) {
            throw new BusinessException("当前账号无可管理用户范围");
        }

        if (!hasEnabledRoleAssignments(targetUserId)) {
            UserAccountDO user = userAccountRepository.findById(targetUserId).orElse(null);
            if (matchesCreatedScope(user, managedGroupIds, managedStoreIds)) {
                return;
            }
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

    private boolean matchesCreatedScope(UserAccountDO user,
                                        Set<Long> managedGroupIds,
                                        Set<Long> managedStoreIds) {
        if (user == null || user.getCreatedScopeId() == null) {
            return false;
        }
        if (SCOPE_GROUP.equals(user.getCreatedScopeType())) {
            return managedGroupIds != null && managedGroupIds.contains(user.getCreatedScopeId());
        }
        if (SCOPE_STORE.equals(user.getCreatedScopeType())) {
            return managedStoreIds != null && managedStoreIds.contains(user.getCreatedScopeId());
        }
        return false;
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
