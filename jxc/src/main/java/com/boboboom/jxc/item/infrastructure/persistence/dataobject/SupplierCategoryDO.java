package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("supplier_category")
public class SupplierCategoryDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String categoryCode;
    private String categoryName;
    private String parentCategory;

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
}

