package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;

/**
 * 采购入库流程字段桥接工具。
 */
public final class PurchaseInboundWorkflowBridge {

    private PurchaseInboundWorkflowBridge() {
    }

    public static InventoryDocumentHeader toHeader(PurchaseInboundDO source) {
        InventoryDocumentHeader header = new InventoryDocumentHeader();
        header.setId(source.getId());
        header.setScopeType(source.getScopeType());
        header.setScopeId(source.getScopeId());
        header.setDocumentCode(source.getDocumentCode());
        header.setDocumentDate(source.getInboundDate());
        header.setPrimaryName(source.getWarehouseName());
        header.setCounterpartyName(source.getSupplierName());
        header.setSalesmanUserId(source.getSalesmanUserId());
        header.setSalesmanName(source.getSalesmanName());
        header.setUpstreamCode(source.getUpstreamCode());
        header.setStatus(source.getStatus());
        header.setWorkflowProcessCode(source.getWorkflowProcessCode());
        header.setWorkflowDefinitionKey(source.getWorkflowDefinitionKey());
        header.setWorkflowDefinitionId(source.getWorkflowDefinitionId());
        header.setWorkflowInstanceId(source.getWorkflowInstanceId());
        header.setWorkflowTaskId(source.getWorkflowTaskId());
        header.setWorkflowTaskName(source.getWorkflowTaskName());
        header.setWorkflowStatus(source.getWorkflowStatus());
        header.setPendingOperation(source.getPendingOperation());
        header.setRemark(source.getRemark());
        header.setRejectionReason(source.getRejectionReason());
        header.setCreatedBy(source.getCreatedBy());
        header.setApprovedBy(source.getApprovedBy());
        header.setApprovedAt(source.getApprovedAt());
        header.setCreatedAt(source.getCreatedAt());
        header.setUpdatedAt(source.getUpdatedAt());
        return header;
    }

    public static void applyHeader(PurchaseInboundDO target, InventoryDocumentHeader source) {
        target.setScopeType(source.getScopeType());
        target.setScopeId(source.getScopeId());
        target.setDocumentCode(source.getDocumentCode());
        target.setInboundDate(source.getDocumentDate());
        target.setWarehouseName(source.getPrimaryName());
        target.setSupplierName(source.getCounterpartyName());
        target.setSalesmanUserId(source.getSalesmanUserId());
        target.setSalesmanName(source.getSalesmanName());
        target.setUpstreamCode(source.getUpstreamCode());
        target.setStatus(source.getStatus());
        target.setWorkflowProcessCode(source.getWorkflowProcessCode());
        target.setWorkflowDefinitionKey(source.getWorkflowDefinitionKey());
        target.setWorkflowDefinitionId(source.getWorkflowDefinitionId());
        target.setWorkflowInstanceId(source.getWorkflowInstanceId());
        target.setWorkflowTaskId(source.getWorkflowTaskId());
        target.setWorkflowTaskName(source.getWorkflowTaskName());
        target.setWorkflowStatus(source.getWorkflowStatus());
        target.setPendingOperation(source.getPendingOperation());
        target.setRemark(source.getRemark());
        target.setRejectionReason(source.getRejectionReason());
        target.setCreatedBy(source.getCreatedBy());
        target.setApprovedBy(source.getApprovedBy());
        target.setApprovedAt(source.getApprovedAt());
    }
}
