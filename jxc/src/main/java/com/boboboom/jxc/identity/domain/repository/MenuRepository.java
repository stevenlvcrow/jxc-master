package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;

import java.util.List;
import java.util.Optional;

public interface MenuRepository {

    Optional<MenuDO> findByMenuCode(String menuCode);

    List<MenuDO> findAllOrdered();

    List<MenuDO> findByIds(List<Long> ids);

    List<MenuPermissionView> findMenusByUserContext(Long userId, String scopeType, Long scopeId);

    void save(MenuDO menu);

    void update(MenuDO menu);
}
