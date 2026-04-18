package com.boboboom.jxc.workflow.domain.repository;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;

import java.util.Optional;

public interface WorkflowProcessRegistryRepository {

    Optional<WorkflowProcessRegistryDO> findByScopeAndProcessCode(String scopeType, Long scopeId, String processCode);
}
