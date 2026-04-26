package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface RoleMenuRelRepository {

    List<RoleMenuRelDO> findByRoleId(Long roleId);

    List<RoleMenuRelDO> findByRoleIds(List<Long> roleIds);

    Optional<RoleMenuRelDO> findByRoleIdAndMenuId(Long roleId, Long menuId);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIdAndMenuId(Long roleId, Long menuId);

    void deleteByMenuId(Long menuId);

    void save(RoleMenuRelDO rel);
}
