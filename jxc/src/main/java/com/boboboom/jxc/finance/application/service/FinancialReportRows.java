package com.boboboom.jxc.finance.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务报表行模型集合。
 */
public final class FinancialReportRows {

    private FinancialReportRows() {
    }

    /** 成本差异分析行。 */
    public record CostVarianceRow(Long id,
                                  String dishId,
                                  String dishName,
                                  String itemCode,
                                  String itemName,
                                  BigDecimal standardQty,
                                  BigDecimal standardCost,
                                  BigDecimal actualQty,
                                  BigDecimal actualCost,
                                  BigDecimal varianceAmount,
                                  BigDecimal varianceRate,
                                  String varianceReason) {
    }

    /** 菜品报损成本统计行。 */
    public record DishDamageCostRow(Long id,
                                    String documentCode,
                                    LocalDate documentDate,
                                    String dishId,
                                    String dishName,
                                    String itemCode,
                                    String itemName,
                                    BigDecimal damageQty,
                                    BigDecimal damageCost,
                                    String damageReason,
                                    BigDecimal damageRate) {
    }

    /** 门店毛利分析行。 */
    public record StoreGrossProfitRow(Long id,
                                      String storeName,
                                      String periodStart,
                                      String periodEnd,
                                      BigDecimal revenue,
                                      BigDecimal materialCost,
                                      BigDecimal grossProfit,
                                      BigDecimal grossProfitRate,
                                      BigDecimal costRate,
                                      BigDecimal expenseRatio) {
    }

    /** 部门毛利分析行。 */
    public record DepartmentGrossProfitRow(Long id,
                                           String department,
                                           BigDecimal revenue,
                                           BigDecimal materialCost,
                                           BigDecimal grossProfit,
                                           BigDecimal grossProfitRate,
                                           BigDecimal costRate) {
    }

    /** 菜品毛利分析行。 */
    public record DishGrossProfitRow(Long id,
                                     String dishId,
                                     String dishName,
                                     BigDecimal salesQty,
                                     BigDecimal unitPrice,
                                     BigDecimal revenue,
                                     BigDecimal materialCost,
                                     BigDecimal grossProfit,
                                     BigDecimal grossProfitRate,
                                     BigDecimal costRate) {
    }

    /** 应付对账行。 */
    public record PayableReconciliationRow(Long id,
                                           String supplierName,
                                           LocalDate periodStart,
                                           LocalDate periodEnd,
                                           BigDecimal openingPayableAmount,
                                           BigDecimal purchaseAmount,
                                           BigDecimal paymentAmount,
                                           BigDecimal endingPayableAmount,
                                           String reconciliationStatus) {
    }

    /** 应付对账单状态跟踪行。 */
    public record PayableReconciliationStatusRow(Long id,
                                                 String supplierName,
                                                 String reconciliationNo,
                                                 LocalDate periodStart,
                                                 LocalDate periodEnd,
                                                 BigDecimal reconciliationAmount,
                                                 String reconciliationStatus,
                                                 LocalDate settlementDate,
                                                 String remark) {
    }

    /** 应付明细行。 */
    public record PayableDetailRow(Long id,
                                   String supplierName,
                                   String inboundDocumentCode,
                                   LocalDate inboundDate,
                                   String itemCode,
                                   String itemName,
                                   BigDecimal inboundAmount,
                                   BigDecimal paidAmount,
                                   BigDecimal unpaidAmount,
                                   String invoiceStatus) {
    }
}
