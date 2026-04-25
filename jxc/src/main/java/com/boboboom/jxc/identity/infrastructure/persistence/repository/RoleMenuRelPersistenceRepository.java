package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMenuRelMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class RoleMenuRelPersistenceRepository extends AbstractMpRepository<RoleMenuRelDO> {

    private final RoleMenuRelMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public RoleMenuRelPersistenceRepository(RoleMenuRelMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<RoleMenuRelDO> mapper() {
        return mapper;
    }
}

