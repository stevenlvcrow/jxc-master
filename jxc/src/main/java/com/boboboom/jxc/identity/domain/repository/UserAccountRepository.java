package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface UserAccountRepository {

    Optional<UserAccountDO> findById(Long id);

    Optional<UserAccountDO> findByPhone(String phone);

    Optional<UserAccountDO> findLoginUserByAccount(String account);

    List<UserAccountDO> findAllOrdered();

    List<UserAccountDO> findByIdsOrdered(List<Long> ids);

    List<UserAccountDO> findByCreatedGroupScopes(List<Long> groupIds);

    List<UserAccountDO> findByGroupScope(Long groupId);

    List<UserAccountDO> findRolelessUsersByCreatedScopes(List<Long> groupIds, List<Long> storeIds);

    List<UserRoleView> findUserRolesByUserIds(List<Long> userIds);

    List<UserRoleView> findUserRoles(Long userId);

    List<UserRoleView> findUsersByRoleAndScope(String roleCode, String scopeType, Long scopeId);

    void save(UserAccountDO user);

    void update(UserAccountDO user);

    void deleteById(Long id);
}
