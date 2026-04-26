package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 库存批次余额数据对象，记录仓库、物品、批次维度的真实结存。 */
@TableName("inventory_batch_balance")
public class InventoryBatchBalanceDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String warehouseName;
    private String itemCode;
    private String itemName;
    private String batchNo;
    private String manufacturer;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private BigDecimal costAmount;
    private BigDecimal avgCost;

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

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
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

    /** 获取Quantity。 */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /** 设置Quantity。 */
    public void setQuantity(BigDecimal quantityValue) {
        this.quantity = quantityValue;
    }

    /** 获取CostAmount。 */
    public BigDecimal getCostAmount() {
        return costAmount;
    }

    /** 设置CostAmount。 */
    public void setCostAmount(BigDecimal costAmountValue) {
        this.costAmount = costAmountValue;
    }

    /** 获取AvgCost。 */
    public BigDecimal getAvgCost() {
        return avgCost;
    }

    /** 设置AvgCost。 */
    public void setAvgCost(BigDecimal avgCostValue) {
        this.avgCost = avgCostValue;
    }
}
