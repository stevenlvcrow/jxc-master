package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseCreateDO;

/** 周期期初库存明细数据对象。 */
@TableName("inventory_period_opening_line")
public class InventoryPeriodOpeningLineDO extends BaseCreateDO {

    private Long headerId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String category;
    private String unitName;
    private BigDecimal openingQty;
    private BigDecimal openingAmount;
    private BigDecimal openingAvgCost;
    private String remark;

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

    /** 获取OpeningQty。 */
    public BigDecimal getOpeningQty() {
        return openingQty;
    }

    /** 设置OpeningQty。 */
    public void setOpeningQty(BigDecimal openingQtyValue) {
        this.openingQty = openingQtyValue;
    }

    /** 获取OpeningAmount。 */
    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    /** 设置OpeningAmount。 */
    public void setOpeningAmount(BigDecimal openingAmountValue) {
        this.openingAmount = openingAmountValue;
    }

    /** 获取OpeningAvgCost。 */
    public BigDecimal getOpeningAvgCost() {
        return openingAvgCost;
    }

    /** 设置OpeningAvgCost。 */
    public void setOpeningAvgCost(BigDecimal openingAvgCostValue) {
        this.openingAvgCost = openingAvgCostValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }
}
