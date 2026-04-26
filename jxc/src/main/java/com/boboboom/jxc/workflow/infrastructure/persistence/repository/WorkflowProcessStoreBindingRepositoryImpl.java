package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessStoreBindingMapper;

/** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class WorkflowProcessStoreBindingRepositoryImpl implements WorkflowProcessStoreBindingRepository {

    private final WorkflowProcessStoreBindingMapper workflowProcessStoreBindingMapper;

    /** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
    public WorkflowProcessStoreBindingRepositoryImpl(WorkflowProcessStoreBindingMapper workflowProcessStoreBindingMapperValue) {
        this.workflowProcessStoreBindingMapper = workflowProcessStoreBindingMapperValue;
    }

    /** 删除By门店标识。 */
    @Override
    public void deleteByStoreId(Long storeId) {
        if (storeId == null) {
            return;
        }
        workflowProcessStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getStoreId, storeId));
    }

    /** 查询By集团And流程注册记录标识。 */
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

    /** 查询By集团And流程注册记录标识。 */
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

    /** 保存业务数据。 */
    @Override
    public void save(WorkflowProcessStoreBindingDO binding) {
        workflowProcessStoreBindingMapper.insert(binding);
    }

    /** 删除By集团And流程注册记录标识。 */
    @Override
    public void deleteByGroupAndProcessRegistryId(Long groupId, Long processRegistryId) {
        if (groupId == null || processRegistryId == null) {
            return;
        }
        workflowProcessStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, processRegistryId));
    }

    /** 删除By集团And流程注册记录标识And门店标识。 */
    @Override
    public void deleteByGroupAndProcessRegistryIdAndStoreId(Long groupId, Long processRegistryId, Long storeId) {
        if (groupId == null || processRegistryId == null || storeId == null) {
            return;
        }
        workflowProcessStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, processRegistryId)
                .eq(WorkflowProcessStoreBindingDO::getStoreId, storeId));
    }
}
