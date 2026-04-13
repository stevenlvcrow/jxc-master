package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AuditLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AuditLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogPersistenceRepository extends AbstractMpRepository<AuditLogDO> {

    private final AuditLogMapper mapper;

    public AuditLogPersistenceRepository(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<AuditLogDO> mapper() {
        return mapper;
    }
}

