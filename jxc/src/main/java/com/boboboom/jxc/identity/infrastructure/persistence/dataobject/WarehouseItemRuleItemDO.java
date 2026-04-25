package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("warehouse_item_rule_item")
public class WarehouseItemRuleItemDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private String itemCode;

    private String itemName;

    private String specModel;

    private String itemCategory;

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

    /** 获取SpecModel。 */
    public String getSpecModel() {
        return specModel;
    }

    /** 设置SpecModel。 */
    public void setSpecModel(String specModelValue) {
        this.specModel = specModelValue;
    }

    /** 获取ItemCategory。 */
    public String getItemCategory() {
        return itemCategory;
    }

    /** 设置ItemCategory。 */
    public void setItemCategory(String itemCategoryValue) {
        this.itemCategory = itemCategoryValue;
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
