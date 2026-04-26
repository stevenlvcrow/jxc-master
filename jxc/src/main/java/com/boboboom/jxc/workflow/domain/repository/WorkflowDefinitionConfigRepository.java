package com.boboboom.jxc.workflow.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;

/** 审批流程仓储接口，定义领域需要的数据访问能力。 */
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

    List<WorkflowDefinitionConfigDO> findPublishedByScope(String scopeType, Long scopeId);

    void save(WorkflowDefinitionConfigDO config);

    void update(WorkflowDefinitionConfigDO config);

    void deleteById(Long id);

    void deleteByScopeBusinessAndWorkflow(String scopeType, Long scopeId, String businessCode, String workflowCode);
}
