package com.boboboom.jxc.workflow.domain.repository;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;

import java.util.List;
import java.util.Optional;

public interface WorkflowProcessRegistryRepository {

    Optional<WorkflowProcessRegistryDO> findById(Long id);

    Optional<WorkflowProcessRegistryDO> findByScopeAndProcessCode(String scopeType, Long scopeId, String processCode);

    List<WorkflowProcessRegistryDO> findByScopeOrdered(String scopeType, Long scopeId);

    void save(WorkflowProcessRegistryDO row);

    void update(WorkflowProcessRegistryDO row);

    void deleteById(Long id);
}
