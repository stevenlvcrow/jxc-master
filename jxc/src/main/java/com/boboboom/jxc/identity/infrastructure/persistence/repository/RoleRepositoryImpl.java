package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<RoleDO> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectById(id));
    }

    @Override
    public List<RoleDO> findAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .orderByDesc(RoleDO::getCreatedAt)
                .orderByDesc(RoleDO::getId));
    }

    @Override
    public List<RoleDO> findByTenantGroupId(Long tenantGroupId) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, tenantGroupId)
                .orderByDesc(RoleDO::getCreatedAt)
                .orderByDesc(RoleDO::getId));
    }

    @Override
    public Optional<RoleDO> findByRoleCode(String roleCode) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, roleCode)
                .orderByDesc(RoleDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public List<RoleDO> findBuiltinTemplateRoles(String enabledStatus) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, 0L)
                .eq(RoleDO::getBuiltin, Boolean.TRUE)
                .eq(RoleDO::getStatus, enabledStatus)
                .in(RoleDO::getRoleType, List.of("GROUP", "STORE"))
                .orderByAsc(RoleDO::getId));
    }

    @Override
    public Optional<RoleDO> findByTenantGroupIdAndRoleCode(Long tenantGroupId, String roleCode) {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, tenantGroupId)
                .eq(RoleDO::getRoleCode, roleCode)
                .orderByDesc(RoleDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void save(RoleDO role) {
        roleMapper.insert(role);
    }

    @Override
    public void update(RoleDO role) {
        roleMapper.updateById(role);
    }

    @Override
    public void deleteById(Long id) {
        roleMapper.deleteById(id);
    }
}
