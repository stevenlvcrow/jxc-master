package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public UserAccountRepositoryImpl(UserAccountMapper userAccountMapperValue) {
        this.userAccountMapper = userAccountMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<UserAccountDO> findById(Long id) {
        return Optional.ofNullable(userAccountMapper.selectById(id));
    }

    /** 查询By手机号。 */
    @Override
    public Optional<UserAccountDO> findByPhone(String phone) {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getPhone, phone)
                .orderByDesc(UserAccountDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询Login用户By账号。 */
    @Override
    public Optional<UserAccountDO> findLoginUserByAccount(String account) {
        return Optional.ofNullable(userAccountMapper.selectLoginUserByAccount(account));
    }

    /** 查询All排序。 */
    @Override
    public List<UserAccountDO> findAllOrdered() {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .orderByDesc(UserAccountDO::getCreatedAt)
                .orderByDesc(UserAccountDO::getId));
    }

    /** 查询By标识排序。 */
    @Override
    public List<UserAccountDO> findByIdsOrdered(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .in(UserAccountDO::getId, ids)
                .orderByDesc(UserAccountDO::getCreatedAt)
                .orderByDesc(UserAccountDO::getId));
    }

    /** 查询ByCreated集团Scopes。 */
    @Override
    public List<UserAccountDO> findByCreatedGroupScopes(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUsersByCreatedGroupScopes(groupIds);
    }

    /** 查询By集团作用域。 */
    @Override
    public List<UserAccountDO> findByGroupScope(Long groupId) {
        if (groupId == null || groupId <= 0) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUsersByGroupScope(groupId);
    }

    /** 查询Roleless用户ByCreatedScopes。 */
    @Override
    public List<UserAccountDO> findRolelessUsersByCreatedScopes(List<Long> groupIds, List<Long> storeIds) {
        if ((groupIds == null || groupIds.isEmpty()) && (storeIds == null || storeIds.isEmpty())) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectRolelessUsersByCreatedScopes(groupIds, storeIds);
    }

    /** 查询用户角色By用户标识。 */
    @Override
    public List<UserRoleView> findUserRolesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUserRolesByUserIds(userIds);
    }

    /** 查询用户角色。 */
    @Override
    public List<UserRoleView> findUserRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUserRoles(userId);
    }

    /** 查询用户By角色And作用域。 */
    @Override
    public List<UserRoleView> findUsersByRoleAndScope(String roleCode, String scopeType, Long scopeId) {
        if (scopeId == null) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUsersByRoleAndScope(roleCode, scopeType, scopeId);
    }

    /** 保存业务数据。 */
    @Override
    public void save(UserAccountDO user) {
        userAccountMapper.insert(user);
    }

    /** 更新业务记录。 */
    @Override
    public void update(UserAccountDO user) {
        userAccountMapper.updateById(user);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        userAccountMapper.deleteById(id);
    }
}
