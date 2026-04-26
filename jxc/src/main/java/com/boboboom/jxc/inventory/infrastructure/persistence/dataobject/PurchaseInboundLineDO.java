package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseIdDO;

/** 库存数据对象，映射数据库表记录。 */
@TableName("inventory_purchase_inbound_line")
public class PurchaseInboundLineDO extends BaseIdDO {

    private Long inboundId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String category;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private String batchNo;
    private String manufacturer;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;

    /** 获取InboundId。 */
    public Long getInboundId() {
        return inboundId;
    }

    /** 设置InboundId。 */
    public void setInboundId(Long inboundIdValue) {
        this.inboundId = inboundIdValue;
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

    /** 获取TaxRate。 */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /** 设置TaxRate。 */
    public void setTaxRate(BigDecimal taxRateValue) {
        this.taxRate = taxRateValue;
    }

    /** 获取BatchNo。 */
    public String getBatchNo() {
        return batchNo;
    }

    /** 设置BatchNo。 */
    public void setBatchNo(String batchNoValue) {
        this.batchNo = batchNoValue;
    }

    /** 获取Manufacturer。 */
    public String getManufacturer() {
        return manufacturer;
    }

    /** 设置Manufacturer。 */
    public void setManufacturer(String manufacturerValue) {
        this.manufacturer = manufacturerValue;
    }

    /** 获取ProductionDate。 */
    public LocalDate getProductionDate() {
        return productionDate;
    }

    /** 设置ProductionDate。 */
    public void setProductionDate(LocalDate productionDateValue) {
        this.productionDate = productionDateValue;
    }

    /** 获取ExpiryDate。 */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /** 设置ExpiryDate。 */
    public void setExpiryDate(LocalDate expiryDateValue) {
        this.expiryDate = expiryDateValue;
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
