package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemCategoryBatchDeleteRequest(
        @NotEmpty(message = "删除ID不能为空")
        List<Long> ids
) {
}
