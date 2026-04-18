package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowDefinitionConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowDefinitionConfigRepositoryImpl implements WorkflowDefinitionConfigRepository {

    private final WorkflowDefinitionConfigMapper configMapper;

    public WorkflowDefinitionConfigRepositoryImpl(WorkflowDefinitionConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public Optional<WorkflowDefinitionConfigDO> findById(Long id) {
        return Optional.ofNullable(configMapper.selectById(id));
    }

    @Override
    public Optional<WorkflowDefinitionConfigDO> findByScopeBusinessAndWorkflow(String scopeType,
                                                                                Long scopeId,
                                                                                String businessCode,
                                                                                String workflowCode) {
        return configMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scopeType)
                .eq(WorkflowDefinitionConfigDO::getScopeId, scopeId)
                .eq(WorkflowDefinitionConfigDO::getBusinessCode, businessCode)
                .eq(WorkflowDefinitionConfigDO::getWorkflowCode, workflowCode)
                .orderByDesc(WorkflowDefinitionConfigDO::getUpdatedAt)
                .orderByDesc(WorkflowDefinitionConfigDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public List<WorkflowDefinitionConfigDO> findByScopeBusinessAndWorkflowOrdered(String scopeType,
                                                                                  Long scopeId,
                                                                                  String businessCode,
                                                                                  String workflowCode) {
        return configMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scopeType)
                .eq(WorkflowDefinitionConfigDO::getScopeId, scopeId)
                .eq(WorkflowDefinitionConfigDO::getBusinessCode, businessCode)
                .eq(WorkflowDefinitionConfigDO::getWorkflowCode, workflowCode)
                .orderByDesc(WorkflowDefinitionConfigDO::getUpdatedAt)
                .orderByDesc(WorkflowDefinitionConfigDO::getId));
    }

    @Override
    public List<WorkflowDefinitionConfigDO> findByScopeOrdered(String scopeType, Long scopeId, int limit) {
        if (limit < 1) {
            return Collections.emptyList();
        }
        List<WorkflowDefinitionConfigDO> rows = configMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scopeType)
                .eq(WorkflowDefinitionConfigDO::getScopeId, scopeId)
                .orderByDesc(WorkflowDefinitionConfigDO::getUpdatedAt)
                .orderByDesc(WorkflowDefinitionConfigDO::getId));
        if (rows.size() <= limit) {
            return rows;
        }
        return rows.subList(0, limit);
    }

    @Override
    public void save(WorkflowDefinitionConfigDO config) {
        configMapper.insert(config);
    }

    @Override
    public void update(WorkflowDefinitionConfigDO config) {
        configMapper.updateById(config);
    }

    @Override
    public void deleteById(Long id) {
        configMapper.deleteById(id);
    }
}
