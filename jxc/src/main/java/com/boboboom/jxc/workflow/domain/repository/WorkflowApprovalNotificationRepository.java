package com.boboboom.jxc.workflow.domain.repository;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;

import java.util.List;

public interface WorkflowApprovalNotificationRepository {

    void save(WorkflowApprovalNotificationDO notification);

    void update(WorkflowApprovalNotificationDO notification);

    List<WorkflowApprovalNotificationDO> findByScopeOrdered(String scopeType, Long scopeId);

    WorkflowApprovalNotificationDO findLatestByScopeAndBusiness(String scopeType, Long scopeId, String businessCode, Long businessId);
}
