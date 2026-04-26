package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存补充报表响应行模型。
 */
public final class InventorySupplementReportRows {

    private InventorySupplementReportRows() {
    }

    /** 分页响应数据。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    /** 呆滞库存报表行。 */
    public record StagnantStockReportRow(String id, String warehouse, String itemName, String itemCode,
                                         String specModel, String unit, String firstInboundTime,
                                         String latestInboundTime, String latestOutboundTime,
                                         BigDecimal latestInboundQty, BigDecimal latestOutboundQty,
                                         BigDecimal stockQty, int retainedDays, int itemStagnantDays,
                                         String stagnant, String itemStatus, String itemCategory) {
    }

    /** 盘点盈亏报表行。 */
    public record InventoryProfitLossReportRow(String id, String itemCode, String itemName, String specModel,
                                               String itemCategory, String statisticsType, String unit,
                                               String checkDocumentNo, String checkType, String stockDocumentNo,
                                               String orgName, String orgCode, String warehouse, String checkTime,
                                               String auditTime, String auditor, BigDecimal bookQty,
                                               BigDecimal bookAmount, BigDecimal actualQty, BigDecimal actualAmount,
                                               BigDecimal profitLossQty, BigDecimal profitLossAmount,
                                               BigDecimal profitLossQtyAbs, BigDecimal profitLossAmountAbs,
                                               BigDecimal adjustmentAmount, String profitLossResult,
                                               BigDecimal profitInboundPrice, BigDecimal lossOutboundPrice,
                                               String checkReason, String remark) {
    }

    /** 进销存汇总报表行。 */
    public record InventoryInoutSummaryReportRow(String id, String itemCode, String itemName, String specModel,
                                                 String itemCategory, String statisticType, String unit,
                                                 String orgName, String orgCode, String warehouse,
                                                 String warehouseType, BigDecimal openingQty,
                                                 BigDecimal openingCostAmountExTax, BigDecimal openingAvgCostExTax,
                                                 BigDecimal inboundQty, BigDecimal inboundCostAmountExTax,
                                                 BigDecimal inboundAvgCostExTax, BigDecimal outboundQty,
                                                 BigDecimal outboundCostAmountExTax, BigDecimal outboundAvgCostExTax,
                                                 BigDecimal closingQty, BigDecimal closingCostAmountExTax,
                                                 BigDecimal closingAvgCostExTax, BigDecimal inventoryProfitLossQty,
                                                 BigDecimal inventoryProfitLossCostAmountTaxIncluded,
                                                 BigDecimal inventoryProfitLossCostAmountExTax,
                                                 BigDecimal inventoryCheckQty,
                                                 BigDecimal inventoryCheckCostAmountTaxIncluded,
                                                 BigDecimal closingCheckDiffQty,
                                                 BigDecimal closingCheckDiffAmountExTax,
                                                 BigDecimal returnDifferenceQty,
                                                 BigDecimal returnDifferenceCostAmountExTax, String inoutType) {
        boolean isEmpty() {
            return openingQty.compareTo(BigDecimal.ZERO) == 0
                    && inboundQty.compareTo(BigDecimal.ZERO) == 0
                    && outboundQty.compareTo(BigDecimal.ZERO) == 0
                    && closingQty.compareTo(BigDecimal.ZERO) == 0
                    && inventoryProfitLossQty.compareTo(BigDecimal.ZERO) == 0
                    && inventoryCheckQty.compareTo(BigDecimal.ZERO) == 0
                    && closingCheckDiffQty.compareTo(BigDecimal.ZERO) == 0
                    && returnDifferenceQty.compareTo(BigDecimal.ZERO) == 0;
        }

        boolean hasInoutMovement() {
            return inboundQty.compareTo(BigDecimal.ZERO) != 0 || outboundQty.compareTo(BigDecimal.ZERO) != 0;
        }
    }

    /** 库存出入库汇总报表行。 */
    public record StockInoutSummaryReportRow(String id, String itemCode, String itemName, String specModel,
                                             String itemCategory, String statisticType, String unit,
                                             String inoutType, String warehouse, String warehouseType,
                                             String oppositeOrg, String oppositeWarehouse, BigDecimal inboundQty,
                                             BigDecimal inboundCostAmountTaxIncluded,
                                             BigDecimal inboundAvgCostTaxIncluded,
                                             BigDecimal inboundSettlementAmountTaxIncluded,
                                             BigDecimal inboundAvgSettlementTaxIncluded, BigDecimal outboundQty,
                                             BigDecimal outboundCostAmountTaxIncluded,
                                             BigDecimal outboundAvgCostTaxIncluded,
                                             BigDecimal outboundSettlementAmountTaxIncluded,
                                             BigDecimal outboundAvgSettlementTaxIncluded, String statisticMode) {
    }

    /** 其他出入库汇总报表行。 */
    public record OtherInoutSummaryReportRow(String id, String itemCode, String itemName, String specModel,
                                             String itemCategory, String baseUnit, String warehouse,
                                             String inoutType, String reasonType, BigDecimal quantity,
                                             BigDecimal amountExTax) {
    }

    /** 跨组织调拨明细报表行。 */
    public record InterOrgTransferDetailReportRow(String id, String transferNo, String itemCode, String itemName,
                                                  String documentStatus, String transferDate,
                                                  String outboundAuditTime, String sourceStore,
                                                  String sourceWarehouse, String inboundDate,
                                                  String inboundAuditTime, String targetStore,
                                                  String targetWarehouse, String specModel, String itemCategory,
                                                  String baseUnit, BigDecimal transferBaseQty, String businessUnit,
                                                  BigDecimal transferQty, BigDecimal inboundAmountTaxIncluded,
                                                  BigDecimal outboundCostAmountExTax,
                                                  BigDecimal outboundSettlementAmountTaxIncluded,
                                                  BigDecimal inboundPriceTaxIncluded,
                                                  BigDecimal outboundCostPriceExTax,
                                                  BigDecimal outboundSettlementPriceTaxIncluded, String remark,
                                                  String statisticMode) {
    }

    /** 跨组织调拨汇总报表行。 */
    public record InterOrgTransferSummaryReportRow(String id, String itemCode, String itemName, String sourceStore,
                                                   String targetStore, String specModel, String itemCategory,
                                                   String unit, BigDecimal transferQty,
                                                   BigDecimal inboundAmountTaxIncluded,
                                                   BigDecimal outboundCostAmountExTax,
                                                   BigDecimal outboundSettlementAmountTaxIncluded,
                                                   BigDecimal inboundAvgPriceTaxIncluded,
                                                   BigDecimal outboundCostAvgPriceExTax,
                                                   BigDecimal outboundSettlementAvgPriceTaxIncluded) {
    }

    /** 库存周转率报表行。 */
    public record StockTurnoverRateReportRow(String id, String orgName, String warehouse, String itemName,
                                             String itemCode, String unit, String itemCategory, String itemStatus,
                                             BigDecimal openingAmount, BigDecimal closingAmount,
                                             BigDecimal avgStockAmount, BigDecimal outboundAmount,
                                             BigDecimal turnoverRate, BigDecimal turnoverDays) {
    }
}
