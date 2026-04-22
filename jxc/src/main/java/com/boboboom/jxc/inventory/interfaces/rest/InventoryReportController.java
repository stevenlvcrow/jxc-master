package com.boboboom.jxc.inventory.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.application.service.InventoryReportApplicationService;
import com.boboboom.jxc.inventory.application.service.InventoryReportApplicationService.DishConsumptionOutboundReportPage;
import com.boboboom.jxc.inventory.application.service.InventoryReportApplicationService.InventoryInoutDetailReportRow;
import com.boboboom.jxc.inventory.application.service.InventoryReportApplicationService.PageData;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存报表接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory")
public class InventoryReportController {

    private final InventoryReportApplicationService inventoryReportApplicationService;

    public InventoryReportController(InventoryReportApplicationService inventoryReportApplicationService) {
        this.inventoryReportApplicationService = inventoryReportApplicationService;
    }

    @GetMapping("/dish-consumption-outbound/report")
    public CodeDataResponse<DishConsumptionOutboundReportPage> dishConsumptionOutboundReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String dishName,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String deductionType,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String queryScheme,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventoryReportApplicationService.dishConsumptionOutboundReport(
                pageNo,
                pageSize,
                dimension,
                startDate,
                endDate,
                warehouse,
                dishName,
                itemCode,
                deductionType,
                unitType,
                queryScheme,
                orgId
        ));
    }

    @GetMapping("/inout-detail/report")
    public CodeDataResponse<PageData<InventoryInoutDetailReportRow>> inventoryInoutDetailReport(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(required = false) String statisticDimension,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String dateText,
            @RequestParam(required = false) String auditStartTime,
            @RequestParam(required = false) String auditEndTime,
            @RequestParam(required = false) String inoutType,
            @RequestParam(required = false) String upstreamDocumentType,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String statisticType,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String reasonType,
            @RequestParam(required = false) String adjustmentDocument,
            @RequestParam(required = false) String oppositeOrg,
            @RequestParam(required = false) String documentNo,
            @RequestParam(required = false) String crossMonthDocument,
            @RequestParam(required = false) String inoutDirection,
            @RequestParam(required = false) String gift,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) String queryScheme,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(inventoryReportApplicationService.inventoryInoutDetailReport(
                pageNo,
                pageSize,
                statisticDimension,
                warehouse,
                warehouseType,
                startDate,
                endDate,
                dateText,
                auditStartTime,
                auditEndTime,
                inoutType,
                upstreamDocumentType,
                itemCategory,
                statisticType,
                itemCode,
                reasonType,
                adjustmentDocument,
                oppositeOrg,
                documentNo,
                crossMonthDocument,
                inoutDirection,
                gift,
                unitType,
                queryScheme,
                orgId
        ));
    }
}
