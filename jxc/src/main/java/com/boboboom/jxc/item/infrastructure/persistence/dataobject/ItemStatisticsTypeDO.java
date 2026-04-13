package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

@TableName("dev.item_statistics_type")
public class ItemStatisticsTypeDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String code;
    private String name;
    private String statisticsCategory;
    private String createType;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatisticsCategory() {
        return statisticsCategory;
    }

    public void setStatisticsCategory(String statisticsCategory) {
        this.statisticsCategory = statisticsCategory;
    }

    public String getCreateType() {
        return createType;
    }

    public void setCreateType(String createType) {
        this.createType = createType;
    }
}
