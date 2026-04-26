package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 库存数据对象，映射数据库表记录。 */
@TableName("inventory_purchase_inbound")
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

    /** 获取InboundDate。 */
    public LocalDate getInboundDate() {
        return inboundDate;
    }

    /** 设置InboundDate。 */
    public void setInboundDate(LocalDate inboundDateValue) {
        this.inboundDate = inboundDateValue;
    }

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取SupplierName。 */
    public String getSupplierName() {
        return supplierName;
    }

    /** 设置SupplierName。 */
    public void setSupplierName(String supplierNameValue) {
        this.supplierName = supplierNameValue;
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

    /** 获取UpstreamCode。 */
    public String getUpstreamCode() {
        return upstreamCode;
    }

    /** 设置UpstreamCode。 */
    public void setUpstreamCode(String upstreamCodeValue) {
        this.upstreamCode = upstreamCodeValue;
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
