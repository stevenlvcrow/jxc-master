package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreAdminRelMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StoreAdminRelPersistenceRepository extends AbstractMpRepository<StoreAdminRelDO> {

    private final StoreAdminRelMapper mapper;

    public StoreAdminRelPersistenceRepository(StoreAdminRelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<StoreAdminRelDO> mapper() {
        return mapper;
    }
}

