package com.boboboom.jxc.workflow.domain.repository;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionConfigRepository {

    Optional<WorkflowDefinitionConfigDO> findById(Long id);

    Optional<WorkflowDefinitionConfigDO> findByScopeBusinessAndWorkflow(String scopeType,
                                                                        Long scopeId,
                                                                        String businessCode,
                                                                        String workflowCode);

    List<WorkflowDefinitionConfigDO> findByScopeBusinessAndWorkflowOrdered(String scopeType,
                                                                          Long scopeId,
                                                                          String businessCode,
                                                                          String workflowCode);

    List<WorkflowDefinitionConfigDO> findByScopeOrdered(String scopeType, Long scopeId, int limit);

    void save(WorkflowDefinitionConfigDO config);

    void update(WorkflowDefinitionConfigDO config);

    void deleteById(Long id);
}
