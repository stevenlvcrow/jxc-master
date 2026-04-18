package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.boboboom.jxc.workflow.application.service.PurchaseInboundWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 采购入库反审核编排服务。
 */
@Service
public class PurchaseInboundUnapproveService {

    private static final String STATUS_APPROVED = "已审核";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String PENDING_OPERATION_DELETE = "DELETE";
    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final String INVENTORY_BIZ_TYPE_UNAPPROVE = "PURCHASE_INBOUND_UNAPPROVE";

    private final PurchaseInboundWorkflowService purchaseInboundWorkflowService;
    private final PurchaseInboundNotificationService purchaseInboundNotificationService;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final PurchaseInboundRepository purchaseInboundRepository;

    public PurchaseInboundUnapproveService(PurchaseInboundWorkflowService purchaseInboundWorkflowService,
                                           PurchaseInboundNotificationService purchaseInboundNotificationService,
                                           InventoryStockMutationService inventoryStockMutationService,
                                           PurchaseInboundRepository purchaseInboundRepository) {
        this.purchaseInboundWorkflowService = purchaseInboundWorkflowService;
        this.purchaseInboundNotificationService = purchaseInboundNotificationService;
        this.inventoryStockMutationService = inventoryStockMutationService;
        this.purchaseInboundRepository = purchaseInboundRepository;
    }

    /**
     * 执行单张采购入库单的反审核。
     *
     * @param scopeType       作用域类型
     * @param scopeId         作用域 ID
     * @param groupId         所属集团 ID
     * @param header          采购入库单
     * @param lines           采购入库明细
     * @param operatorId      操作人 ID
     * @param rejectionReason 拒审原因
     */
    public void execute(String scopeType,
                        Long scopeId,
                        Long groupId,
                        PurchaseInboundDO header,
                        List<PurchaseInboundLineDO> lines,
                        Long operatorId,
                        String rejectionReason) {
        if (!Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            resetDeletePendingIfNecessary(header);
            return;
        }
        String approverRole = purchaseInboundWorkflowService.resolveApprovalRoleLabel(
                scopeType,
                scopeId,
                groupId,
                operatorId,
                header.getWorkflowTaskName()
        );
        rollbackInventory(scopeType, scopeId, header, lines, operatorId);
        markHeaderSubmitted(header, rejectionReason);
        persistAfterUnapprove(header);
        purchaseInboundNotificationService.recordRejected(
                scopeType,
                scopeId,
                groupId,
                header,
                approverRole,
                rejectionReason
        );
    }

    private void resetDeletePendingIfNecessary(PurchaseInboundDO header) {
        if (!PENDING_OPERATION_DELETE.equals(header.getPendingOperation())) {
            return;
        }
        header.setPendingOperation(PENDING_OPERATION_NONE);
        persistAfterUnapprove(header);
    }

    private void rollbackInventory(String scopeType,
                                   Long scopeId,
                                   PurchaseInboundDO header,
                                   List<PurchaseInboundLineDO> lines,
                                   Long operatorId) {
        for (PurchaseInboundLineDO line : lines) {
            inventoryStockMutationService.applyDelta(
                    scopeType,
                    scopeId,
                    header.getWarehouseName(),
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getQuantity().negate(),
                    INVENTORY_BIZ_TYPE_UNAPPROVE,
                    operatorId
            );
        }
    }

    private void markHeaderSubmitted(PurchaseInboundDO header, String rejectionReason) {
        header.setStatus(STATUS_SUBMITTED);
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        header.setRejectionReason(rejectionReason);
        header.setPendingOperation(PENDING_OPERATION_NONE);
    }

    private void persistAfterUnapprove(PurchaseInboundDO header) {
        if (hasWorkflowMetadata(header)) {
            purchaseInboundWorkflowService.resetWorkflowState(header);
            return;
        }
        purchaseInboundRepository.update(header);
    }

    private boolean hasWorkflowMetadata(PurchaseInboundDO header) {
        return StringUtils.hasText(header.getWorkflowProcessCode())
                || StringUtils.hasText(header.getWorkflowDefinitionKey())
                || StringUtils.hasText(header.getWorkflowDefinitionId())
                || StringUtils.hasText(header.getWorkflowInstanceId())
                || StringUtils.hasText(header.getWorkflowTaskId())
                || StringUtils.hasText(header.getWorkflowTaskName())
                || (StringUtils.hasText(header.getWorkflowStatus()) && !"NONE".equals(header.getWorkflowStatus()));
    }
}
