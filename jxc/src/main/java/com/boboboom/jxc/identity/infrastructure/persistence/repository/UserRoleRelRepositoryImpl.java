package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserRoleRelRepositoryImpl implements UserRoleRelRepository {

    private final UserRoleRelMapper userRoleRelMapper;

    public UserRoleRelRepositoryImpl(UserRoleRelMapper userRoleRelMapper) {
        this.userRoleRelMapper = userRoleRelMapper;
    }

    @Override
    public List<UserRoleRelDO> findByUserIdAndStatus(Long userId, String status) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status));
    }

    @Override
    public List<UserRoleRelDO> findByUserIdAndScopeTypeAndStatus(Long userId, String scopeType, String status) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getStatus, status)
                .isNotNull(UserRoleRelDO::getScopeId));
    }

    @Override
    public Optional<UserRoleRelDO> findByUserIdRoleAndScope(Long userId, Long roleId, String scopeType, Long scopeId) {
        return Optional.ofNullable(userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, roleId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .last("limit 1")));
    }

    @Override
    public List<UserRoleRelDO> findByStatusAndGroupScopes(String status, Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getStatus, status)
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .in(UserRoleRelDO::getScopeId, groupIds));
    }

    @Override
    public List<UserRoleRelDO> findByStatusAndGroupOrStoreScopes(String status, Set<Long> groupIds, Set<Long> storeIds) {
        if ((groupIds == null || groupIds.isEmpty()) && (storeIds == null || storeIds.isEmpty())) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserRoleRelDO> query = new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getStatus, status);
        if (groupIds != null && !groupIds.isEmpty() && storeIds != null && !storeIds.isEmpty()) {
            query.and(wrapper -> wrapper
                    .and(groupScoped -> groupScoped
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .in(UserRoleRelDO::getScopeId, groupIds))
                    .or(storeScoped -> storeScoped
                            .eq(UserRoleRelDO::getScopeType, "STORE")
                            .in(UserRoleRelDO::getScopeId, storeIds)));
        } else if (groupIds != null && !groupIds.isEmpty()) {
            query.eq(UserRoleRelDO::getScopeType, "GROUP")
                    .in(UserRoleRelDO::getScopeId, groupIds);
        } else {
            query.eq(UserRoleRelDO::getScopeType, "STORE")
                    .in(UserRoleRelDO::getScopeId, storeIds);
        }
        return userRoleRelMapper.selectList(query);
    }

    @Override
    public Long countByUserIdAndStatus(Long userId, String status) {
        return userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status));
    }

    @Override
    public Long countByUserAndScopedRoles(Long userId, String status, Set<Long> groupIds, Set<Long> storeIds) {
        if ((groupIds == null || groupIds.isEmpty()) && (storeIds == null || storeIds.isEmpty())) {
            return 0L;
        }
        LambdaQueryWrapper<UserRoleRelDO> query = new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status);
        if (groupIds != null && !groupIds.isEmpty() && storeIds != null && !storeIds.isEmpty()) {
            query.and(wrapper -> wrapper
                    .and(groupScoped -> groupScoped
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .in(UserRoleRelDO::getScopeId, groupIds))
                    .or(storeScoped -> storeScoped
                            .eq(UserRoleRelDO::getScopeType, "STORE")
                            .in(UserRoleRelDO::getScopeId, storeIds)));
        } else if (groupIds != null && !groupIds.isEmpty()) {
            query.eq(UserRoleRelDO::getScopeType, "GROUP")
                    .in(UserRoleRelDO::getScopeId, groupIds);
        } else {
            query.eq(UserRoleRelDO::getScopeType, "STORE")
                    .in(UserRoleRelDO::getScopeId, storeIds);
        }
        return userRoleRelMapper.selectCount(query);
    }

    @Override
    public void save(UserRoleRelDO rel) {
        userRoleRelMapper.insert(rel);
    }

    @Override
    public void update(UserRoleRelDO rel) {
        userRoleRelMapper.updateById(rel);
    }

    @Override
    public void deleteByUserId(Long userId) {
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>().eq(UserRoleRelDO::getUserId, userId));
    }

    @Override
    public void deleteByUserIdAndGroupScopes(Long userId, Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .in(UserRoleRelDO::getScopeId, groupIds));
    }

    @Override
    public void deleteByUserIdAndStoreScopes(Long userId, Set<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return;
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, "STORE")
                .in(UserRoleRelDO::getScopeId, storeIds));
    }
}
