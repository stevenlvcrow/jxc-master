package com.boboboom.jxc.workflow.domain.repository;

import java.util.List;

import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;

/** 审批流程仓储接口，定义领域需要的数据访问能力。 */
public interface WorkflowApprovalNotificationRepository {

    void save(WorkflowApprovalNotificationDO notification);

    void update(WorkflowApprovalNotificationDO notification);

    List<WorkflowApprovalNotificationDO> findByScopeOrdered(String scopeType, Long scopeId);

    WorkflowApprovalNotificationDO findLatestByScopeAndBusiness(String scopeType, Long scopeId, String businessCode, Long businessId);
}
