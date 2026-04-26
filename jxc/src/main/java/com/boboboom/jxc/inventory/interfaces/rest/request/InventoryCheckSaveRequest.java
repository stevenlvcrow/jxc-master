package com.boboboom.jxc.inventory.interfaces.rest.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * 盘点单保存请求。
 *
 * @param checkDate 盘点日期
 * @param warehouseName 盘点仓库
 * @param checkRangeType 盘点范围类型
 * @param freezeStock 是否冻结库存
 * @param collaborativeFlag 是否多人协同盘点
 * @param planName 盘点方案名称
 * @param thirdPartyDocument 第三方单号
 * @param salesmanUserId 盘点人用户 ID
 * @param salesmanName 盘点人名称
 * @param remark 备注
 * @param submitted 是否提交
 * @param items 盘点明细
 */
public record InventoryCheckSaveRequest(
        @NotBlank(message = "盘点日期不能为空")
        String checkDate,
        @NotBlank(message = "盘点仓库不能为空")
        String warehouseName,
        @NotBlank(message = "盘点范围类型不能为空")
        String checkRangeType,
        Boolean freezeStock,
        Boolean collaborativeFlag,
        String planName,
        String thirdPartyDocument,
        Long salesmanUserId,
        String salesmanName,
        String remark,
        Boolean submitted,
        @Valid
        @NotEmpty(message = "盘点明细不能为空")
        List<InventoryCheckLineRequest> items
) {

    /**
     * 盘点单明细请求。
     *
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param spec 规格
     * @param category 分类
     * @param unitName 单位
     * @param availableQty 账面可用数量
     * @param bookQty 账面数量
     * @param actualQty 实盘数量
     * @param bookPrice 账面单价
     * @param profitLossReason 盈亏原因
     * @param remark 备注
     * @param extraFields 扩展字段
     */
    public record InventoryCheckLineRequest(
            @NotBlank(message = "物品编码不能为空")
            String itemCode,
            @NotBlank(message = "物品名称不能为空")
            String itemName,
            String spec,
            String category,
            String unitName,
            BigDecimal availableQty,
            BigDecimal bookQty,
            BigDecimal actualQty,
            BigDecimal bookPrice,
            String profitLossReason,
            String remark,
            Map<String, String> extraFields
    ) {
    }
}
