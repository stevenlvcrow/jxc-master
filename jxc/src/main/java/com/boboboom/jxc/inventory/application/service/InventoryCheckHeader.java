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
    private String stocktakeFrequency;
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

    /** 获取CheckDate。 */
    public LocalDate getCheckDate() {
        return checkDate;
    }

    /** 设置CheckDate。 */
    public void setCheckDate(LocalDate checkDateValue) {
        this.checkDate = checkDateValue;
    }

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取CheckRangeType。 */
    public String getCheckRangeType() {
        return checkRangeType;
    }

    /** 设置CheckRangeType。 */
    public void setCheckRangeType(String checkRangeTypeValue) {
        this.checkRangeType = checkRangeTypeValue;
    }

    /** 获取StocktakeFrequency。 */
    public String getStocktakeFrequency() {
        return stocktakeFrequency;
    }

    /** 设置StocktakeFrequency。 */
    public void setStocktakeFrequency(String stocktakeFrequencyValue) {
        this.stocktakeFrequency = stocktakeFrequencyValue;
    }

    /** 获取FreezeStock。 */
    public Boolean getFreezeStock() {
        return freezeStock;
    }

    /** 设置FreezeStock。 */
    public void setFreezeStock(Boolean freezeStockValue) {
        this.freezeStock = freezeStockValue;
    }

    /** 获取CollaborativeFlag。 */
    public Boolean getCollaborativeFlag() {
        return collaborativeFlag;
    }

    /** 设置CollaborativeFlag。 */
    public void setCollaborativeFlag(Boolean collaborativeFlagValue) {
        this.collaborativeFlag = collaborativeFlagValue;
    }

    /** 获取PlanName。 */
    public String getPlanName() {
        return planName;
    }

    /** 设置PlanName。 */
    public void setPlanName(String planNameValue) {
        this.planName = planNameValue;
    }

    /** 获取ThirdPartyDocument。 */
    public String getThirdPartyDocument() {
        return thirdPartyDocument;
    }

    /** 设置ThirdPartyDocument。 */
    public void setThirdPartyDocument(String thirdPartyDocumentValue) {
        this.thirdPartyDocument = thirdPartyDocumentValue;
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

    /** 获取ItemCount。 */
    public Integer getItemCount() {
        return itemCount;
    }

    /** 设置ItemCount。 */
    public void setItemCount(Integer itemCountValue) {
        this.itemCount = itemCountValue;
    }

    /** 获取TotalBookAmount。 */
    public BigDecimal getTotalBookAmount() {
        return totalBookAmount;
    }

    /** 设置TotalBookAmount。 */
    public void setTotalBookAmount(BigDecimal totalBookAmountValue) {
        this.totalBookAmount = totalBookAmountValue;
    }

    /** 获取TotalActualAmount。 */
    public BigDecimal getTotalActualAmount() {
        return totalActualAmount;
    }

    /** 设置TotalActualAmount。 */
    public void setTotalActualAmount(BigDecimal totalActualAmountValue) {
        this.totalActualAmount = totalActualAmountValue;
    }

    /** 获取TotalDiffAmount。 */
    public BigDecimal getTotalDiffAmount() {
        return totalDiffAmount;
    }

    /** 设置TotalDiffAmount。 */
    public void setTotalDiffAmount(BigDecimal totalDiffAmountValue) {
        this.totalDiffAmount = totalDiffAmountValue;
    }

    /** 获取DiffStatus。 */
    public String getDiffStatus() {
        return diffStatus;
    }

    /** 设置DiffStatus。 */
    public void setDiffStatus(String diffStatusValue) {
        this.diffStatus = diffStatusValue;
    }

    /** 获取GeneratedStatus。 */
    public String getGeneratedStatus() {
        return generatedStatus;
    }

    /** 设置GeneratedStatus。 */
    public void setGeneratedStatus(String generatedStatusValue) {
        this.generatedStatus = generatedStatusValue;
    }

    /** 获取PrintStatus。 */
    public String getPrintStatus() {
        return printStatus;
    }

    /** 设置PrintStatus。 */
    public void setPrintStatus(String printStatusValue) {
        this.printStatus = printStatusValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
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
