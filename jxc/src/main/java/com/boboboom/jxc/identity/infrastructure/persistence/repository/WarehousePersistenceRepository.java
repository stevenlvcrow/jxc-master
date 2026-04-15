package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WarehousePersistenceRepository extends AbstractMpRepository<WarehouseDO> {

    private final WarehouseMapper mapper;

    public WarehousePersistenceRepository(WarehouseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<WarehouseDO> mapper() {
        return mapper;
    }
}
