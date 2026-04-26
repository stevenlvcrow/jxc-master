package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface MenuRepository {

    Optional<MenuDO> findByMenuCode(String menuCode);

    Optional<MenuDO> findById(Long id);

    List<MenuDO> findAllOrdered();

    List<MenuDO> findByIds(List<Long> ids);

    List<MenuDO> findByParentId(Long parentId);

    List<MenuPermissionView> findMenusByUserContext(Long userId, String scopeType, Long scopeId);

    void save(MenuDO menu);

    void update(MenuDO menu);

    void deleteById(Long id);
}
