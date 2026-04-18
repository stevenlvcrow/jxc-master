package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("dev.inventory_purchase_inbound")
public class PurchaseInboundDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String documentCode;
    private LocalDate inboundDate;
    private String warehouseName;
    private String supplierName;
    private Long salesmanUserId;
    private String salesmanName;
    private String upstreamCode;
    private String status;
    private String workflowProcessCode;
    private String workflowDefinitionKey;
    private String workflowDefinitionId;
    private String workflowInstanceId;
    private String workflowTaskId;
    private String workflowTaskName;
    private String workflowStatus;
    private String pendingOperation;
    private String remark;
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;
    }

    public LocalDate getInboundDate() {
        return inboundDate;
    }

    public void setInboundDate(LocalDate inboundDate) {
        this.inboundDate = inboundDate;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getSalesmanUserId() {
        return salesmanUserId;
    }

    public void setSalesmanUserId(Long salesmanUserId) {
        this.salesmanUserId = salesmanUserId;
    }

    public String getSalesmanName() {
        return salesmanName;
    }

    public void setSalesmanName(String salesmanName) {
        this.salesmanName = salesmanName;
    }

    public String getUpstreamCode() {
        return upstreamCode;
    }

    public void setUpstreamCode(String upstreamCode) {
        this.upstreamCode = upstreamCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkflowProcessCode() {
        return workflowProcessCode;
    }

    public void setWorkflowProcessCode(String workflowProcessCode) {
        this.workflowProcessCode = workflowProcessCode;
    }

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getWorkflowTaskId() {
        return workflowTaskId;
    }

    public void setWorkflowTaskId(String workflowTaskId) {
        this.workflowTaskId = workflowTaskId;
    }

    public String getWorkflowTaskName() {
        return workflowTaskName;
    }

    public void setWorkflowTaskName(String workflowTaskName) {
        this.workflowTaskName = workflowTaskName;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getPendingOperation() {
        return pendingOperation;
    }

    public void setPendingOperation(String pendingOperation) {
        this.pendingOperation = pendingOperation;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
