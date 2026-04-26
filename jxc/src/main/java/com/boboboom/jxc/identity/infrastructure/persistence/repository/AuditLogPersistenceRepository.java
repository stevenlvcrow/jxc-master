package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AuditLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AuditLogMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class AuditLogPersistenceRepository extends AbstractMpRepository<AuditLogDO> {

    private final AuditLogMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public AuditLogPersistenceRepository(AuditLogMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<AuditLogDO> mapper() {
        return mapper;
    }
}

