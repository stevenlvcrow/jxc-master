package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemCategoryUpdateRequest(
        @NotBlank(message = "类别名称不能为空")
        @Size(max = 128, message = "类别名称长度不能超过128")
        String categoryName,

        @NotBlank(message = "上级类别不能为空")
        @Size(max = 128, message = "上级类别长度不能超过128")
        String parentCategory,

        @NotBlank(message = "状态不能为空")
        String status,

        @Size(max = 500, message = "备注长度不能超过500")
        String remark
) {
}
