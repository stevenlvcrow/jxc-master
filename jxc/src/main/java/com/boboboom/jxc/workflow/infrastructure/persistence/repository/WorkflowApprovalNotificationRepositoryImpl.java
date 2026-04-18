package com.boboboom.jxc.workflow.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.workflow.domain.repository.WorkflowApprovalNotificationRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowApprovalNotificationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkflowApprovalNotificationRepositoryImpl implements WorkflowApprovalNotificationRepository {

    private final WorkflowApprovalNotificationMapper notificationMapper;

    public WorkflowApprovalNotificationRepositoryImpl(WorkflowApprovalNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void save(WorkflowApprovalNotificationDO notification) {
        notificationMapper.insert(notification);
    }

    @Override
    public void update(WorkflowApprovalNotificationDO notification) {
        notificationMapper.updateById(notification);
    }

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
