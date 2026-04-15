package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

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
    public record ItemRow(
        String itemCode,
        String itemName,
        String specModel,
        String itemCategory
    ) {}

    public record CategoryRow(
        String categoryCode,
        String categoryName,
        String parentCategory,
        String childCategory
    ) {}

    public record WarehouseRow(
        Long warehouseId
    ) {}
}
