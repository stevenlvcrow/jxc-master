package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRoleRelRepository {

    List<UserRoleRelDO> findByUserIdAndStatus(Long userId, String status);

    List<UserRoleRelDO> findByUserIdAndScopeTypeAndStatus(Long userId, String scopeType, String status);

    Optional<UserRoleRelDO> findByUserIdRoleAndScope(Long userId, Long roleId, String scopeType, Long scopeId);

    List<UserRoleRelDO> findByStatusAndGroupScopes(String status, Set<Long> groupIds);

    List<UserRoleRelDO> findByStatusAndGroupOrStoreScopes(String status, Set<Long> groupIds, Set<Long> storeIds);

    Long countByUserIdAndStatus(Long userId, String status);

    Long countByUserAndScopedRoles(Long userId, String status, Set<Long> groupIds, Set<Long> storeIds);

    void save(UserRoleRelDO rel);

    void update(UserRoleRelDO rel);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndGroupScopes(Long userId, Set<Long> groupIds);

    void deleteByUserIdAndStoreScopes(Long userId, Set<Long> storeIds);
}
