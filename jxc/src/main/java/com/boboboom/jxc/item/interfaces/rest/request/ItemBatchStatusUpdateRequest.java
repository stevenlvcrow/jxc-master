package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemBatchStatusUpdateRequest(
        @NotEmpty(message = "物品ID不能为空")
        @Size(max = 200, message = "单次最多支持200条")
        List<@NotBlank(message = "物品ID不能为空") String> ids,

        @NotBlank(message = "状态不能为空")
        String status
) {
}
