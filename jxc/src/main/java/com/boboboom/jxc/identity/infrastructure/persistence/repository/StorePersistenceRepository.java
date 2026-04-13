package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StorePersistenceRepository extends AbstractMpRepository<StoreDO> {

    private final StoreMapper mapper;

    public StorePersistenceRepository(StoreMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<StoreDO> mapper() {
        return mapper;
    }
}

