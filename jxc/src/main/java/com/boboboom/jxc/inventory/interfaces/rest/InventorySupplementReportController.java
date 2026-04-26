package com.boboboom.jxc.inventory.interfaces.rest;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.application.service.InventoryRealtimeReportApplicationService;
import com.boboboom.jxc.inventory.application.service.InventoryRealtimeReportApplicationService.RealtimeStockReportRow;
import com.boboboom.jxc.inventory.application.service.InventoryRealtimeReportApplicationService.StockWarningReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportApplicationService;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InterOrgTransferDetailReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InterOrgTransferSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InventoryInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InventoryProfitLossReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.OtherInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.PageData;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StagnantStockReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StockInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StockTurnoverRateReportRow;

import jakarta.validation.constraints.Min;

/**
 * 库存报表补充接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory")
public class InventorySupplementReportController {

    private final InventorySupplementReportApplicationService inventorySupplementReportApplicationService;
    private final InventoryRealtimeReportApplicationService inventoryRealtimeReportApplicationService;

    /** 库存接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public InventorySupplementReportController(InventorySupplementReportApplicationService inventorySupplementReportApplicationServiceValue,
                                               InventoryRealtimeReportApplicationService inventoryRealtimeReportApplicationServiceValue) {
        this.inventorySupplementReportApplicationService = inventorySupplementReportApplicationServiceValue;
        this.inventoryRealtimeReportApplicationService = inventoryRealtimeReportApplicationServiceValue;
    }

    /** 处理GetMapping。 */
    @GetMapping("/realtime-stock/report")
    public CodeDataResponse<InventoryRealtimeReportApplicationService.PageData<RealtimeStockReportRow>> realtimeStockReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String shelfLifeStatus,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventoryRealtimeReportApplicationService.realtimeStockReport(
                pageNo,
                pageSize,
                warehouse,
                itemCategory,
                itemCode,
                itemStatus,
                batchNo,
                shelfLifeStatus,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/stock-warning/report")
    public CodeDataResponse<InventoryRealtimeReportApplicationService.PageData<StockWarningReportRow>> stockWarningReport(
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
        return CodeDataResponse.ok(inventoryRealtimeReportApplicationService.stockWarningReport(
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

    /** 处理GetMapping。 */
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

    /** 处理GetMapping。 */
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

    /** 处理GetMapping。 */
    @GetMapping("/inventory-inout-summary/report")
    public CodeDataResponse<PageData<InventoryInoutSummaryReportRow>> inventoryInoutSummaryReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String periodStartDate,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String statisticType,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String inoutType,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String hideNoInout,
            @RequestParam(required = false) String queryScheme,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.inventoryInoutSummaryReport(
                pageNo,
                pageSize,
                statisticDimension,
                warehouse,
                warehouseType,
                startDate,
                endDate,
                periodType,
                periodStartDate,
                itemCode,
                itemCategory,
                statisticType,
                itemStatus,
                inoutType,
                unitType,
                hideNoInout,
                queryScheme,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/stock-inout-summary/report")
    public CodeDataResponse<PageData<StockInoutSummaryReportRow>> stockInoutSummaryReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticMode,
            @RequestParam(required = false) String dateDimension,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String periodStartDate,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) String targetStore,
            @RequestParam(required = false) String itemKeyword,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String statisticType,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String inoutType,
            @RequestParam(required = false) String inoutDirection,
            @RequestParam(required = false) String oppositeOrg,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String queryScheme,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.stockInoutSummaryReport(
                pageNo,
                pageSize,
                statisticMode,
                dateDimension,
                startDate,
                endDate,
                periodType,
                periodStartDate,
                statisticDimension,
                warehouse,
                warehouseType,
                targetStore,
                itemKeyword,
                itemCategory,
                statisticType,
                itemStatus,
                inoutType,
                inoutDirection,
                oppositeOrg,
                unitType,
                queryScheme,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/other-inout-summary/report")
    public CodeDataResponse<PageData<OtherInoutSummaryReportRow>> otherInoutSummaryReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String periodStartDate,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String inoutType,
            @RequestParam(required = false) String reasonType,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.otherInoutSummaryReport(
                pageNo,
                pageSize,
                startDate,
                endDate,
                periodType,
                periodStartDate,
                warehouse,
                itemCategory,
                itemCode,
                inoutType,
                reasonType,
                itemStatus,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/inter-org-transfer-detail/report")
    public CodeDataResponse<PageData<InterOrgTransferDetailReportRow>> interOrgTransferDetailReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticMode,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String targetStore,
            @RequestParam(required = false) String sourceStore,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String sourceWarehouse,
            @RequestParam(required = false) String targetWarehouse,
            @RequestParam(required = false) String documentStatus,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.interOrgTransferDetailReport(
                pageNo,
                pageSize,
                statisticMode,
                dateType,
                startDate,
                endDate,
                targetStore,
                sourceStore,
                itemName,
                itemCategory,
                sourceWarehouse,
                targetWarehouse,
                documentStatus,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/inter-org-transfer-summary/report")
    public CodeDataResponse<PageData<InterOrgTransferSummaryReportRow>> interOrgTransferSummaryReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticMode,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String targetStore,
            @RequestParam(required = false) String itemKeyword,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String queryScheme,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.interOrgTransferSummaryReport(
                pageNo,
                pageSize,
                statisticMode,
                dateType,
                startDate,
                endDate,
                statisticDimension,
                targetStore,
                itemKeyword,
                itemCategory,
                unitType,
                queryScheme,
                orgId
        ));
    }

    /** 处理GetMapping。 */
    @GetMapping("/stock-turnover-rate/report")
    public CodeDataResponse<PageData<StockTurnoverRateReportRow>> stockTurnoverRateReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String statisticMethod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String periodStartDate,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventorySupplementReportApplicationService.stockTurnoverRateReport(
                pageNo,
                pageSize,
                statisticDimension,
                statisticMethod,
                startDate,
                endDate,
                periodType,
                periodStartDate,
                warehouse,
                itemCategory,
                itemCode,
                itemStatus,
                unitType,
                orgId
        ));
    }
}
