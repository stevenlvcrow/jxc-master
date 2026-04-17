package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class UserRoleAssignmentService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final RoleRepository roleRepository;
    private final UserRoleRelRepository userRoleRelRepository;

    public UserRoleAssignmentService(RoleRepository roleRepository,
                                     UserRoleRelRepository userRoleRelRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRelRepository = userRoleRelRepository;
    }

    public void assignUserRoles(Long targetUserId,
                                Long operatorId,
                                boolean platformAdmin,
                                Set<Long> managedGroupIds,
                                Set<Long> managedStoreIds,
                                List<UserRoleAssignRequest.UserRoleAssignment> assignments) {
        if (!platformAdmin && hasEnabledRoleAssignments(targetUserId)) {
            ensureCanManageUser(targetUserId, managedGroupIds, managedStoreIds);
        }

        List<UserRoleRelDO> toInsert = new ArrayList<>();
        LinkedHashMap<String, UserRoleAssignRequest.UserRoleAssignment> deduped = new LinkedHashMap<>();
        List<UserRoleAssignRequest.UserRoleAssignment> safeAssignments = assignments == null ? List.of() : assignments;
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
            String scopeType = normalizeScopeType(assignment.getScopeType(), role.getRoleType());
            Long scopeId = normalizeScopeId(scopeType, assignment.getScopeId());
            if (!platformAdmin && !isAllowedScope(scopeType, scopeId, managedGroupIds, managedStoreIds)) {
                throw new BusinessException("包含无权限授权范围");
            }
            String key = assignment.getRoleId() + ":" + scopeType + ":" + String.valueOf(scopeId);
            if (deduped.containsKey(key)) {
                continue;
            }
            deduped.put(key, assignment);

            UserRoleRelDO rel = new UserRoleRelDO();
            rel.setUserId(targetUserId);
            rel.setRoleId(assignment.getRoleId());
            rel.setScopeType(scopeType);
            rel.setScopeId(scopeId);
            rel.setAssignedBy(operatorId);
            rel.setStatus(STATUS_ENABLED);
            toInsert.add(rel);
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

        Long matched = userRoleRelRepository.countByUserAndScopedRoles(targetUserId, STATUS_ENABLED, managedGroupIds, managedStoreIds);
        if (matched == null || matched == 0) {
            throw new BusinessException("当前账号无该用户操作权限");
        }
    }

    private boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelRepository.countByUserIdAndStatus(userId, STATUS_ENABLED);
        return count != null && count > 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
