package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreAdminRelMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class StoreAdminRelPersistenceRepository extends AbstractMpRepository<StoreAdminRelDO> {

    private final StoreAdminRelMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public StoreAdminRelPersistenceRepository(StoreAdminRelMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<StoreAdminRelDO> mapper() {
        return mapper;
    }
}

