package com.boboboom.jxc.workflow.domain.repository;

import java.util.List;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;

/** 审批流程仓储接口，定义领域需要的数据访问能力。 */
public interface WorkflowProcessStoreBindingRepository {

    void deleteByStoreId(Long storeId);

    List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryIds(Long groupId, List<Long> processRegistryIds);

    List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryId(Long groupId, Long processRegistryId);

    void save(WorkflowProcessStoreBindingDO binding);

    void deleteByGroupAndProcessRegistryId(Long groupId, Long processRegistryId);

    void deleteByGroupAndProcessRegistryIdAndStoreId(Long groupId, Long processRegistryId, Long storeId);
}
