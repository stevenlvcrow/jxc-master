package com.boboboom.jxc.workflow.domain.repository;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;

import java.util.List;

public interface WorkflowProcessStoreBindingRepository {

    void deleteByStoreId(Long storeId);

    List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryIds(Long groupId, List<Long> processRegistryIds);

    List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryId(Long groupId, Long processRegistryId);

    void save(WorkflowProcessStoreBindingDO binding);

    void deleteByGroupAndProcessRegistryId(Long groupId, Long processRegistryId);
}
