package com.boboboom.jxc.inventory.interfaces.rest.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseInboundBatchRequest(
        @NotEmpty(message = "单据ID不能为空")
        @Size(max = 200, message = "单次最多支持200条")
        List<@NotNull(message = "单据ID不能为空") Long> ids
) {
}

