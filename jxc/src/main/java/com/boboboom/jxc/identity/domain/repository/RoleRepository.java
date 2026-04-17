package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Optional<RoleDO> findById(Long id);

    List<RoleDO> findAll();

    Optional<RoleDO> findByRoleCode(String roleCode);

    List<RoleDO> findBuiltinTemplateRoles();

    Optional<RoleDO> findByTenantGroupIdAndRoleCode(Long tenantGroupId, String roleCode);

    void save(RoleDO role);

    void update(RoleDO role);
}
