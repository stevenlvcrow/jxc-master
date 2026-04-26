package com.boboboom.jxc.purchase.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 采购报表单据行快照。 */
public record PurchaseReportLineSnapshot(Long documentId,
                                         Long lineId,
                                         String documentCode,
                                         LocalDate documentDate,
                                         LocalDate expectedArrivalDate,
                                         String documentStatus,
                                         String receiveStatus,
                                         String sourceDocumentCode,
                                         String supplierName,
                                         String supplierCode,
                                         String warehouseName,
                                         String itemCode,
                                         String itemName,
                                         String spec,
                                         String itemCategory,
                                         String purchaseUnit,
                                         String baseUnit,
                                         String baseConversion,
                                         BigDecimal quantity,
                                         BigDecimal reviewQty,
                                         BigDecimal receivedQty,
                                         BigDecimal unitPrice,
                                         BigDecimal amount,
                                         Boolean isGift,
                                         String lineWarehouseName,
                                         LocalDate lineExpectedArrivalDate) {
}
