package com.boboboom.jxc.inventory.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.IdPayload;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.InventoryBalanceRow;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.PageData;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.PurchaseInboundDetail;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.PurchaseInboundPermissionView;
import com.boboboom.jxc.inventory.application.service.PurchaseInboundApplicationService.PurchaseInboundRow;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundCreateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/inventory")
public class PurchaseInboundController {

    private final PurchaseInboundApplicationService purchaseInboundApplicationService;

    public PurchaseInboundController(PurchaseInboundApplicationService purchaseInboundApplicationService) {
        this.purchaseInboundApplicationService = purchaseInboundApplicationService;
    }

    @GetMapping("/purchase-inbound")
    public CodeDataResponse<PageData<PurchaseInboundRow>> listPurchaseInbound(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String timeType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String documentStatus,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String reconciliationStatus,
            @RequestParam(required = false) String splitStatus,
            @RequestParam(required = false) String upstreamCode,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) String inspectionCount,
            @RequestParam(required = false) String printStatus,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.listPurchaseInbound(
                pageNo,
                pageSize,
                timeType,
                startDate,
                endDate,
                warehouse,
                documentCode,
                supplier,
                itemName,
                documentStatus,
                reviewStatus,
                reconciliationStatus,
                splitStatus,
                upstreamCode,
                invoiceStatus,
                inspectionCount,
                printStatus,
                remark,
                orgId));
    }

    @GetMapping("/purchase-inbound/permissions")
    public CodeDataResponse<PurchaseInboundPermissionView> purchaseInboundPermissions(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.purchaseInboundPermissions(orgId));
    }

    @PostMapping("/purchase-inbound")
    public CodeDataResponse<IdPayload> createPurchaseInbound(@RequestParam(required = false) String orgId,
                                                             @Valid @RequestBody PurchaseInboundCreateRequest request) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.createPurchaseInbound(orgId, request));
    }

    @GetMapping("/purchase-inbound/{id}")
    public CodeDataResponse<PurchaseInboundDetail> detailPurchaseInbound(@PathVariable Long id,
                                                                         @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.detailPurchaseInbound(id, orgId));
    }

    @PutMapping("/purchase-inbound/{id}")
    public CodeDataResponse<Void> updatePurchaseInbound(@PathVariable Long id,
                                                        @RequestParam(required = false) String orgId,
                                                        @Valid @RequestBody PurchaseInboundCreateRequest request) {
        purchaseInboundApplicationService.updatePurchaseInbound(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/purchase-inbound/{id}")
    public CodeDataResponse<Void> deletePurchaseInbound(@PathVariable Long id,
                                                        @RequestParam(required = false) String orgId) {
        purchaseInboundApplicationService.deletePurchaseInbound(id, orgId);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/purchase-inbound")
    public CodeDataResponse<Void> batchDeletePurchaseInbound(@RequestParam(required = false) String orgId,
                                                             @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchDeletePurchaseInbound(orgId, request);
        return CodeDataResponse.ok();
    }

    @PostMapping("/purchase-inbound/batch-approve")
    public CodeDataResponse<Void> batchApprove(@RequestParam(required = false) String orgId,
                                               @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchApprove(orgId, request);
        return CodeDataResponse.ok();
    }

    @PostMapping("/purchase-inbound/batch-unapprove")
    public CodeDataResponse<Void> batchUnapprove(@RequestParam(required = false) String orgId,
                                                  @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchUnapprove(orgId, request);
        return CodeDataResponse.ok();
    }

    @GetMapping("/balances")
    public CodeDataResponse<List<InventoryBalanceRow>> listBalances(@RequestParam(required = false) String warehouse,
                                                                    @RequestParam(required = false) String itemName,
                                                                    @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.listBalances(warehouse, itemName, orgId));
    }
}
