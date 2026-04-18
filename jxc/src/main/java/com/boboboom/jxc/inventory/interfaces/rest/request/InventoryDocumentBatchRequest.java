package com.boboboom.jxc.inventory.interfaces.rest.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 通用库存单据批量请求。
 *
 * @param ids 单据 ID 集合
 * @param rejectionReason 拒审原因
 */
public record InventoryDocumentBatchRequest(
        @NotEmpty(message = "单据 ID 不能为空")
        List<Long> ids,
        String rejectionReason
) {
}
