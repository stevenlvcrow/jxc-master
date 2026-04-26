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

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取ScopeType。 */
    public String getScopeType() {
        return scopeType;
    }

    /** 设置ScopeType。 */
    public void setScopeType(String scopeTypeValue) {
        this.scopeType = scopeTypeValue;
    }

    /** 获取ScopeId。 */
    public Long getScopeId() {
        return scopeId;
    }

    /** 设置ScopeId。 */
    public void setScopeId(Long scopeIdValue) {
        this.scopeId = scopeIdValue;
    }

    /** 获取DocumentCode。 */
    public String getDocumentCode() {
        return documentCode;
    }

    /** 设置DocumentCode。 */
    public void setDocumentCode(String documentCodeValue) {
        this.documentCode = documentCodeValue;
    }

    /** 获取DocumentDate。 */
    public LocalDate getDocumentDate() {
        return documentDate;
    }

    /** 设置DocumentDate。 */
    public void setDocumentDate(LocalDate documentDateValue) {
        this.documentDate = documentDateValue;
    }

    /** 获取PrimaryName。 */
    public String getPrimaryName() {
        return primaryName;
    }

    /** 设置PrimaryName。 */
    public void setPrimaryName(String primaryNameValue) {
        this.primaryName = primaryNameValue;
    }

    /** 获取SecondaryName。 */
    public String getSecondaryName() {
        return secondaryName;
    }

    /** 设置SecondaryName。 */
    public void setSecondaryName(String secondaryNameValue) {
        this.secondaryName = secondaryNameValue;
    }

    /** 获取CounterpartyName。 */
    public String getCounterpartyName() {
        return counterpartyName;
    }

    /** 设置CounterpartyName。 */
    public void setCounterpartyName(String counterpartyNameValue) {
        this.counterpartyName = counterpartyNameValue;
    }

    /** 获取CounterpartyName2。 */
    public String getCounterpartyName2() {
        return counterpartyName2;
    }

    /** 设置CounterpartyName2。 */
    public void setCounterpartyName2(String counterpartyName2Value) {
        this.counterpartyName2 = counterpartyName2Value;
    }

    /** 获取Reason。 */
    public String getReason() {
        return reason;
    }

    /** 设置Reason。 */
    public void setReason(String reasonValue) {
        this.reason = reasonValue;
    }

    /** 获取UpstreamCode。 */
    public String getUpstreamCode() {
        return upstreamCode;
    }

    /** 设置UpstreamCode。 */
    public void setUpstreamCode(String upstreamCodeValue) {
        this.upstreamCode = upstreamCodeValue;
    }

    /** 获取SalesmanUserId。 */
    public Long getSalesmanUserId() {
        return salesmanUserId;
    }

    /** 设置SalesmanUserId。 */
    public void setSalesmanUserId(Long salesmanUserIdValue) {
        this.salesmanUserId = salesmanUserIdValue;
    }

    /** 获取SalesmanName。 */
    public String getSalesmanName() {
        return salesmanName;
    }

    /** 设置SalesmanName。 */
    public void setSalesmanName(String salesmanNameValue) {
        this.salesmanName = salesmanNameValue;
    }

    /** 获取TotalAmount。 */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /** 设置TotalAmount。 */
    public void setTotalAmount(BigDecimal totalAmountValue) {
        this.totalAmount = totalAmountValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取WorkflowProcessCode。 */
    public String getWorkflowProcessCode() {
        return workflowProcessCode;
    }

    /** 设置WorkflowProcessCode。 */
    public void setWorkflowProcessCode(String workflowProcessCodeValue) {
        this.workflowProcessCode = workflowProcessCodeValue;
    }

    /** 获取WorkflowDefinitionKey。 */
    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    /** 设置WorkflowDefinitionKey。 */
    public void setWorkflowDefinitionKey(String workflowDefinitionKeyValue) {
        this.workflowDefinitionKey = workflowDefinitionKeyValue;
    }

    /** 获取WorkflowDefinitionId。 */
    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    /** 设置WorkflowDefinitionId。 */
    public void setWorkflowDefinitionId(String workflowDefinitionIdValue) {
        this.workflowDefinitionId = workflowDefinitionIdValue;
    }

    /** 获取WorkflowInstanceId。 */
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    /** 设置WorkflowInstanceId。 */
    public void setWorkflowInstanceId(String workflowInstanceIdValue) {
        this.workflowInstanceId = workflowInstanceIdValue;
    }

    /** 获取WorkflowTaskId。 */
    public String getWorkflowTaskId() {
        return workflowTaskId;
    }

    /** 设置WorkflowTaskId。 */
    public void setWorkflowTaskId(String workflowTaskIdValue) {
        this.workflowTaskId = workflowTaskIdValue;
    }

    /** 获取WorkflowTaskName。 */
    public String getWorkflowTaskName() {
        return workflowTaskName;
    }

    /** 设置WorkflowTaskName。 */
    public void setWorkflowTaskName(String workflowTaskNameValue) {
        this.workflowTaskName = workflowTaskNameValue;
    }

    /** 获取WorkflowStatus。 */
    public String getWorkflowStatus() {
        return workflowStatus;
    }

    /** 设置WorkflowStatus。 */
    public void setWorkflowStatus(String workflowStatusValue) {
        this.workflowStatus = workflowStatusValue;
    }

    /** 获取PendingOperation。 */
    public String getPendingOperation() {
        return pendingOperation;
    }

    /** 设置PendingOperation。 */
    public void setPendingOperation(String pendingOperationValue) {
        this.pendingOperation = pendingOperationValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }

    /** 获取RejectionReason。 */
    public String getRejectionReason() {
        return rejectionReason;
    }

    /** 设置RejectionReason。 */
    public void setRejectionReason(String rejectionReasonValue) {
        this.rejectionReason = rejectionReasonValue;
    }

    /** 获取ExtraJson。 */
    public String getExtraJson() {
        return extraJson;
    }

    /** 设置ExtraJson。 */
    public void setExtraJson(String extraJsonValue) {
        this.extraJson = extraJsonValue;
    }

    /** 获取CreatedBy。 */
    public Long getCreatedBy() {
        return createdBy;
    }

    /** 设置CreatedBy。 */
    public void setCreatedBy(Long createdByValue) {
        this.createdBy = createdByValue;
    }

    /** 获取ApprovedBy。 */
    public Long getApprovedBy() {
        return approvedBy;
    }

    /** 设置ApprovedBy。 */
    public void setApprovedBy(Long approvedByValue) {
        this.approvedBy = approvedByValue;
    }

    /** 获取ApprovedAt。 */
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    /** 设置ApprovedAt。 */
    public void setApprovedAt(LocalDateTime approvedAtValue) {
        this.approvedAt = approvedAtValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置UpdatedAt。 */
    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }
}
