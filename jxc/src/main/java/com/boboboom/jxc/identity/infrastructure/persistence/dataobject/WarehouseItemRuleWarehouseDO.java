package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("warehouse_item_rule_warehouse")
public class WarehouseItemRuleWarehouseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private Long warehouseId;

    private String warehouseName;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取RuleId。 */
    public Long getRuleId() {
        return ruleId;
    }

    /** 设置RuleId。 */
    public void setRuleId(Long ruleIdValue) {
        this.ruleId = ruleIdValue;
    }

    /** 获取WarehouseId。 */
    public Long getWarehouseId() {
        return warehouseId;
    }

    /** 设置WarehouseId。 */
    public void setWarehouseId(Long warehouseIdValue) {
        this.warehouseId = warehouseIdValue;
    }

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取SortOrder。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 设置SortOrder。 */
    public void setSortOrder(Integer sortOrderValue) {
        this.sortOrder = sortOrderValue;
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
