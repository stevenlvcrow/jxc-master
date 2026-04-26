package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

/** 物品与供应商请求参数，承载接口入参。 */
public class StatisticsTypeBatchExportRequest {

    private List<Long> ids;

    /** 获取Ids。 */
    public List<Long> getIds() {
        return ids;
    }

    /** 设置Ids。 */
    public void setIds(List<Long> idsValue) {
        this.ids = idsValue;
    }
}
