package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 物品与供应商请求参数，承载接口入参。 */
public class StatisticsTypeCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String statisticsCategory;

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
}
