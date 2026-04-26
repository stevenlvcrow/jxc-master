package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("dish_profile")
public class DishProfileDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String dishId;
    private String spuCode;
    private String dishName;
    private String spec;
    private Long categoryId;
    private String dishType;
    private String deleted;
    private String linkedCostCard;

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

    /** 获取DishId。 */
    public String getDishId() {
        return dishId;
    }

    /** 设置DishId。 */
    public void setDishId(String dishIdValue) {
        this.dishId = dishIdValue;
    }

    /** 获取SpuCode。 */
    public String getSpuCode() {
        return spuCode;
    }

    /** 设置SpuCode。 */
    public void setSpuCode(String spuCodeValue) {
        this.spuCode = spuCodeValue;
    }

    /** 获取DishName。 */
    public String getDishName() {
        return dishName;
    }

    /** 设置DishName。 */
    public void setDishName(String dishNameValue) {
        this.dishName = dishNameValue;
    }

    /** 获取Spec。 */
    public String getSpec() {
        return spec;
    }

    /** 设置Spec。 */
    public void setSpec(String specValue) {
        this.spec = specValue;
    }

    /** 获取CategoryId。 */
    public Long getCategoryId() {
        return categoryId;
    }

    /** 设置CategoryId。 */
    public void setCategoryId(Long categoryIdValue) {
        this.categoryId = categoryIdValue;
    }

    /** 获取DishType。 */
    public String getDishType() {
        return dishType;
    }

    /** 设置DishType。 */
    public void setDishType(String dishTypeValue) {
        this.dishType = dishTypeValue;
    }

    /** 获取Deleted。 */
    public String getDeleted() {
        return deleted;
    }

    /** 设置Deleted。 */
    public void setDeleted(String deletedValue) {
        this.deleted = deletedValue;
    }

    /** 获取LinkedCostCard。 */
    public String getLinkedCostCard() {
        return linkedCostCard;
    }

    /** 设置LinkedCostCard。 */
    public void setLinkedCostCard(String linkedCostCardValue) {
        this.linkedCostCard = linkedCostCardValue;
    }
}
