package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

public class StatisticsTypeBatchExportRequest {

    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
