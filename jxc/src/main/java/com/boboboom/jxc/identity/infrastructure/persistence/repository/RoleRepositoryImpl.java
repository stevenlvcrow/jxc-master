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

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String GROUP_ROLE_TEMPLATE_DESC = "GROUP_ROLE_TEMPLATE";

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
    public Optional<RoleDO> findByRoleCode(String roleCode) {
        return Optional.ofNullable(roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, roleCode)
                .last("limit 1")));
    }

    @Override
    public List<RoleDO> findBuiltinTemplateRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, 0L)
                .eq(RoleDO::getStatus, STATUS_ENABLED)
                .eq(RoleDO::getDescription, GROUP_ROLE_TEMPLATE_DESC)
                .in(RoleDO::getRoleType, List.of("GROUP", "STORE"))
                .orderByAsc(RoleDO::getId));
    }

    @Override
    public Optional<RoleDO> findByTenantGroupIdAndRoleCode(Long tenantGroupId, String roleCode) {
        return Optional.ofNullable(roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, tenantGroupId)
                .eq(RoleDO::getRoleCode, roleCode)
                .last("limit 1")));
    }

    @Override
    public void save(RoleDO role) {
        roleMapper.insert(role);
    }

    @Override
    public void update(RoleDO role) {
        roleMapper.updateById(role);
    }
}
