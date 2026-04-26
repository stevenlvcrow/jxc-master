package com.boboboom.jxc.workflow.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;

/** 审批流程仓储接口，定义领域需要的数据访问能力。 */
public interface WorkflowProcessRegistryRepository {

    Optional<WorkflowProcessRegistryDO> findById(Long id);

    Optional<WorkflowProcessRegistryDO> findByScopeAndProcessCode(String scopeType, Long scopeId, String processCode);

    List<WorkflowProcessRegistryDO> findByScopeOrdered(String scopeType, Long scopeId);

    void save(WorkflowProcessRegistryDO row);

    void update(WorkflowProcessRegistryDO row);

    void deleteById(Long id);
}
