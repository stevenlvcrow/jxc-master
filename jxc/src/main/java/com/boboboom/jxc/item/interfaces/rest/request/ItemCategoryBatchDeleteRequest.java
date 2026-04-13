package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ItemCategoryBatchDeleteRequest(
        @NotEmpty(message = "删除ID不能为空")
        List<Long> ids
) {
}
