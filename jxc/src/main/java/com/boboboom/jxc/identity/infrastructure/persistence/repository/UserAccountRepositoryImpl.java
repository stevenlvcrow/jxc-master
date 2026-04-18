package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;

    public UserAccountRepositoryImpl(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public Optional<UserAccountDO> findById(Long id) {
        return Optional.ofNullable(userAccountMapper.selectById(id));
    }

    @Override
    public Optional<UserAccountDO> findByPhone(String phone) {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getPhone, phone)
                .orderByDesc(UserAccountDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserAccountDO> findLoginUserByAccount(String account) {
        return Optional.ofNullable(userAccountMapper.selectLoginUserByAccount(account));
    }

    @Override
    public List<UserAccountDO> findAllOrdered() {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .orderByDesc(UserAccountDO::getCreatedAt)
                .orderByDesc(UserAccountDO::getId));
    }

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

    @Override
    public List<UserRoleView> findUserRolesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUserRolesByUserIds(userIds);
    }

    @Override
    public List<UserRoleView> findUserRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUserRoles(userId);
    }

    @Override
    public List<UserRoleView> findUsersByRoleAndScope(String roleCode, String scopeType, Long scopeId) {
        if (scopeId == null) {
            return Collections.emptyList();
        }
        return userAccountMapper.selectUsersByRoleAndScope(roleCode, scopeType, scopeId);
    }

    @Override
    public void save(UserAccountDO user) {
        userAccountMapper.insert(user);
    }

    @Override
    public void update(UserAccountDO user) {
        userAccountMapper.updateById(user);
    }

    @Override
    public void deleteById(Long id) {
        userAccountMapper.deleteById(id);
    }
}
