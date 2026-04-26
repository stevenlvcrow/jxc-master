package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.AuditLogRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AuditLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AuditLogMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogMapper auditLogMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public AuditLogRepositoryImpl(AuditLogMapper auditLogMapperValue) {
        this.auditLogMapper = auditLogMapperValue;
    }

    /** 删除ByOperator用户标识。 */
    @Override
    public void deleteByOperatorUserId(Long userId) {
        if (userId == null) {
            return;
        }
        auditLogMapper.delete(new LambdaQueryWrapper<AuditLogDO>()
                .eq(AuditLogDO::getOperatorUserId, userId));
    }
}
