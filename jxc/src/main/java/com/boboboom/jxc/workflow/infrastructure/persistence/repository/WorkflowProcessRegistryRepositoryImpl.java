package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessRegistryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WorkflowProcessRegistryRepositoryImpl implements WorkflowProcessRegistryRepository {

    private final WorkflowProcessRegistryMapper workflowProcessRegistryMapper;

    public WorkflowProcessRegistryRepositoryImpl(WorkflowProcessRegistryMapper workflowProcessRegistryMapper) {
        this.workflowProcessRegistryMapper = workflowProcessRegistryMapper;
    }

    @Override
    public Optional<WorkflowProcessRegistryDO> findByScopeAndProcessCode(String scopeType, Long scopeId, String processCode) {
        if (scopeType == null || scopeId == null || processCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowProcessRegistryMapper.selectOne(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, scopeType)
                .eq(WorkflowProcessRegistryDO::getScopeId, scopeId)
                .eq(WorkflowProcessRegistryDO::getProcessCode, processCode)
                .last("limit 1")));
    }
}
