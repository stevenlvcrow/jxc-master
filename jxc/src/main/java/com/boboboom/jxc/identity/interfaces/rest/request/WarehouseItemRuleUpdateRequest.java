package com.boboboom.jxc.identity.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public record WarehouseItemRuleUpdateRequest(
    @NotBlank String ruleName,
    Boolean businessControl,
    Boolean controlOrder,
    Boolean controlPurchaseInbound,
    Boolean controlTransferInbound,
    List<ItemRow> items,
    List<CategoryRow> categories,
    List<WarehouseRow> warehouses
) {
    /** 身份与权限行数据模型，承载列表或报表明细。 */
    public record ItemRow(
        String itemCode,
        String itemName,
        String specModel,
        String itemCategory
    ) { }

    /** 身份与权限行数据模型，承载列表或报表明细。 */
    public record CategoryRow(
        String categoryCode,
        String categoryName,
        String parentCategory,
        String childCategory
    ) { }

    /** 身份与权限行数据模型，承载列表或报表明细。 */
    public record WarehouseRow(
        Long warehouseId
    ) { }
}
