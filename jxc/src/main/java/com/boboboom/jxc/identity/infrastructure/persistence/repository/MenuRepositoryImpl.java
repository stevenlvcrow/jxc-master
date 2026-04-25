package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper menuMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public MenuRepositoryImpl(MenuMapper menuMapperValue) {
        this.menuMapper = menuMapperValue;
    }

    /** 查询By菜单编码。 */
    @Override
    public Optional<MenuDO> findByMenuCode(String menuCode) {
        List<MenuDO> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, menuCode));
        if (menus == null || menus.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(menus.get(0));
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<MenuDO> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(menuMapper.selectById(id));
    }

    /** 查询All排序。 */
    @Override
    public List<MenuDO> findAllOrdered() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getParentId)
                .orderByAsc(MenuDO::getSortNo)
                .orderByAsc(MenuDO::getId));
    }

    /** 查询By标识。 */
    @Override
    public List<MenuDO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return menuMapper.selectByIds(ids);
    }

    /** 查询ByParent标识。 */
    @Override
    public List<MenuDO> findByParentId(Long parentId) {
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSortNo)
                .orderByAsc(MenuDO::getId);
        if (parentId == null) {
            wrapper.isNull(MenuDO::getParentId);
        } else {
            wrapper.eq(MenuDO::getParentId, parentId);
        }
        return menuMapper.selectList(wrapper);
    }

    /** 查询菜单By用户Context。 */
    @Override
    public List<MenuPermissionView> findMenusByUserContext(Long userId, String scopeType, Long scopeId) {
        return menuMapper.selectMenusByUserContext(userId, scopeType, scopeId);
    }

    /** 保存业务数据。 */
    @Override
    public void save(MenuDO menu) {
        menuMapper.insert(menu);
    }

    /** 更新业务记录。 */
    @Override
    public void update(MenuDO menu) {
        menuMapper.updateById(menu);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        menuMapper.deleteById(id);
    }
}
