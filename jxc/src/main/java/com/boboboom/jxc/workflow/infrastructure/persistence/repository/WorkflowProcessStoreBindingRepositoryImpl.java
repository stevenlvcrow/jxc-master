package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessStoreBindingMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class WorkflowProcessStoreBindingRepositoryImpl implements WorkflowProcessStoreBindingRepository {

    private final WorkflowProcessStoreBindingMapper workflowProcessStoreBindingMapper;

    public WorkflowProcessStoreBindingRepositoryImpl(WorkflowProcessStoreBindingMapper workflowProcessStoreBindingMapper) {
        this.workflowProcessStoreBindingMapper = workflowProcessStoreBindingMapper;
    }

    @Override
    public void deleteByStoreId(Long storeId) {
        if (storeId == null) {
            return;
        }
        workflowProcessStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getStoreId, storeId));
    }

    @Override
    public List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryIds(Long groupId, List<Long> processRegistryIds) {
        if (groupId == null || processRegistryIds == null || processRegistryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return workflowProcessStoreBindingMapper.selectList(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .in(WorkflowProcessStoreBindingDO::getProcessRegistryId, processRegistryIds)
                .orderByAsc(WorkflowProcessStoreBindingDO::getStoreId));
    }

    @Override
    public List<WorkflowProcessStoreBindingDO> findByGroupAndProcessRegistryId(Long groupId, Long processRegistryId) {
        if (groupId == null || processRegistryId == null) {
            return List.of();
        }
        return workflowProcessStoreBindingMapper.selectList(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, processRegistryId)
                .orderByAsc(WorkflowProcessStoreBindingDO::getStoreId));
    }

    @Override
    public void save(WorkflowProcessStoreBindingDO binding) {
        workflowProcessStoreBindingMapper.insert(binding);
    }

    @Override
    public void deleteByGroupAndProcessRegistryId(Long groupId, Long processRegistryId) {
        if (groupId == null || processRegistryId == null) {
            return;
        }
        workflowProcessStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, processRegistryId));
    }
}
