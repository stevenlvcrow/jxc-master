package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通用库存单据主表记录。
 */
public class InventoryDocumentHeader {

    private Long id;
    private String scopeType;
    private Long scopeId;
    private String documentCode;
    private LocalDate documentDate;
    private String primaryName;
    private String secondaryName;
    private String counterpartyName;
    private String counterpartyName2;
    private String reason;
    private String upstreamCode;
    private Long salesmanUserId;
    private String salesmanName;
    private BigDecimal totalAmount;
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
    private String rejectionReason;
    private String extraJson;
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
    }

    public String getSecondaryName() {
        return secondaryName;
    }

    public void setSecondaryName(String secondaryName) {
        this.secondaryName = secondaryName;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getCounterpartyName2() {
        return counterpartyName2;
    }

    public void setCounterpartyName2(String counterpartyName2) {
        this.counterpartyName2 = counterpartyName2;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUpstreamCode() {
        return upstreamCode;
    }

    public void setUpstreamCode(String upstreamCode) {
        this.upstreamCode = upstreamCode;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
