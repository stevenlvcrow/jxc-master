package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.util.StringUtils;

import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.InterOrgTransferDetailReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.InterOrgTransferSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.InventoryInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.OtherInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.StockInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.StockTurnoverRateReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.StockWarningReportRow;

final class MutableStockWarningRow {
    private static final int INVENTORY_QUANTITY_SCALE = 4;

    private String id = "";
    private String itemCode = "";
    private String itemName = "";
    private String unit = "";
    private String itemCategory = "";
    private String warehouse = "";
    private BigDecimal currentStock = BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    private BigDecimal stockUpperLimit = BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    private BigDecimal stockLowerLimit = BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    private String warningStatus = "正常";
    private String itemStatus = "";

    void merge(StockWarningReportRow row) {
        if (!StringUtils.hasText(id)) {
            id = row.id();
        }
        itemCode = row.itemCode();
        itemName = row.itemName();
        unit = row.unit();
        itemCategory = row.itemCategory();
        warehouse = row.warehouse();
        currentStock = currentStock.add(defaultQuantity(row.currentStock()));
        stockUpperLimit = row.stockUpperLimit();
        stockLowerLimit = row.stockLowerLimit();
        warningStatus = row.warningStatus();
        itemStatus = row.itemStatus();
    }

    StockWarningReportRow toRow() {
        return new StockWarningReportRow(id, itemCode, itemName, unit, itemCategory, warehouse,
                currentStock, stockUpperLimit, stockLowerLimit, warningStatus, itemStatus);
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}

final class MutableInventoryInoutSummaryRow {
    private InventoryInoutSummaryReportRow row;

    void merge(InventoryInoutSummaryReportRow value) {
        if (row == null) {
            row = value;
            return;
        }
        row = new InventoryInoutSummaryReportRow(row.id(), row.itemCode(), row.itemName(), row.specModel(),
                row.itemCategory(), row.statisticType(), row.unit(), row.orgName(), row.orgCode(), row.warehouse(),
                row.warehouseType(), row.openingQty().add(value.openingQty()),
                row.openingCostAmountExTax().add(value.openingCostAmountExTax()), row.openingAvgCostExTax(),
                row.inboundQty().add(value.inboundQty()),
                row.inboundCostAmountExTax().add(value.inboundCostAmountExTax()), row.inboundAvgCostExTax(),
                row.outboundQty().add(value.outboundQty()),
                row.outboundCostAmountExTax().add(value.outboundCostAmountExTax()), row.outboundAvgCostExTax(),
                row.closingQty().add(value.closingQty()),
                row.closingCostAmountExTax().add(value.closingCostAmountExTax()), row.closingAvgCostExTax(),
                row.inventoryProfitLossQty().add(value.inventoryProfitLossQty()),
                row.inventoryProfitLossCostAmountTaxIncluded().add(value.inventoryProfitLossCostAmountTaxIncluded()),
                row.inventoryProfitLossCostAmountExTax().add(value.inventoryProfitLossCostAmountExTax()),
                row.inventoryCheckQty().add(value.inventoryCheckQty()),
                row.inventoryCheckCostAmountTaxIncluded().add(value.inventoryCheckCostAmountTaxIncluded()),
                row.closingCheckDiffQty().add(value.closingCheckDiffQty()),
                row.closingCheckDiffAmountExTax().add(value.closingCheckDiffAmountExTax()),
                row.returnDifferenceQty().add(value.returnDifferenceQty()),
                row.returnDifferenceCostAmountExTax().add(value.returnDifferenceCostAmountExTax()), row.inoutType());
    }

    InventoryInoutSummaryReportRow toRow() {
        return row;
    }
}

final class MutableStockInoutSummaryRow {
    private StockInoutSummaryReportRow row;

