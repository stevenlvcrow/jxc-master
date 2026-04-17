package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;

import java.util.List;
import java.util.Optional;

public interface RoleMenuRelRepository {

    List<RoleMenuRelDO> findByRoleId(Long roleId);

    List<RoleMenuRelDO> findByRoleIds(List<Long> roleIds);

    Optional<RoleMenuRelDO> findByRoleIdAndMenuId(Long roleId, Long menuId);

    void deleteByRoleId(Long roleId);

    void save(RoleMenuRelDO rel);
}
