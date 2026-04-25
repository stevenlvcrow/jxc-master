package com.boboboom.jxc.inventory.interfaces.rest.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 库存请求参数，承载接口入参。 */
public record PurchaseInboundCreateRequest(
        @NotBlank(message = "入库日期不能为空")
        String inboundDate,
        @NotBlank(message = "仓库不能为空")
        String warehouse,
        @NotBlank(message = "供应商不能为空")
        String supplier,
        Long salesmanUserId,
        String salesmanName,
        String upstreamCode,
        String remark,
        @NotEmpty(message = "入库物品不能为空")
        @Size(max = 200, message = "单据行最多支持200条")
        List<@Valid LineItem> items
) {
    /** 库存明细项模型，承载子表或批量操作明细。 */
    public record LineItem(
            @NotBlank(message = "物品编码不能为空")
            String itemCode,
            @NotBlank(message = "物品名称不能为空")
            String itemName,
            String spec,
            String category,
            @NotNull(message = "数量不能为空")
            @DecimalMin(value = "0.000001", message = "数量必须大于0")
            BigDecimal quantity,
            @NotNull(message = "单价不能为空")
            @DecimalMin(value = "0", message = "单价不能小于0")
            BigDecimal unitPrice,
            @DecimalMin(value = "0", message = "税率不能小于0")
            BigDecimal taxRate
    ) {
    }
}
