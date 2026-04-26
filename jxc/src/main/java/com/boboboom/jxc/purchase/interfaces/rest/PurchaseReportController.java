package com.boboboom.jxc.purchase.interfaces.rest;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.purchase.application.service.PurchaseReportApplicationService;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PageData;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchaseOrderStatusTrackingRow;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchasePriceAnalysisReport;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchaseReturnStatusTrackingRow;

/** 采购报表接口，负责采购报表 HTTP 请求协议适配。 */
@Validated
@RestController
@RequestMapping("/api/purchase/reports")
public class PurchaseReportController {

    private final PurchaseReportApplicationService purchaseReportApplicationService;

    /** 采购报表接口，负责采购报表 HTTP 请求协议适配。 */
    public PurchaseReportController(PurchaseReportApplicationService purchaseReportApplicationServiceValue) {
        this.purchaseReportApplicationService = purchaseReportApplicationServiceValue;
    }

    /** 查询采购订单状态跟踪表。 */
    @GetMapping("/order-status-tracking")
    public CodeDataResponse<PageData<PurchaseOrderStatusTrackingRow>> purchaseOrderStatusTrackingReport(
            @RequestParam Map<String, String> params) {
        return CodeDataResponse.ok(purchaseReportApplicationService.purchaseOrderStatusTrackingReport(params));
    }

    /** 查询采购退货状态跟踪表。 */
    @GetMapping("/return-status-tracking")
    public CodeDataResponse<PageData<PurchaseReturnStatusTrackingRow>> purchaseReturnStatusTrackingReport(
            @RequestParam Map<String, String> params) {
        return CodeDataResponse.ok(purchaseReportApplicationService.purchaseReturnStatusTrackingReport(params));
    }

    /** 查询采购物品价格分析表。 */
    @GetMapping("/item-price-analysis")
    public CodeDataResponse<PurchasePriceAnalysisReport> purchaseItemPriceAnalysisReport(
            @RequestParam Map<String, String> params) {
        return CodeDataResponse.ok(purchaseReportApplicationService.purchaseItemPriceAnalysisReport(params));
    }
}
