package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemCategoryStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
