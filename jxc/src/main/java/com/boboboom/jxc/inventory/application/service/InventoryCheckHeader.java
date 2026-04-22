package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单头信息。
 */
public class InventoryCheckHeader {

    private Long id;
    private String scopeType;
    private Long scopeId;
    private String documentCode;
    private LocalDate checkDate;
    private String warehouseName;
    private String checkRangeType;
    private Boolean freezeStock;
    private Boolean collaborativeFlag;
    private String planName;
    private String thirdPartyDocument;
    private Long salesmanUserId;
    private String salesmanName;
    private Integer itemCount;
    private BigDecimal totalBookAmount;
    private BigDecimal totalActualAmount;
    private BigDecimal totalDiffAmount;
    private String diffStatus;
    private String generatedStatus;
    private String printStatus;
    private String status;
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

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getCheckRangeType() {
        return checkRangeType;
    }

    public void setCheckRangeType(String checkRangeType) {
        this.checkRangeType = checkRangeType;
    }

    public Boolean getFreezeStock() {
        return freezeStock;
    }

    public void setFreezeStock(Boolean freezeStock) {
        this.freezeStock = freezeStock;
    }

    public Boolean getCollaborativeFlag() {
        return collaborativeFlag;
    }

    public void setCollaborativeFlag(Boolean collaborativeFlag) {
        this.collaborativeFlag = collaborativeFlag;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getThirdPartyDocument() {
        return thirdPartyDocument;
    }

    public void setThirdPartyDocument(String thirdPartyDocument) {
        this.thirdPartyDocument = thirdPartyDocument;
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

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalBookAmount() {
        return totalBookAmount;
    }

    public void setTotalBookAmount(BigDecimal totalBookAmount) {
        this.totalBookAmount = totalBookAmount;
    }

    public BigDecimal getTotalActualAmount() {
        return totalActualAmount;
    }

    public void setTotalActualAmount(BigDecimal totalActualAmount) {
        this.totalActualAmount = totalActualAmount;
    }

    public BigDecimal getTotalDiffAmount() {
        return totalDiffAmount;
    }

    public void setTotalDiffAmount(BigDecimal totalDiffAmount) {
        this.totalDiffAmount = totalDiffAmount;
    }

    public String getDiffStatus() {
        return diffStatus;
    }

    public void setDiffStatus(String diffStatus) {
        this.diffStatus = diffStatus;
    }

    public String getGeneratedStatus() {
        return generatedStatus;
    }

    public void setGeneratedStatus(String generatedStatus) {
        this.generatedStatus = generatedStatus;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
