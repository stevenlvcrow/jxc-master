package com.boboboom.jxc.finance.application.service;

import java.time.LocalDate;

/**
 * 财务报表查询条件。
 */
public record FinancialReportQuery(String scopeType,
                                   Long scopeId,
                                   Long operatorId,
                                   boolean viewAll,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String keyword,
                                   String supplierName,
                                   String warehouseName,
                                   String department,
                                   String dishId,
                                   String itemCode,
                                   String status) {
}
