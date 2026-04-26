package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 字典项数据对象。
 */
@TableName("sys_dict_item")
public class DictItemDO extends BaseAuditDO {

    private Long dictTypeId;
    private Long parentId;
    private String itemKey;
    private String itemCode;
    private String itemLabel;
    private String status;
    private Boolean builtin;
    private Integer sortNo;
    private String extraJson;
    private String remark;

    /** 获取DictTypeId。 */
    public Long getDictTypeId() {
        return dictTypeId;
    }

    /** 设置DictTypeId。 */
    public void setDictTypeId(Long dictTypeIdValue) {
        this.dictTypeId = dictTypeIdValue;
    }

    /** 获取ParentId。 */
    public Long getParentId() {
        return parentId;
    }

    /** 设置ParentId。 */
    public void setParentId(Long parentIdValue) {
        this.parentId = parentIdValue;
    }

    /** 获取ItemKey。 */
    public String getItemKey() {
        return itemKey;
    }

    /** 设置ItemKey。 */
    public void setItemKey(String itemKeyValue) {
        this.itemKey = itemKeyValue;
    }

    /** 获取ItemCode。 */
    public String getItemCode() {
        return itemCode;
    }

    /** 设置ItemCode。 */
    public void setItemCode(String itemCodeValue) {
        this.itemCode = itemCodeValue;
    }

    /** 获取ItemLabel。 */
    public String getItemLabel() {
        return itemLabel;
    }

    /** 设置ItemLabel。 */
    public void setItemLabel(String itemLabelValue) {
        this.itemLabel = itemLabelValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取Builtin。 */
    public Boolean getBuiltin() {
        return builtin;
    }

    /** 设置Builtin。 */
    public void setBuiltin(Boolean builtinValue) {
        this.builtin = builtinValue;
    }

    /** 获取SortNo。 */
    public Integer getSortNo() {
        return sortNo;
    }

    /** 设置SortNo。 */
    public void setSortNo(Integer sortNoValue) {
        this.sortNo = sortNoValue;
    }

    /** 获取ExtraJson。 */
    public String getExtraJson() {
        return extraJson;
    }

    /** 设置ExtraJson。 */
    public void setExtraJson(String extraJsonValue) {
        this.extraJson = extraJsonValue;
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
