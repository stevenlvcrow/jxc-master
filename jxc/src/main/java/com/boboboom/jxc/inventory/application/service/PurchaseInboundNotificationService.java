package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.workflow.application.service.PurchaseInboundWorkflowService;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 采购入库通知记录协作服务。
 */
@Service
public class PurchaseInboundNotificationService {

    private static final String APPROVAL_ROUTE_PREFIX = "/inventory/1/2/view/";

    private final WorkflowApprovalNotificationApplicationService workflowApprovalNotificationApplicationService;
    private final PurchaseInboundWorkflowService purchaseInboundWorkflowService;

    public PurchaseInboundNotificationService(WorkflowApprovalNotificationApplicationService workflowApprovalNotificationApplicationService,
                                              PurchaseInboundWorkflowService purchaseInboundWorkflowService) {
        this.workflowApprovalNotificationApplicationService = workflowApprovalNotificationApplicationService;
        this.purchaseInboundWorkflowService = purchaseInboundWorkflowService;
    }

    /**
     * 记录采购入库发起后的待审核通知。
     *
     * @param scopeType 作用域类型
     * @param scopeId   作用域 ID
     * @param groupId   所属集团 ID
     * @param header    采购入库单
     */
    public void recordSubmit(String scopeType, Long scopeId, Long groupId, PurchaseInboundDO header) {
        WorkflowActionService.ApprovalTarget approvalTarget = purchaseInboundWorkflowService
                .resolveApprovalTarget(scopeType, scopeId, groupId, header.getWorkflowTaskName())
                .orElse(null);
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                purchaseInboundWorkflowService.businessCode(),
                purchaseInboundWorkflowService.resolveBusinessName(scopeType, scopeId, groupId),
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
                routePath(header)
        );
    }

    /**
     * 记录采购入库审批通过通知。
     *
     * @param scopeType    作用域类型
     * @param scopeId      作用域 ID
     * @param groupId      所属集团 ID
     * @param header       采购入库单
     * @param approverRole 审批角色
     * @param approvedAt   审批时间
     */
    public void recordApproved(String scopeType,
                               Long scopeId,
                               Long groupId,
                               PurchaseInboundDO header,
                               String approverRole,
                               LocalDateTime approvedAt) {
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                purchaseInboundWorkflowService.businessCode(),
                purchaseInboundWorkflowService.resolveBusinessName(scopeType, scopeId, groupId),
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
                routePath(header)
        );
    }

    /**
     * 记录采购入库审批拒绝通知。
     *
     * @param scopeType      作用域类型
     * @param scopeId        作用域 ID
     * @param groupId        所属集团 ID
     * @param header         采购入库单
     * @param approverRole   审批角色
     * @param rejectionReason 拒绝原因
     */
    public void recordRejected(String scopeType,
                               Long scopeId,
                               Long groupId,
                               PurchaseInboundDO header,
                               String approverRole,
                               String rejectionReason) {
        workflowApprovalNotificationApplicationService.record(
                scopeType,
                scopeId,
                purchaseInboundWorkflowService.businessCode(),
                purchaseInboundWorkflowService.resolveBusinessName(scopeType, scopeId, groupId),
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
                routePath(header)
        );
    }

    private String routePath(PurchaseInboundDO header) {
        return APPROVAL_ROUTE_PREFIX + header.getId();
    }
}
