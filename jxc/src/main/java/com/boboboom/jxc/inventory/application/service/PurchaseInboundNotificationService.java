package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 采购入库通知记录协作服务。
 */
@Service
public class PurchaseInboundNotificationService {

    private final InventoryDocumentNotificationService inventoryDocumentNotificationService;

    public PurchaseInboundNotificationService(InventoryDocumentNotificationService inventoryDocumentNotificationService) {
        this.inventoryDocumentNotificationService = inventoryDocumentNotificationService;
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
        inventoryDocumentNotificationService.recordSubmit(
                InventoryDocumentType.PURCHASE_INBOUND,
                scopeType,
                scopeId,
                groupId,
                PurchaseInboundWorkflowBridge.toHeader(header)
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
        inventoryDocumentNotificationService.recordApproved(
                InventoryDocumentType.PURCHASE_INBOUND,
                scopeType,
                scopeId,
                PurchaseInboundWorkflowBridge.toHeader(header),
                approverRole,
                approvedAt
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
        inventoryDocumentNotificationService.recordRejected(
                InventoryDocumentType.PURCHASE_INBOUND,
                scopeType,
                scopeId,
                PurchaseInboundWorkflowBridge.toHeader(header),
                approverRole,
                rejectionReason
        );
    }
}
