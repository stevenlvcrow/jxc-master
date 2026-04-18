package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通用库存单据通知记录服务。
 */
@Service
public class InventoryDocumentNotificationService {

    private final WorkflowApprovalNotificationApplicationService workflowApprovalNotificationApplicationService;
    private final InventoryDocumentWorkflowService inventoryDocumentWorkflowService;

    public InventoryDocumentNotificationService(WorkflowApprovalNotificationApplicationService workflowApprovalNotificationApplicationService,
                                                InventoryDocumentWorkflowService inventoryDocumentWorkflowService) {
        this.workflowApprovalNotificationApplicationService = workflowApprovalNotificationApplicationService;
        this.inventoryDocumentWorkflowService = inventoryDocumentWorkflowService;
    }

    /**
     * 记录提交待审通知。
     *
     * @param type      业务类型
     * @param scopeType 作用域类型
     * @param scopeId   作用域 ID
     * @param groupId   集团 ID
     * @param header    单据主表
     */
    public void recordSubmit(InventoryDocumentType type,
                             String scopeType,
                             Long scopeId,
                             Long groupId,
                             InventoryDocumentHeader header) {
        WorkflowActionService.ApprovalTarget approvalTarget = inventoryDocumentWorkflowService
                .resolveApprovalTarget(type, scopeType, scopeId, groupId, header.getWorkflowTaskName())
                .orElse(null);
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                header.getId(),
                header.getDocumentCode(),
                AuthContextHolder.userNameOr("system"),
                "发起人",
                approvalTarget == null ? null : approvalTarget.userId(),
                approvalTarget == null ? null : approvalTarget.roleCode(),
                approvalTarget == null ? null : approvalTarget.roleName(),
                LocalDateTime.now(),
                "待审核",
                "",
                type.getRouteViewPrefix() + header.getId()
        );
    }

    /**
     * 记录审核通过通知。
     *
     * @param type         业务类型
     * @param scopeType    作用域类型
     * @param scopeId      作用域 ID
     * @param header       单据主表
     * @param approverRole 审批角色
     * @param approvedAt   审批时间
     */
    public void recordApproved(InventoryDocumentType type,
                               String scopeType,
                               Long scopeId,
                               InventoryDocumentHeader header,
                               String approverRole,
                               LocalDateTime approvedAt) {
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                header.getId(),
                header.getDocumentCode(),
                AuthContextHolder.userNameOr("system"),
                approverRole,
                null,
                null,
                null,
                approvedAt,
                "通过",
                "",
                type.getRouteViewPrefix() + header.getId()
        );
    }

    /**
     * 记录审核拒绝通知。
     *
     * @param type            业务类型
     * @param scopeType       作用域类型
     * @param scopeId         作用域 ID
     * @param header          单据主表
     * @param approverRole    审批角色
     * @param rejectionReason 拒绝原因
     */
    public void recordRejected(InventoryDocumentType type,
                               String scopeType,
                               Long scopeId,
                               InventoryDocumentHeader header,
                               String approverRole,
                               String rejectionReason) {
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                header.getId(),
                header.getDocumentCode(),
                AuthContextHolder.userNameOr("system"),
                approverRole,
                null,
                null,
                null,
                LocalDateTime.now(),
                "拒绝",
                rejectionReason,
                type.getRouteViewPrefix() + header.getId()
        );
    }
}
