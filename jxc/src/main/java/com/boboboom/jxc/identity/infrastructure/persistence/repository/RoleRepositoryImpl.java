package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public RoleRepositoryImpl(RoleMapper roleMapperValue) {
        this.roleMapper = roleMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<RoleDO> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectById(id));
    }

    /** 查询全部记录。 */
    @Override
    public List<RoleDO> findAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .orderByDesc(RoleDO::getCreatedAt)
                .orderByDesc(RoleDO::getId));
    }

    /** 查询ByTenant集团标识。 */
    @Override
    public List<RoleDO> findByTenantGroupId(Long tenantGroupId) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, tenantGroupId)
                .orderByDesc(RoleDO::getCreatedAt)
                .orderByDesc(RoleDO::getId));
    }

    /** 查询By角色编码。 */
    @Override
    public Optional<RoleDO> findByRoleCode(String roleCode) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, roleCode)
                .orderByDesc(RoleDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询Builtin模板角色。 */
    @Override
    public List<RoleDO> findBuiltinTemplateRoles(String enabledStatus) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, 0L)
                .eq(RoleDO::getBuiltin, Boolean.TRUE)
                .eq(RoleDO::getStatus, enabledStatus)
                .in(RoleDO::getRoleType, List.of("GROUP", "STORE"))
                .orderByAsc(RoleDO::getId));
    }

    /** 查询ByTenant集团标识And角色编码。 */
    @Override
    public Optional<RoleDO> findByTenantGroupIdAndRoleCode(Long tenantGroupId, String roleCode) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, tenantGroupId)
                .eq(RoleDO::getRoleCode, roleCode)
                .orderByDesc(RoleDO::getId))
                .stream()
                .findFirst();
    }

    /** 保存业务数据。 */
    @Override
    public void save(RoleDO role) {
        roleMapper.insert(role);
    }

    /** 更新业务记录。 */
    @Override
    public void update(RoleDO role) {
        roleMapper.updateById(role);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        roleMapper.deleteById(id);
    }
}
