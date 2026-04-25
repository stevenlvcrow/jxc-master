package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowApprovalNotificationRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowApprovalNotificationMapper;

/** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class WorkflowApprovalNotificationRepositoryImpl implements WorkflowApprovalNotificationRepository {

    private final WorkflowApprovalNotificationMapper notificationMapper;

    /** 审批流程仓储实现，负责通过持久层组件完成数据读写。 */
    public WorkflowApprovalNotificationRepositoryImpl(WorkflowApprovalNotificationMapper notificationMapperValue) {
        this.notificationMapper = notificationMapperValue;
    }

    /** 保存业务数据。 */
    @Override
    public void save(WorkflowApprovalNotificationDO notification) {
        notificationMapper.insert(notification);
    }

    /** 更新业务记录。 */
    @Override
    public void update(WorkflowApprovalNotificationDO notification) {
        notificationMapper.updateById(notification);
    }

    /** 查询By作用域排序。 */
    @Override
    public List<WorkflowApprovalNotificationDO> findByScopeOrdered(String scopeType, Long scopeId) {
        if (scopeType == null || scopeId == null) {
            return List.of();
        }
        return notificationMapper.selectList(new LambdaQueryWrapper<WorkflowApprovalNotificationDO>()
                .eq(WorkflowApprovalNotificationDO::getScopeType, scopeType)
                .eq(WorkflowApprovalNotificationDO::getScopeId, scopeId)
                .orderByDesc(WorkflowApprovalNotificationDO::getAuditedAt)
                .orderByDesc(WorkflowApprovalNotificationDO::getId));
    }

    /** 查询最新By作用域And业务。 */
    @Override
    public WorkflowApprovalNotificationDO findLatestByScopeAndBusiness(String scopeType, Long scopeId, String businessCode, Long businessId) {
        if (scopeType == null || scopeId == null || businessId == null) {
            return null;
        }
        return notificationMapper.selectList(new LambdaQueryWrapper<WorkflowApprovalNotificationDO>()
                .eq(WorkflowApprovalNotificationDO::getScopeType, scopeType)
                .eq(WorkflowApprovalNotificationDO::getScopeId, scopeId)
                .eq(WorkflowApprovalNotificationDO::getBusinessCode, businessCode)
                .eq(WorkflowApprovalNotificationDO::getBusinessId, businessId)
                .orderByDesc(WorkflowApprovalNotificationDO::getAuditedAt)
                .orderByDesc(WorkflowApprovalNotificationDO::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
