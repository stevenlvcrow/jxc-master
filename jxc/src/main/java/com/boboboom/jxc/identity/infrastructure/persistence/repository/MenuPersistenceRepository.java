package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MenuPersistenceRepository extends AbstractMpRepository<MenuDO> {

    private final MenuMapper mapper;

    public MenuPersistenceRepository(MenuMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<MenuDO> mapper() {
        return mapper;
    }
}

