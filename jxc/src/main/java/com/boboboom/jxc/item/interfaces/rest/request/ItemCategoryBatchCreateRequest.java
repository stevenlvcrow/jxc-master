package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemCategoryBatchCreateRequest(
        @NotBlank(message = "上级类别不能为空")
        @Size(max = 128, message = "上级类别长度不能超过128")
        String parentCategory,

        @NotBlank(message = "状态不能为空")
        String status,

        @NotEmpty(message = "批量数据不能为空")
        List<@Valid BatchItem> items
) {
    public record BatchItem(
            @NotBlank(message = "类别编码不能为空")
            @Size(max = 64, message = "类别编码长度不能超过64")
            String categoryCode,

            @NotBlank(message = "类别名称不能为空")
            @Size(max = 128, message = "类别名称长度不能超过128")
            String categoryName,

            @Size(max = 500, message = "备注长度不能超过500")
            String remark
    ) {
    }
}
