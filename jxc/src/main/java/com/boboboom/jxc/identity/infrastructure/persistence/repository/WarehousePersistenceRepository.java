package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class WarehousePersistenceRepository extends AbstractMpRepository<WarehouseDO> {

    private final WarehouseMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public WarehousePersistenceRepository(WarehouseMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<WarehouseDO> mapper() {
        return mapper;
    }
}
