package com.boboboom.jxc.identity.domain.repository;

public interface AuditLogRepository {

    void deleteByOperatorUserId(Long userId);
}
