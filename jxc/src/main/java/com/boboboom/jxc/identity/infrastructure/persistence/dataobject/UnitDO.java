package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_unit")
public class UnitDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String unitCode;
    private String unitName;
    private String unitType;
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

    /** 获取UnitCode。 */
    public String getUnitCode() {
        return unitCode;
    }

    /** 设置UnitCode。 */
    public void setUnitCode(String unitCodeValue) {
        this.unitCode = unitCodeValue;
    }

    /** 获取UnitName。 */
    public String getUnitName() {
        return unitName;
    }

    /** 设置UnitName。 */
    public void setUnitName(String unitNameValue) {
        this.unitName = unitNameValue;
    }

    /** 获取UnitType。 */
    public String getUnitType() {
        return unitType;
    }

    /** 设置UnitType。 */
    public void setUnitType(String unitTypeValue) {
        this.unitType = unitTypeValue;
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
