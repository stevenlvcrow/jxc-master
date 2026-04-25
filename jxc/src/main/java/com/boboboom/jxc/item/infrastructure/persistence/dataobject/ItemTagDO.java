package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("item_tag")
public class ItemTagDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String tagCode;
    private String tagName;
    private String status;
    private String remark;

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

    /** 获取TagCode。 */
    public String getTagCode() {
        return tagCode;
    }

    /** 设置TagCode。 */
    public void setTagCode(String tagCodeValue) {
        this.tagCode = tagCodeValue;
    }

    /** 获取TagName。 */
    public String getTagName() {
        return tagName;
    }

    /** 设置TagName。 */
    public void setTagName(String tagNameValue) {
        this.tagName = tagNameValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
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