    void merge(StockInoutSummaryReportRow value) {
        if (row == null) {
            row = value;
            return;
        }
        row = new StockInoutSummaryReportRow(row.id(), row.itemCode(), row.itemName(), row.specModel(),
                row.itemCategory(), row.statisticType(), row.unit(), row.inoutType(), row.warehouse(),
                row.warehouseType(), row.oppositeOrg(), row.oppositeWarehouse(),
                row.inboundQty().add(value.inboundQty()),
                row.inboundCostAmountTaxIncluded().add(value.inboundCostAmountTaxIncluded()),
                row.inboundAvgCostTaxIncluded(),
                row.inboundSettlementAmountTaxIncluded().add(value.inboundSettlementAmountTaxIncluded()),
                row.inboundAvgSettlementTaxIncluded(), row.outboundQty().add(value.outboundQty()),
                row.outboundCostAmountTaxIncluded().add(value.outboundCostAmountTaxIncluded()),
                row.outboundAvgCostTaxIncluded(),
                row.outboundSettlementAmountTaxIncluded().add(value.outboundSettlementAmountTaxIncluded()),
                row.outboundAvgSettlementTaxIncluded(), row.statisticMode());
    }

    StockInoutSummaryReportRow toRow() {
        return row;
    }
}

final class MutableOtherInoutSummaryRow {
    private OtherInoutSummaryReportRow row;

    void merge(OtherInoutSummaryReportRow value) {
        if (row == null) {
            row = value;
            return;
        }
        row = new OtherInoutSummaryReportRow(row.id(), row.itemCode(), row.itemName(), row.specModel(),
                row.itemCategory(), row.baseUnit(), row.warehouse(), row.inoutType(), row.reasonType(),
                row.quantity().add(value.quantity()), row.amountExTax().add(value.amountExTax()));
    }

    OtherInoutSummaryReportRow toRow() {
        return row;
    }
}

final class MutableInterOrgTransferSummaryRow {
    private InterOrgTransferSummaryReportRow row;

    void merge(InterOrgTransferDetailReportRow value) {
        if (row == null) {
            row = new InterOrgTransferSummaryReportRow(value.id(), value.itemCode(), value.itemName(),
                    value.sourceStore(), value.targetStore(), value.specModel(), value.itemCategory(), value.baseUnit(),
                    value.transferQty(), value.inboundAmountTaxIncluded(), value.outboundCostAmountExTax(),
                    value.outboundSettlementAmountTaxIncluded(), value.inboundPriceTaxIncluded(),
                    value.outboundCostPriceExTax(), value.outboundSettlementPriceTaxIncluded());
            return;
        }
        row = new InterOrgTransferSummaryReportRow(row.id(), row.itemCode(), row.itemName(), row.sourceStore(),
                row.targetStore(), row.specModel(), row.itemCategory(), row.unit(),
                row.transferQty().add(value.transferQty()),
                row.inboundAmountTaxIncluded().add(value.inboundAmountTaxIncluded()),
                row.outboundCostAmountExTax().add(value.outboundCostAmountExTax()),
                row.outboundSettlementAmountTaxIncluded().add(value.outboundSettlementAmountTaxIncluded()),
                row.inboundAvgPriceTaxIncluded(), row.outboundCostAvgPriceExTax(),
                row.outboundSettlementAvgPriceTaxIncluded());
    }

    InterOrgTransferSummaryReportRow toRow() {
        return row;
    }
}

final class MutableStockTurnoverRateRow {
    private StockTurnoverRateReportRow row;

    void merge(StockTurnoverRateReportRow value) {
        if (row == null) {
            row = value;
            return;
        }
        row = new StockTurnoverRateReportRow(row.id(), row.orgName(), row.warehouse(), row.itemName(),
                row.itemCode(), row.unit(), row.itemCategory(), row.itemStatus(),
                row.openingAmount().add(value.openingAmount()), row.closingAmount().add(value.closingAmount()),
                row.avgStockAmount().add(value.avgStockAmount()), row.outboundAmount().add(value.outboundAmount()),
                row.turnoverRate().add(value.turnoverRate()), row.turnoverDays().add(value.turnoverDays()));
    }

    StockTurnoverRateReportRow toRow() {
        return row;
    }
}
