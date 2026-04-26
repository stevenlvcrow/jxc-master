package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("warehouse_item_rule_category")
public class WarehouseItemRuleCategoryDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private String categoryCode;

    private String categoryName;

    private String parentCategory;

    private String childCategory;

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

    /** 获取CategoryCode。 */
    public String getCategoryCode() {
        return categoryCode;
    }

    /** 设置CategoryCode。 */
    public void setCategoryCode(String categoryCodeValue) {
        this.categoryCode = categoryCodeValue;
    }

    /** 获取CategoryName。 */
    public String getCategoryName() {
        return categoryName;
    }

    /** 设置CategoryName。 */
    public void setCategoryName(String categoryNameValue) {
        this.categoryName = categoryNameValue;
    }

    /** 获取ParentCategory。 */
    public String getParentCategory() {
        return parentCategory;
    }

    /** 设置ParentCategory。 */
    public void setParentCategory(String parentCategoryValue) {
        this.parentCategory = parentCategoryValue;
    }

    /** 获取ChildCategory。 */
    public String getChildCategory() {
        return childCategory;
    }

    /** 设置ChildCategory。 */
    public void setChildCategory(String childCategoryValue) {
        this.childCategory = childCategoryValue;
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
