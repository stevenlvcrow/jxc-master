package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class UserRoleRelRepositoryImpl implements UserRoleRelRepository {

    private final UserRoleRelMapper userRoleRelMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public UserRoleRelRepositoryImpl(UserRoleRelMapper userRoleRelMapperValue) {
        this.userRoleRelMapper = userRoleRelMapperValue;
    }

    /** 查询By用户标识And状态。 */
    @Override
    public List<UserRoleRelDO> findByUserIdAndStatus(Long userId, String status) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status));
    }

    /** 查询By用户标识And作用域类型And状态。 */
    @Override
    public List<UserRoleRelDO> findByUserIdAndScopeTypeAndStatus(Long userId, String scopeType, String status) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getStatus, status)
                .isNotNull(UserRoleRelDO::getScopeId));
    }

    /** 判断By用户标识And角色标识And作用域类型And状态是否存在。 */
    @Override
    public boolean existsByUserIdAndRoleIdAndScopeTypeAndStatus(Long userId, Long roleId, String scopeType, String status) {
        if (userId == null || roleId == null || scopeType == null || status == null) {
            return false;
        }
        Long count = userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, roleId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getStatus, status));
        return count != null && count > 0;
    }

    /** 判断By用户标识And作用域类型And作用域标识And状态是否存在。 */
    @Override
    public boolean existsByUserIdAndScopeTypeAndScopeIdAndStatus(Long userId, String scopeType, Long scopeId, String status) {
        if (userId == null || scopeType == null || scopeId == null || status == null) {
            return false;
        }
        Long count = userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .eq(UserRoleRelDO::getStatus, status));
        return count != null && count > 0;
    }

    /** 查询By用户标识角色And作用域。 */
    @Override
    public Optional<UserRoleRelDO> findByUserIdRoleAndScope(Long userId, Long roleId, String scopeType, Long scopeId) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, roleId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .orderByDesc(UserRoleRelDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询By状态And集团Scopes。 */
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

    /** 查询By状态And集团Or门店Scopes。 */
    @Override
    public List<UserRoleRelDO> findByStatusAndGroupOrStoreScopes(String status, Set<Long> groupIds, Set<Long> storeIds) {
        if ((groupIds == null || groupIds.isEmpty()) && (storeIds == null || storeIds.isEmpty())) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserRoleRelDO> query = new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getStatus, status);
        applyGroupOrStoreScopeFilter(query, groupIds, storeIds);
        return userRoleRelMapper.selectList(query);
    }

    private void applyGroupOrStoreScopeFilter(LambdaQueryWrapper<UserRoleRelDO> query, Set<Long> groupIds, Set<Long> storeIds) {
        boolean hasGroupIds = groupIds != null && !groupIds.isEmpty();
        boolean hasStoreIds = storeIds != null && !storeIds.isEmpty();
        if (hasGroupIds && hasStoreIds) {
            query.and(wrapper -> wrapper
                    .and(groupScoped -> groupScoped
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .in(UserRoleRelDO::getScopeId, groupIds))
                    .or(storeScoped -> storeScoped
                            .eq(UserRoleRelDO::getScopeType, "STORE")
                            .in(UserRoleRelDO::getScopeId, storeIds)));
        } else if (hasGroupIds) {
            query.eq(UserRoleRelDO::getScopeType, "GROUP")
                    .in(UserRoleRelDO::getScopeId, groupIds);
        } else {
            query.eq(UserRoleRelDO::getScopeType, "STORE")
                    .in(UserRoleRelDO::getScopeId, storeIds);
        }
    }

    /** 统计By用户标识And状态数量。 */
    @Override
    public Long countByUserIdAndStatus(Long userId, String status) {
        return userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status));
    }

    /** 统计By用户AndScoped角色数量。 */
    @Override
    public Long countByUserAndScopedRoles(Long userId, String status, Set<Long> groupIds, Set<Long> storeIds) {
        if ((groupIds == null || groupIds.isEmpty()) && (storeIds == null || storeIds.isEmpty())) {
            return 0L;
        }
        LambdaQueryWrapper<UserRoleRelDO> query = new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, status);
        applyGroupOrStoreScopeFilter(query, groupIds, storeIds);
        return userRoleRelMapper.selectCount(query);
    }

    /** 统计By作用域类型And作用域标识数量。 */
    @Override
    public Long countByScopeTypeAndScopeId(String scopeType, Long scopeId) {
        if (scopeType == null || scopeId == null) {
            return 0L;
        }
        return userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(UserRoleRelDO rel) {
        userRoleRelMapper.insert(rel);
    }

    /** 更新业务记录。 */
    @Override
    public void update(UserRoleRelDO rel) {
        userRoleRelMapper.updateById(rel);
    }

    /** 删除By用户标识。 */
    @Override
    public void deleteByUserId(Long userId) {
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>().eq(UserRoleRelDO::getUserId, userId));
    }

    /** 删除By角色标识。 */
    @Override
    public void deleteByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>().eq(UserRoleRelDO::getRoleId, roleId));
    }

    /** 删除By作用域类型And作用域标识。 */
    @Override
    public void deleteByScopeTypeAndScopeId(String scopeType, Long scopeId) {
        if (scopeType == null || scopeId == null) {
            return;
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId));
    }

    /** 删除By用户标识And集团Scopes。 */
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

    /** 删除By用户标识And门店Scopes。 */
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
