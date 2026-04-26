package com.boboboom.jxc.identity.domain.repository;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface AuditLogRepository {

    void deleteByOperatorUserId(Long userId);
}
