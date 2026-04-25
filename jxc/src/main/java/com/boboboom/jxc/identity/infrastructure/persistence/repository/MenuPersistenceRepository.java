package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class MenuPersistenceRepository extends AbstractMpRepository<MenuDO> {

    private final MenuMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public MenuPersistenceRepository(MenuMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<MenuDO> mapper() {
        return mapper;
    }
}

