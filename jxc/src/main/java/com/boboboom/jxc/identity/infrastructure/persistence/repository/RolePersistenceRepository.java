package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class RolePersistenceRepository extends AbstractMpRepository<RoleDO> {

    private final RoleMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public RolePersistenceRepository(RoleMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<RoleDO> mapper() {
        return mapper;
    }
}

