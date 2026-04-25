package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemBatchDeleteRequest(
        @NotEmpty(message = "物品ID不能为空")
        @Size(max = 200, message = "单次最多支持200条")
        List<@NotBlank(message = "物品ID不能为空") String> ids
) {
}
