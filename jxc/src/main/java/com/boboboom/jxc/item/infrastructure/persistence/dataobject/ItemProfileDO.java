package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("item_profile")
public class ItemProfileDO extends BaseAuditDO {

    @TableField("scope_type")
    private String scopeType;
    @TableField("scope_id")
    private Long scopeId;
    @TableField("item_id")
    private String itemId;
    @TableField("item_code")
    private String itemCode;
    @TableField("detail_json")
    private String detailJson;
    @TableField("is_draft")
    private Boolean draft;

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

    /** 获取ItemId。 */
    public String getItemId() {
        return itemId;
    }

    /** 设置ItemId。 */
    public void setItemId(String itemIdValue) {
        this.itemId = itemIdValue;
    }

    /** 获取ItemCode。 */
    public String getItemCode() {
        return itemCode;
    }

    /** 设置ItemCode。 */
    public void setItemCode(String itemCodeValue) {
        this.itemCode = itemCodeValue;
    }

    /** 获取DetailJson。 */
    public String getDetailJson() {
        return detailJson;
    }

    /** 设置DetailJson。 */
    public void setDetailJson(String detailJsonValue) {
        this.detailJson = detailJsonValue;
    }

    /** 获取Draft。 */
    public Boolean getDraft() {
        return draft;
    }

    /** 设置Draft。 */
    public void setDraft(Boolean draftValue) {
        this.draft = draftValue;
    }
}
