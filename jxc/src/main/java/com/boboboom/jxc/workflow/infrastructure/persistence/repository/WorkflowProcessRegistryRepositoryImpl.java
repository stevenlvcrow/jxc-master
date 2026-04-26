package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessRegistryMapper;

/** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class WorkflowProcessRegistryRepositoryImpl implements WorkflowProcessRegistryRepository {

    private final WorkflowProcessRegistryMapper workflowProcessRegistryMapper;

    /** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
    public WorkflowProcessRegistryRepositoryImpl(WorkflowProcessRegistryMapper workflowProcessRegistryMapperValue) {
        this.workflowProcessRegistryMapper = workflowProcessRegistryMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<WorkflowProcessRegistryDO> findById(Long id) {
        return Optional.ofNullable(workflowProcessRegistryMapper.selectById(id));
    }

    /** 查询By作用域And流程编码。 */
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

    /** 查询By作用域排序。 */
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

    /** 保存业务数据。 */
    @Override
    public void save(WorkflowProcessRegistryDO row) {
        workflowProcessRegistryMapper.insert(row);
    }

    /** 更新业务记录。 */
    @Override
    public void update(WorkflowProcessRegistryDO row) {
        workflowProcessRegistryMapper.updateById(row);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        workflowProcessRegistryMapper.deleteById(id);
    }
}
