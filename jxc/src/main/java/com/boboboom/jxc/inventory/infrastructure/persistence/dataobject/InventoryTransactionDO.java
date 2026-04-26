package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseCreateDO;

/** 库存数据对象，映射数据库表记录。 */
@TableName("inventory_transaction")
public class InventoryTransactionDO extends BaseCreateDO {

    private String scopeType;
    private Long scopeId;
    private String bizType;
    private Long bizId;
    private Long bizLineId;
    private String warehouseName;
    private String itemCode;
    private String itemName;
    private LocalDate businessDate;
    private BigDecimal quantityDelta;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private BigDecimal amountDelta;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private BigDecimal costPrice;
    private Long operatorId;

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

    /** 获取BizType。 */
    public String getBizType() {
        return bizType;
    }

    /** 设置BizType。 */
    public void setBizType(String bizTypeValue) {
        this.bizType = bizTypeValue;
    }

    /** 获取BizId。 */
    public Long getBizId() {
        return bizId;
    }

    /** 设置BizId。 */
    public void setBizId(Long bizIdValue) {
        this.bizId = bizIdValue;
    }

    /** 获取BizLineId。 */
    public Long getBizLineId() {
        return bizLineId;
    }

    /** 设置BizLineId。 */
    public void setBizLineId(Long bizLineIdValue) {
        this.bizLineId = bizLineIdValue;
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

    /** 获取BusinessDate。 */
    public LocalDate getBusinessDate() {
        return businessDate;
    }

    /** 设置BusinessDate。 */
    public void setBusinessDate(LocalDate businessDateValue) {
        this.businessDate = businessDateValue;
    }

    /** 获取QuantityDelta。 */
    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    /** 设置QuantityDelta。 */
    public void setQuantityDelta(BigDecimal quantityDeltaValue) {
        this.quantityDelta = quantityDeltaValue;
    }

    /** 获取BeforeQty。 */
    public BigDecimal getBeforeQty() {
        return beforeQty;
    }

    /** 设置BeforeQty。 */
    public void setBeforeQty(BigDecimal beforeQtyValue) {
        this.beforeQty = beforeQtyValue;
    }

    /** 获取AfterQty。 */
    public BigDecimal getAfterQty() {
        return afterQty;
    }

    /** 设置AfterQty。 */
    public void setAfterQty(BigDecimal afterQtyValue) {
        this.afterQty = afterQtyValue;
    }

    /** 获取AmountDelta。 */
    public BigDecimal getAmountDelta() {
        return amountDelta;
    }

    /** 设置AmountDelta。 */
    public void setAmountDelta(BigDecimal amountDeltaValue) {
        this.amountDelta = amountDeltaValue;
    }

    /** 获取BeforeAmount。 */
    public BigDecimal getBeforeAmount() {
        return beforeAmount;
    }

    /** 设置BeforeAmount。 */
    public void setBeforeAmount(BigDecimal beforeAmountValue) {
        this.beforeAmount = beforeAmountValue;
    }

    /** 获取AfterAmount。 */
    public BigDecimal getAfterAmount() {
        return afterAmount;
    }

    /** 设置AfterAmount。 */
    public void setAfterAmount(BigDecimal afterAmountValue) {
        this.afterAmount = afterAmountValue;
    }

    /** 获取CostPrice。 */
    public BigDecimal getCostPrice() {
        return costPrice;
    }

    /** 设置CostPrice。 */
    public void setCostPrice(BigDecimal costPriceValue) {
        this.costPrice = costPriceValue;
    }

    /** 获取OperatorId。 */
    public Long getOperatorId() {
        return operatorId;
    }

    /** 设置OperatorId。 */
    public void setOperatorId(Long operatorIdValue) {
        this.operatorId = operatorIdValue;
    }
}

