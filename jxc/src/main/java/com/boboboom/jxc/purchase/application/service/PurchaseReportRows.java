package com.boboboom.jxc.purchase.application.service;

import java.math.BigDecimal;
import java.util.List;

/** 采购报表行模型集合。 */
public final class PurchaseReportRows {

    private PurchaseReportRows() {
    }

    /** 采购报表分页数据。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    /** 采购订单状态跟踪报表行。 */
    public record PurchaseOrderStatusTrackingRow(String id,
                                                 String purchaseOrderCode,
                                                 String documentStatus,
                                                 String receiveStatus,
                                                 String orderDate,
                                                 String expectedArrivalDate,
                                                 String supplierName,
                                                 String itemName,
                                                 String spec,
                                                 String itemCategory,
                                                 String isGift,
                                                 String purchaseUnit,
                                                 BigDecimal purchasePrice,
                                                 BigDecimal purchaseQty,
                                                 BigDecimal auditQty,
                                                 BigDecimal purchaseAmount,
                                                 BigDecimal auditAmount,
                                                 String purchaseWarehouse,
                                                 String receiptDate,
                                                 String receiptWarehouse,
                                                 BigDecimal receivedQty,
                                                 BigDecimal receiptAmount,
                                                 BigDecimal unreceivedQty,
                                                 BigDecimal returnedQty,
                                                 BigDecimal returnAmount) {
    }

    /** 采购退货状态跟踪报表行。 */
    public record PurchaseReturnStatusTrackingRow(String id,
                                                  String returnCode,
                                                  String documentStatus,
                                                  String returnDate,
                                                  String sourceCode,
                                                  String supplierCode,
                                                  String supplierName,
                                                  String itemName,
                                                  String spec,
                                                  String itemCategory,
                                                  String purchaseUnit,
                                                  String baseUnit,
                                                  String isGift,
                                                  BigDecimal returnQty,
                                                  BigDecimal returnBaseQty,
                                                  BigDecimal returnAmount,
                                                  BigDecimal auditQty,
                                                  BigDecimal shippedQty,
                                                  BigDecimal shippedBaseQty,
                                                  BigDecimal shippedAmount,
                                                  String shippingWarehouse) {
    }

    /** 采购价格分析周期列。 */
    public record PurchasePriceAnalysisPeriod(String key, String label, String compactLabel) {
    }

    /** 采购物品价格分析报表行。 */
    public record PurchasePriceAnalysisRow(String id,
                                           String itemName,
                                           String itemCode,
                                           String spec,
                                           String itemCategory,
                                           String baseUnit,
                                           String unit,
                                           String supplier,
                                           List<PurchasePriceAnalysisValue> values) {
    }

    /** 采购物品价格分析周期值。 */
    public record PurchasePriceAnalysisValue(String periodKey,
                                             BigDecimal total,
                                             BigDecimal avg,
                                             BigDecimal fluctuationRate) {
    }

    /** 采购物品价格分析报表。 */
    public record PurchasePriceAnalysisReport(List<PurchasePriceAnalysisPeriod> periods,
                                              PageData<PurchasePriceAnalysisRow> page) {
    }
}
