package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通用库存单据明细记录。
 */
public class InventoryDocumentLine {

    private Long id;
    private Long headerId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String category;
    private String unitName;
    private BigDecimal availableQty;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String lineReason;
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

    /** 获取Quantity。 */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /** 设置Quantity。 */
    public void setQuantity(BigDecimal quantityValue) {
        this.quantity = quantityValue;
    }

    /** 获取UnitPrice。 */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** 设置UnitPrice。 */
    public void setUnitPrice(BigDecimal unitPriceValue) {
        this.unitPrice = unitPriceValue;
    }

    /** 获取Amount。 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** 设置Amount。 */
    public void setAmount(BigDecimal amountValue) {
        this.amount = amountValue;
    }

    /** 获取LineReason。 */
    public String getLineReason() {
        return lineReason;
    }

    /** 设置LineReason。 */
    public void setLineReason(String lineReasonValue) {
        this.lineReason = lineReasonValue;
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
