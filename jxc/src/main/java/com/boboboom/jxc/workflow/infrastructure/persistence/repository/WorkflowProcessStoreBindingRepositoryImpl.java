package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessStoreBindingMapper;
import org.springframework.stereotype.Repository;

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
}
