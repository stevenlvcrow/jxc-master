package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class StorePersistenceRepository extends AbstractMpRepository<StoreDO> {

    private final StoreMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public StorePersistenceRepository(StoreMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<StoreDO> mapper() {
        return mapper;
    }
}

