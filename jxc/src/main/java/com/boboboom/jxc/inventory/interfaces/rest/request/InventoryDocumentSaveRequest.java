package com.boboboom.jxc.inventory.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 通用库存单据保存请求。
 *
 * @param documentDate 业务日期
 * @param primaryName 主体一名称
 * @param secondaryName 主体二名称
 * @param counterpartyName 对方主体一
 * @param counterpartyName2 对方主体二
 * @param reason 业务原因
 * @param upstreamCode 上游单号
 * @param salesmanUserId 业务员 ID
 * @param salesmanName 业务员名称
 * @param remark 备注
 * @param extraFields 扩展字段
 * @param items 单据明细
 */
public record InventoryDocumentSaveRequest(
        @NotBlank(message = "业务日期不能为空")
        String documentDate,
        String primaryName,
        String secondaryName,
        String counterpartyName,
        String counterpartyName2,
        String reason,
        String upstreamCode,
        Long salesmanUserId,
        String salesmanName,
        String remark,
        Map<String, String> extraFields,
        @Valid
        @NotEmpty(message = "单据明细不能为空")
        List<InventoryDocumentLineRequest> items
) {

    /**
     * 通用库存单据明细请求。
     *
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param spec 规格
     * @param category 分类
     * @param unitName 单位
     * @param availableQty 可用数量
     * @param quantity 业务数量
     * @param unitPrice 单价
     * @param amount 金额
     * @param lineReason 行原因
     * @param remark 行备注
     * @param extraFields 扩展字段
     */
    public record InventoryDocumentLineRequest(
            @NotBlank(message = "物品编码不能为空")
            String itemCode,
            @NotBlank(message = "物品名称不能为空")
            String itemName,
            String spec,
            String category,
            String unitName,
            BigDecimal availableQty,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String lineReason,
            String remark,
            Map<String, String> extraFields
    ) {
    }
}
