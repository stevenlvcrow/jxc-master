package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMenuRelMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleMenuRelRepositoryImpl implements RoleMenuRelRepository {

    private final RoleMenuRelMapper roleMenuRelMapper;

    public RoleMenuRelRepositoryImpl(RoleMenuRelMapper roleMenuRelMapper) {
        this.roleMenuRelMapper = roleMenuRelMapper;
    }

    @Override
    public List<RoleMenuRelDO> findByRoleId(Long roleId) {
        return roleMenuRelMapper.selectByRoleId(roleId);
    }

    @Override
    public List<RoleMenuRelDO> findByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMenuRelMapper.selectByRoleIds(roleIds);
    }

    @Override
    public Optional<RoleMenuRelDO> findByRoleIdAndMenuId(Long roleId, Long menuId) {
        return Optional.ofNullable(roleMenuRelMapper.selectOne(new LambdaQueryWrapper<RoleMenuRelDO>()
                .eq(RoleMenuRelDO::getRoleId, roleId)
                .eq(RoleMenuRelDO::getMenuId, menuId)
                .last("limit 1")));
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRelDO>().eq(RoleMenuRelDO::getRoleId, roleId));
    }

    @Override
    public void save(RoleMenuRelDO rel) {
        roleMenuRelMapper.insert(rel);
    }
}
