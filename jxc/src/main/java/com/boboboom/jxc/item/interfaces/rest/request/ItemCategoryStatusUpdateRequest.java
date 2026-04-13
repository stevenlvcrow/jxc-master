package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record ItemCategoryStatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
