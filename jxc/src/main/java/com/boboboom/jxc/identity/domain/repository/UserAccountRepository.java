package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccountDO> findById(Long id);

    Optional<UserAccountDO> findByPhone(String phone);

    Optional<UserAccountDO> findLoginUserByAccount(String account);

    List<UserAccountDO> findAllOrdered();

    List<UserAccountDO> findByIdsOrdered(List<Long> ids);

    List<UserRoleView> findUserRolesByUserIds(List<Long> userIds);

    List<UserRoleView> findUserRoles(Long userId);

    List<UserRoleView> findUsersByRoleAndScope(String roleCode, String scopeType, Long scopeId);

    void save(UserAccountDO user);

    void update(UserAccountDO user);

    void deleteById(Long id);
}
