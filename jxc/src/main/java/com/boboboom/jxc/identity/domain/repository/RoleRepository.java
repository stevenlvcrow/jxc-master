package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface RoleRepository {

    Optional<RoleDO> findById(Long id);

    List<RoleDO> findAll();

    List<RoleDO> findByTenantGroupId(Long tenantGroupId);

    Optional<RoleDO> findByRoleCode(String roleCode);

    List<RoleDO> findBuiltinTemplateRoles(String enabledStatus);

    Optional<RoleDO> findByTenantGroupIdAndRoleCode(Long tenantGroupId, String roleCode);

    void save(RoleDO role);

    void update(RoleDO role);

    void deleteById(Long id);
}
