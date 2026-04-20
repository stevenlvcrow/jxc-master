package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessRegistryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowProcessRegistryRepositoryImpl implements WorkflowProcessRegistryRepository {

    private final WorkflowProcessRegistryMapper workflowProcessRegistryMapper;

    public WorkflowProcessRegistryRepositoryImpl(WorkflowProcessRegistryMapper workflowProcessRegistryMapper) {
        this.workflowProcessRegistryMapper = workflowProcessRegistryMapper;
    }

    @Override
    public Optional<WorkflowProcessRegistryDO> findById(Long id) {
        return Optional.ofNullable(workflowProcessRegistryMapper.selectById(id));
    }

    @Override
    public Optional<WorkflowProcessRegistryDO> findByScopeAndProcessCode(String scopeType, Long scopeId, String processCode) {
        if (scopeType == null || scopeId == null || processCode == null) {
            return Optional.empty();
        }
        return workflowProcessRegistryMapper.selectList(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, scopeType)
                .eq(WorkflowProcessRegistryDO::getScopeId, scopeId)
                .eq(WorkflowProcessRegistryDO::getProcessCode, processCode)
                .orderByDesc(WorkflowProcessRegistryDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public List<WorkflowProcessRegistryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        if (scopeType == null || scopeId == null) {
            return List.of();
        }
        return workflowProcessRegistryMapper.selectList(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, scopeType)
                .eq(WorkflowProcessRegistryDO::getScopeId, scopeId)
                .orderByDesc(WorkflowProcessRegistryDO::getCreatedAt)
                .orderByDesc(WorkflowProcessRegistryDO::getId));
    }

    @Override
    public void save(WorkflowProcessRegistryDO row) {
        workflowProcessRegistryMapper.insert(row);
    }

    @Override
    public void update(WorkflowProcessRegistryDO row) {
        workflowProcessRegistryMapper.updateById(row);
    }

    @Override
    public void deleteById(Long id) {
        workflowProcessRegistryMapper.deleteById(id);
    }
}
