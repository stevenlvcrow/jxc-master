package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点单行信息。
 */
public class InventoryCheckLine {

    private Long id;
    private Long headerId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String category;
    private String unitName;
    private BigDecimal availableQty;
    private BigDecimal bookQty;
    private BigDecimal actualQty;
    private BigDecimal bookPrice;
    private BigDecimal bookAmount;
    private BigDecimal actualAmount;
    private BigDecimal diffQty;
    private BigDecimal diffAmount;
    private BigDecimal profitQty;
    private BigDecimal lossQty;
    private String profitLossReason;
    private BigDecimal profitInboundPrice;
    private BigDecimal profitAmount;
    private BigDecimal lossOutboundPrice;
    private BigDecimal lossAmount;
    private String abnormalFlag;
    private String remark;
    private String extraJson;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHeaderId() {
        return headerId;
    }

    public void setHeaderId(Long headerId) {
        this.headerId = headerId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(BigDecimal availableQty) {
        this.availableQty = availableQty;
    }

    public BigDecimal getBookQty() {
        return bookQty;
    }

    public void setBookQty(BigDecimal bookQty) {
        this.bookQty = bookQty;
    }

    public BigDecimal getActualQty() {
        return actualQty;
    }

    public void setActualQty(BigDecimal actualQty) {
        this.actualQty = actualQty;
    }

    public BigDecimal getBookPrice() {
        return bookPrice;
    }

    public void setBookPrice(BigDecimal bookPrice) {
        this.bookPrice = bookPrice;
    }

    public BigDecimal getBookAmount() {
        return bookAmount;
    }

    public void setBookAmount(BigDecimal bookAmount) {
        this.bookAmount = bookAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getDiffQty() {
        return diffQty;
    }

    public void setDiffQty(BigDecimal diffQty) {
        this.diffQty = diffQty;
    }

    public BigDecimal getDiffAmount() {
        return diffAmount;
    }

    public void setDiffAmount(BigDecimal diffAmount) {
        this.diffAmount = diffAmount;
    }

    public BigDecimal getProfitQty() {
        return profitQty;
    }

    public void setProfitQty(BigDecimal profitQty) {
        this.profitQty = profitQty;
    }

    public BigDecimal getLossQty() {
        return lossQty;
    }

    public void setLossQty(BigDecimal lossQty) {
        this.lossQty = lossQty;
    }

    public String getProfitLossReason() {
        return profitLossReason;
    }

    public void setProfitLossReason(String profitLossReason) {
        this.profitLossReason = profitLossReason;
    }

    public BigDecimal getProfitInboundPrice() {
        return profitInboundPrice;
    }

    public void setProfitInboundPrice(BigDecimal profitInboundPrice) {
        this.profitInboundPrice = profitInboundPrice;
    }

    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    public void setProfitAmount(BigDecimal profitAmount) {
        this.profitAmount = profitAmount;
    }

    public BigDecimal getLossOutboundPrice() {
        return lossOutboundPrice;
    }

    public void setLossOutboundPrice(BigDecimal lossOutboundPrice) {
        this.lossOutboundPrice = lossOutboundPrice;
    }

    public BigDecimal getLossAmount() {
        return lossAmount;
    }

    public void setLossAmount(BigDecimal lossAmount) {
        this.lossAmount = lossAmount;
    }

    public String getAbnormalFlag() {
        return abnormalFlag;
    }

    public void setAbnormalFlag(String abnormalFlag) {
        this.abnormalFlag = abnormalFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
