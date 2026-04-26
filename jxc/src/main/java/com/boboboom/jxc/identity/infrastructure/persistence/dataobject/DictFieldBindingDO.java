package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 字典字段绑定数据对象，用于字典存储值变更时迁移历史数据。
 */
@TableName("sys_dict_field_binding")
public class DictFieldBindingDO extends BaseCreateDO {

    private String dictCode;
    private String tableName;
    private String columnName;
    private String remark;

    /** 获取DictCode。 */
    public String getDictCode() {
        return dictCode;
    }

    /** 设置DictCode。 */
    public void setDictCode(String dictCodeValue) {
        this.dictCode = dictCodeValue;
    }

    /** 获取TableName。 */
    public String getTableName() {
        return tableName;
    }

    /** 设置TableName。 */
    public void setTableName(String tableNameValue) {
        this.tableName = tableNameValue;
    }

    /** 获取ColumnName。 */
    public String getColumnName() {
        return columnName;
    }

    /** 设置ColumnName。 */
    public void setColumnName(String columnNameValue) {
        this.columnName = columnNameValue;
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
