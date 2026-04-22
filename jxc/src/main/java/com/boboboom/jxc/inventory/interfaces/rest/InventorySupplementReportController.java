package com.boboboom.jxc.inventory.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.InventoryProfitLossReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.PageData;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.StagnantStockReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService.StockWarningReportRow;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存报表补充接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory")
public class InventorySupplementReportController {

    private final InventorySupplementReportApplicationService inventorySupplementReportApplicationService;

    public InventorySupplementReportController(InventorySupplementReportApplicationService inventorySupplementReportApplicationService) {
        this.inventorySupplementReportApplicationService = inventorySupplementReportApplicationService;
    }

    @GetMapping("/stock-warning/report")
    public CodeDataResponse<PageData<StockWarningReportRow>> stockWarningReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String warningStatus,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.stockWarningReport(
                pageNo,
                pageSize,
                statisticDimension,
                warehouse,
                itemCategory,
                itemCode,
                itemStatus,
                warningStatus,
                unitType,
                orgId
        ));
    }

    @GetMapping("/stagnant-stock/report")
    public CodeDataResponse<PageData<StagnantStockReportRow>> stagnantStockReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String stagnant,
            @RequestParam(required = false) String stagnantDaysGreaterThan,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.stagnantStockReport(
                pageNo,
                pageSize,
                warehouse,
                itemCode,
                itemCategory,
                itemStatus,
                stagnant,
                stagnantDaysGreaterThan,
                unitType,
                orgId
        ));
    }

    @GetMapping("/inventory-profit-loss/report")
    public CodeDataResponse<PageData<InventoryProfitLossReportRow>> inventoryProfitLossReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String dateRangeStart,
            @RequestParam(required = false) String dateRangeEnd,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String statisticsType,
            @RequestParam(required = false) String itemKeyword,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String profitLossResult,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.inventoryProfitLossReport(
                pageNo,
                pageSize,
                warehouse,
                dateRangeStart,
                dateRangeEnd,
                itemCategory,
                statisticsType,
                itemKeyword,
                checkType,
                profitLossResult,
                unitType,
                orgId
        ));
    }
}
