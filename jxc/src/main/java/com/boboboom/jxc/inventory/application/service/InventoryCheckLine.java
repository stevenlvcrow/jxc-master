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

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取HeaderId。 */
    public Long getHeaderId() {
        return headerId;
    }

    /** 设置HeaderId。 */
    public void setHeaderId(Long headerIdValue) {
        this.headerId = headerIdValue;
    }

    /** 获取ItemCode。 */
    public String getItemCode() {
        return itemCode;
    }

    /** 设置ItemCode。 */
    public void setItemCode(String itemCodeValue) {
        this.itemCode = itemCodeValue;
    }

    /** 获取ItemName。 */
    public String getItemName() {
        return itemName;
    }

    /** 设置ItemName。 */
    public void setItemName(String itemNameValue) {
        this.itemName = itemNameValue;
    }

    /** 获取Spec。 */
    public String getSpec() {
        return spec;
    }

    /** 设置Spec。 */
    public void setSpec(String specValue) {
        this.spec = specValue;
    }

    /** 获取Category。 */
    public String getCategory() {
        return category;
    }

    /** 设置Category。 */
    public void setCategory(String categoryValue) {
        this.category = categoryValue;
    }

    /** 获取UnitName。 */
    public String getUnitName() {
        return unitName;
    }

    /** 设置UnitName。 */
    public void setUnitName(String unitNameValue) {
        this.unitName = unitNameValue;
    }

    /** 获取AvailableQty。 */
    public BigDecimal getAvailableQty() {
        return availableQty;
    }

    /** 设置AvailableQty。 */
    public void setAvailableQty(BigDecimal availableQtyValue) {
        this.availableQty = availableQtyValue;
    }

    /** 获取BookQty。 */
    public BigDecimal getBookQty() {
        return bookQty;
    }

    /** 设置BookQty。 */
    public void setBookQty(BigDecimal bookQtyValue) {
        this.bookQty = bookQtyValue;
    }

    /** 获取ActualQty。 */
    public BigDecimal getActualQty() {
        return actualQty;
    }

    /** 设置ActualQty。 */
    public void setActualQty(BigDecimal actualQtyValue) {
        this.actualQty = actualQtyValue;
    }

    /** 获取BookPrice。 */
    public BigDecimal getBookPrice() {
        return bookPrice;
    }

    /** 设置BookPrice。 */
    public void setBookPrice(BigDecimal bookPriceValue) {
        this.bookPrice = bookPriceValue;
    }

    /** 获取BookAmount。 */
    public BigDecimal getBookAmount() {
        return bookAmount;
    }

    /** 设置BookAmount。 */
    public void setBookAmount(BigDecimal bookAmountValue) {
        this.bookAmount = bookAmountValue;
    }

    /** 获取ActualAmount。 */
    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    /** 设置ActualAmount。 */
    public void setActualAmount(BigDecimal actualAmountValue) {
        this.actualAmount = actualAmountValue;
    }

    /** 获取DiffQty。 */
    public BigDecimal getDiffQty() {
        return diffQty;
    }

    /** 设置DiffQty。 */
    public void setDiffQty(BigDecimal diffQtyValue) {
        this.diffQty = diffQtyValue;
    }

    /** 获取DiffAmount。 */
    public BigDecimal getDiffAmount() {
        return diffAmount;
    }

    /** 设置DiffAmount。 */
    public void setDiffAmount(BigDecimal diffAmountValue) {
        this.diffAmount = diffAmountValue;
    }

    /** 获取ProfitQty。 */
    public BigDecimal getProfitQty() {
        return profitQty;
    }

    /** 设置ProfitQty。 */
    public void setProfitQty(BigDecimal profitQtyValue) {
        this.profitQty = profitQtyValue;
    }

    /** 获取LossQty。 */
    public BigDecimal getLossQty() {
        return lossQty;
    }

    /** 设置LossQty。 */
    public void setLossQty(BigDecimal lossQtyValue) {
        this.lossQty = lossQtyValue;
    }

    /** 获取ProfitLossReason。 */
    public String getProfitLossReason() {
        return profitLossReason;
    }

    /** 设置ProfitLossReason。 */
    public void setProfitLossReason(String profitLossReasonValue) {
        this.profitLossReason = profitLossReasonValue;
    }

    /** 获取ProfitInboundPrice。 */
    public BigDecimal getProfitInboundPrice() {
        return profitInboundPrice;
    }

    /** 设置ProfitInboundPrice。 */
    public void setProfitInboundPrice(BigDecimal profitInboundPriceValue) {
        this.profitInboundPrice = profitInboundPriceValue;
    }

    /** 获取ProfitAmount。 */
    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    /** 设置ProfitAmount。 */
    public void setProfitAmount(BigDecimal profitAmountValue) {
        this.profitAmount = profitAmountValue;
    }

    /** 获取LossOutboundPrice。 */
    public BigDecimal getLossOutboundPrice() {
        return lossOutboundPrice;
    }

    /** 设置LossOutboundPrice。 */
    public void setLossOutboundPrice(BigDecimal lossOutboundPriceValue) {
        this.lossOutboundPrice = lossOutboundPriceValue;
    }

    /** 获取LossAmount。 */
    public BigDecimal getLossAmount() {
        return lossAmount;
    }

    /** 设置LossAmount。 */
    public void setLossAmount(BigDecimal lossAmountValue) {
        this.lossAmount = lossAmountValue;
    }

    /** 获取AbnormalFlag。 */
    public String getAbnormalFlag() {
        return abnormalFlag;
    }

    /** 设置AbnormalFlag。 */
    public void setAbnormalFlag(String abnormalFlagValue) {
        this.abnormalFlag = abnormalFlagValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }

    /** 获取ExtraJson。 */
    public String getExtraJson() {
        return extraJson;
    }

    /** 设置ExtraJson。 */
    public void setExtraJson(String extraJsonValue) {
        this.extraJson = extraJsonValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }
}
