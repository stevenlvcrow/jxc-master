package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface UserRoleRelRepository {

    List<UserRoleRelDO> findByUserIdAndStatus(Long userId, String status);

    List<UserRoleRelDO> findByUserIdAndScopeTypeAndStatus(Long userId, String scopeType, String status);

    boolean existsByUserIdAndRoleIdAndScopeTypeAndStatus(Long userId, Long roleId, String scopeType, String status);

    boolean existsByUserIdAndScopeTypeAndScopeIdAndStatus(Long userId, String scopeType, Long scopeId, String status);

    Optional<UserRoleRelDO> findByUserIdRoleAndScope(Long userId, Long roleId, String scopeType, Long scopeId);

    List<UserRoleRelDO> findByStatusAndGroupScopes(String status, Set<Long> groupIds);

    List<UserRoleRelDO> findByStatusAndGroupOrStoreScopes(String status, Set<Long> groupIds, Set<Long> storeIds);

    Long countByUserIdAndStatus(Long userId, String status);

    Long countByUserAndScopedRoles(Long userId, String status, Set<Long> groupIds, Set<Long> storeIds);

    Long countByScopeTypeAndScopeId(String scopeType, Long scopeId);

    void save(UserRoleRelDO rel);

    void update(UserRoleRelDO rel);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);

    void deleteByScopeTypeAndScopeId(String scopeType, Long scopeId);

    void deleteByUserIdAndGroupScopes(Long userId, Set<Long> groupIds);

    void deleteByUserIdAndStoreScopes(Long userId, Set<Long> storeIds);
}
