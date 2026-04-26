package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowDefinitionConfigMapper;

/** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class WorkflowDefinitionConfigRepositoryImpl implements WorkflowDefinitionConfigRepository {

    private final WorkflowDefinitionConfigMapper configMapper;

    /** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
    public WorkflowDefinitionConfigRepositoryImpl(WorkflowDefinitionConfigMapper configMapperValue) {
        this.configMapper = configMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<WorkflowDefinitionConfigDO> findById(Long id) {
        return Optional.ofNullable(configMapper.selectById(id));
    }

    /** 查询By作用域业务And流程。 */
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

    /** 查询By作用域业务And流程排序。 */
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

    /** 查询By作用域排序。 */
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

    /** 查询已发布By作用域。 */
    @Override
    public List<WorkflowDefinitionConfigDO> findPublishedByScope(String scopeType, Long scopeId) {
        if (scopeType == null || scopeId == null) {
            return List.of();
        }
        return configMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scopeType)
                .eq(WorkflowDefinitionConfigDO::getScopeId, scopeId)
                .eq(WorkflowDefinitionConfigDO::getStatus, "PUBLISHED")
                .orderByDesc(WorkflowDefinitionConfigDO::getUpdatedAt)
                .orderByDesc(WorkflowDefinitionConfigDO::getId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(WorkflowDefinitionConfigDO config) {
        configMapper.insert(config);
    }

    /** 更新业务记录。 */
    @Override
    public void update(WorkflowDefinitionConfigDO config) {
        configMapper.updateById(config);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        configMapper.deleteById(id);
    }
}
