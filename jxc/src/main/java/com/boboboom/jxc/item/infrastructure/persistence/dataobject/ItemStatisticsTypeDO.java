package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("item_statistics_type")
public class ItemStatisticsTypeDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String code;
    private String name;
    private String statisticsCategory;
    private String createType;

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

    /** 获取Code。 */
    public String getCode() {
        return code;
    }

    /** 设置Code。 */
    public void setCode(String codeValue) {
        this.code = codeValue;
    }

    /** 获取Name。 */
    public String getName() {
        return name;
    }

    /** 设置Name。 */
    public void setName(String nameValue) {
        this.name = nameValue;
    }

    /** 获取StatisticsCategory。 */
    public String getStatisticsCategory() {
        return statisticsCategory;
    }

    /** 设置StatisticsCategory。 */
    public void setStatisticsCategory(String statisticsCategoryValue) {
        this.statisticsCategory = statisticsCategoryValue;
    }

    /** 获取CreateType。 */
    public String getCreateType() {
        return createType;
    }

    /** 设置CreateType。 */
    public void setCreateType(String createTypeValue) {
        this.createType = createTypeValue;
    }
}
