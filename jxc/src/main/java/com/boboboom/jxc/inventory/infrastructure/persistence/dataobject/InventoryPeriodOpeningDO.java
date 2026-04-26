package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 周期期初库存主表数据对象。 */
@TableName("inventory_period_opening")
public class InventoryPeriodOpeningDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String documentCode;
    private String warehouseName;
    private String periodType;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private String sourceType;
    private String status;
    private String workflowProcessCode;
    private String workflowDefinitionKey;
    private String workflowDefinitionId;
    private String workflowInstanceId;
    private String workflowTaskId;
    private String workflowTaskName;
    private String workflowStatus;
    private String pendingOperation;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private String remark;
    private String rejectionReason;
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;

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

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取PeriodType。 */
    public String getPeriodType() {
        return periodType;
    }

    /** 设置PeriodType。 */
    public void setPeriodType(String periodTypeValue) {
        this.periodType = periodTypeValue;
    }

    /** 获取PeriodStartDate。 */
    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    /** 设置PeriodStartDate。 */
    public void setPeriodStartDate(LocalDate periodStartDateValue) {
        this.periodStartDate = periodStartDateValue;
    }

    /** 获取PeriodEndDate。 */
    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    /** 设置PeriodEndDate。 */
    public void setPeriodEndDate(LocalDate periodEndDateValue) {
        this.periodEndDate = periodEndDateValue;
    }

    /** 获取SourceType。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 设置SourceType。 */
    public void setSourceType(String sourceTypeValue) {
        this.sourceType = sourceTypeValue;
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

    /** 获取TotalQuantity。 */
    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    /** 设置TotalQuantity。 */
    public void setTotalQuantity(BigDecimal totalQuantityValue) {
        this.totalQuantity = totalQuantityValue;
    }

    /** 获取TotalAmount。 */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /** 设置TotalAmount。 */
    public void setTotalAmount(BigDecimal totalAmountValue) {
        this.totalAmount = totalAmountValue;
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
}
