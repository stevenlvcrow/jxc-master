package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper menuMapper;

    public MenuRepositoryImpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public Optional<MenuDO> findByMenuCode(String menuCode) {
        return Optional.ofNullable(menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, menuCode)
                .last("limit 1")));
    }

    @Override
    public List<MenuDO> findAllOrdered() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSortNo)
                .orderByAsc(MenuDO::getId));
    }

    @Override
    public List<MenuDO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return menuMapper.selectBatchIds(ids);
    }

    @Override
    public List<MenuPermissionView> findMenusByUserContext(Long userId, String scopeType, Long scopeId) {
        return menuMapper.selectMenusByUserContext(userId, scopeType, scopeId);
    }

    @Override
    public void save(MenuDO menu) {
        menuMapper.insert(menu);
    }

    @Override
    public void update(MenuDO menu) {
        menuMapper.updateById(menu);
    }
}
