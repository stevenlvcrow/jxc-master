package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 字典类型数据对象。
 */
@TableName("sys_dict_type")
public class DictTypeDO extends BaseAuditDO {

    private String dictCode;
    private String dictName;
    private String category;
    private String status;
    private Boolean builtin;
    private Integer sortNo;
    private String remark;

    /** 获取DictCode。 */
    public String getDictCode() {
        return dictCode;
    }

    /** 设置DictCode。 */
    public void setDictCode(String dictCodeValue) {
        this.dictCode = dictCodeValue;
    }

    /** 获取DictName。 */
    public String getDictName() {
        return dictName;
    }

    /** 设置DictName。 */
    public void setDictName(String dictNameValue) {
        this.dictName = dictNameValue;
    }

    /** 获取Category。 */
    public String getCategory() {
        return category;
    }

    /** 设置Category。 */
    public void setCategory(String categoryValue) {
        this.category = categoryValue;
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

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }
}
