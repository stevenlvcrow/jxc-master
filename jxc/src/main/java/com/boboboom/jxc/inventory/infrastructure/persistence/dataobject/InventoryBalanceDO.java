package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 库存数据对象，映射数据库表记录。 */
@TableName("inventory_balance")
public class InventoryBalanceDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String warehouseName;
    private String itemCode;
    private String itemName;
    private BigDecimal quantity;

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

    /** 获取Quantity。 */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /** 设置Quantity。 */
    public void setQuantity(BigDecimal quantityValue) {
        this.quantity = quantityValue;
    }
}

