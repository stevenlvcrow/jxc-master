package com.boboboom.jxc.inventory.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 库存请求参数，承载接口入参。 */
public record PurchaseInboundBatchRequest(
        @NotEmpty(message = "单据ID不能为空")
        @Size(max = 200, message = "单次最多支持200条")
        List<@NotNull(message = "单据ID不能为空") Long> ids,
        @Size(max = 256, message = "拒审原因最多256个字符")
        String rejectionReason
) {
}
