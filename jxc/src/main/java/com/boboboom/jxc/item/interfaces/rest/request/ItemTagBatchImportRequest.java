package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemTagBatchImportRequest(
        @NotEmpty(message = "导入数据不能为空")
        List<@Valid Item> items
) {

    /** 物品与供应商明细项模型，承载子表或批量操作明细。 */
    public record Item(
            @Size(max = 64, message = "标签编码长度不能超过64")
            String tagCode,

            @NotBlank(message = "标签名称不能为空")
            @Size(max = 128, message = "标签名称长度不能超过128")
            String tagName,

            @Size(max = 500, message = "物品长度不能超过500")
            String itemName
    ) {
    }
}
