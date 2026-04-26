package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMenuRelMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class RoleMenuRelRepositoryImpl implements RoleMenuRelRepository {

    private final RoleMenuRelMapper roleMenuRelMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public RoleMenuRelRepositoryImpl(RoleMenuRelMapper roleMenuRelMapperValue) {
        this.roleMenuRelMapper = roleMenuRelMapperValue;
    }

    /** 查询By角色标识。 */
    @Override
    public List<RoleMenuRelDO> findByRoleId(Long roleId) {
        return roleMenuRelMapper.selectByRoleId(roleId);
    }

    /** 查询By角色标识。 */
    @Override
    public List<RoleMenuRelDO> findByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMenuRelMapper.selectByRoleIds(roleIds);
    }

    /** 查询By角色标识And菜单标识。 */
    @Override
    public Optional<RoleMenuRelDO> findByRoleIdAndMenuId(Long roleId, Long menuId) {
        return roleMenuRelMapper.selectList(new LambdaQueryWrapper<RoleMenuRelDO>()
                .eq(RoleMenuRelDO::getRoleId, roleId)
                .eq(RoleMenuRelDO::getMenuId, menuId)
                .orderByDesc(RoleMenuRelDO::getId))
                .stream()
                .findFirst();
    }

    /** 删除By角色标识。 */
    @Override
    public void deleteByRoleId(Long roleId) {
        roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRelDO>().eq(RoleMenuRelDO::getRoleId, roleId));
    }

    /** 删除By角色标识And菜单标识。 */
    @Override
    public void deleteByRoleIdAndMenuId(Long roleId, Long menuId) {
        if (roleId == null || menuId == null) {
            return;
        }
        roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRelDO>()
                .eq(RoleMenuRelDO::getRoleId, roleId)
                .eq(RoleMenuRelDO::getMenuId, menuId));
    }

    /** 删除By菜单标识。 */
    @Override
    public void deleteByMenuId(Long menuId) {
        if (menuId == null) {
            return;
        }
        roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRelDO>()
                .eq(RoleMenuRelDO::getMenuId, menuId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(RoleMenuRelDO rel) {
        roleMenuRelMapper.insert(rel);
    }
}
