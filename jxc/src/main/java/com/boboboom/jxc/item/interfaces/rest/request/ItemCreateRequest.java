package com.boboboom.jxc.item.interfaces.rest.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 物品与供应商请求参数，承载接口入参。 */
public record ItemCreateRequest(
        @NotBlank(message = "物品名称不能为空")
        String name,
        String code,
        @NotBlank(message = "物品类别不能为空")
        String category,
        String spec,
        @NotBlank(message = "状态不能为空")
        String status,
        String brand,
        String mnemonicCode,
        String barcode,
        String thirdPartyCode,
        String defaultPurchaseUnit,
        String defaultOrderUnit,
        String defaultStockUnit,
        String defaultCostUnit,
        List<String> stocktakeUnits,
        Boolean assistUnitEnabled,
        String productionRefCost,
        Boolean monthEndUpdate,
        String suggestPurchasePrice,
        @NotBlank(message = "储存方式不能为空")
        String storageMode,
        String statType,
        String stockMin,
        String stockMax,
        String safeStock,
        String taxCode,
        String taxName,
        String taxRate,
        String taxBenefit,
        String consumeOnInbound,
        String disableStocktake,
        String defaultNoStocktake,
        String stocktakeFrequency,
        String purchaseReceiptRule,
        String purchaseRuleMaxRatio,
        String purchaseRuleMinRatio,
        String purchaseRuleBetweenStart,
        String purchaseRuleBetweenEnd,
        Boolean requireAssemblyProcess,
        Boolean requireSplitProcess,
        Boolean requireBatchReport,
        Boolean allowLossReport,
        Boolean allowTransfer,
        Boolean enablePrepare,
        String productCategory,
        String netContent,
        String ingredients,
        String itemDescription,
        String remark,
        String alias,
        String abcClass,
        Boolean batchManagement,
        Boolean shelfLifeEnabled,
        Integer shelfLifeDays,
        Integer warningDays,
        Integer stagnantDays,
        String tag,
        @Size(max = 50, message = "单位设置最多50条")
        List<@Valid UnitSettingRow> unitSettingRows,
        @Size(max = 50, message = "供货关系最多50条")
        List<@Valid SupplierRelationRow> supplierRelationRows,
        Long defaultSupplierRowKey,
        @Size(max = 20, message = "物品图片最多20张")
        List<@Valid IntroImageRow> introImages,
        @Valid NutritionHeader nutritionHeaders,
        @Size(max = 100, message = "营养成分最多100条")
        List<@Valid NutritionRow> nutritionRows,
        @Size(max = 100, message = "扩展信息最多100条")
        List<@Valid ExtensionInfoRow> extensionInfoRows
) {

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
    public record UnitSettingRow(
            String unit,
            String convertFrom,
            String convertTo,
            String volume,
            String volumeUnit,
            String weight,
            String weightUnit,
            String barcode
    ) {
    }

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
    public record SupplierRelationRow(
            Long key,
            String supplier,
            String contact,
            String phone
    ) {
    }

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
    public record IntroImageRow(
            Long id,
            String name,
            String url
    ) {
    }

    /** 物品与供应商数据模型，承载Nutrition单头数据。 */
    public record NutritionHeader(
            String item,
            String per100g,
            String nrv
    ) {
    }

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
    public record NutritionRow(
            Long id,
            String item,
            String per100g,
            String nrv
    ) {
    }

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
    public record ExtensionInfoRow(
            Long id,
            String name,
            String value
    ) {
    }
}
