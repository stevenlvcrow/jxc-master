package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemCategoryBatchCreateRequest(
        @NotBlank(message = "上级类别不能为空")
        @Size(max = 128, message = "上级类别长度不能超过128")
        String parentCategory,

        @NotBlank(message = "状态不能为空")
        String status,

        @NotEmpty(message = "批量数据不能为空")
        List<@Valid BatchItem> items
) {
    /** 物品与供应商明细项模型，承载子表或批量操作明细。 */
    public record BatchItem(
            @NotBlank(message = "类别名称不能为空")
            @Size(max = 128, message = "类别名称长度不能超过128")
            String categoryName,

            @Size(max = 500, message = "备注长度不能超过500")
            String remark
    ) {
    }
}
