package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public class StatisticsTypeCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String statisticsCategory;

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
}
