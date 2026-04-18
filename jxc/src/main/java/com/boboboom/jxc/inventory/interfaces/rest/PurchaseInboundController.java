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
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/inventory")
/**
 * 采购入库接口，负责入库单、权限、库存余额和批量操作。
 */
public class PurchaseInboundController {

    private final PurchaseInboundApplicationService purchaseInboundApplicationService;

    /**
     * 构造采购入库接口。
     *
     * @param purchaseInboundApplicationService 采购入库服务
     */
    public PurchaseInboundController(PurchaseInboundApplicationService purchaseInboundApplicationService) {
        this.purchaseInboundApplicationService = purchaseInboundApplicationService;
    }

    /**
     * 分页查询采购入库单。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param timeType 时间类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param warehouse 仓库
     * @param documentCode 单据编码
     * @param supplier 供应商
     * @param itemName 商品名称
     * @param documentStatus 单据状态
     * @param reviewStatus 审核状态
     * @param reconciliationStatus 对账状态
     * @param splitStatus 拆分状态
     * @param upstreamCode 上游单号
     * @param invoiceStatus 发票状态
     * @param inspectionCount 验货次数
     * @param printStatus 打印状态
     * @param remark 备注
     * @param orgId 机构标识
     * @return 分页结果
     */
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
    /**
     * 查询采购入库权限。
     *
     * @param orgId 机构标识
     * @return 权限视图
     */
    public CodeDataResponse<PurchaseInboundPermissionView> purchaseInboundPermissions(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.purchaseInboundPermissions(orgId));
    }

    /**
     * 新建采购入库单。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping("/purchase-inbound")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createPurchaseInbound(@RequestParam(required = false) String orgId,
                                                             @Valid @RequestBody PurchaseInboundCreateRequest request) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.createPurchaseInbound(orgId, request));
    }

    @GetMapping("/purchase-inbound/{id}")
    /**
     * 查询采购入库单详情。
     *
     * @param id 单据主键
     * @param orgId 机构标识
     * @return 详情结果
     */
    public CodeDataResponse<PurchaseInboundDetail> detailPurchaseInbound(@PathVariable Long id,
                                                                         @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.detailPurchaseInbound(id, orgId));
    }

    /**
     * 更新采购入库单。
     *
     * @param id 单据主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/purchase-inbound/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updatePurchaseInbound(@PathVariable Long id,
                                                        @RequestParam(required = false) String orgId,
                                                        @Valid @RequestBody PurchaseInboundCreateRequest request) {
        purchaseInboundApplicationService.updatePurchaseInbound(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/purchase-inbound/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除采购入库单。
     *
     * @param id 单据主键
     * @param orgId 机构标识
     * @return 空响应
     */
    public CodeDataResponse<Void> deletePurchaseInbound(@PathVariable Long id,
                                                        @RequestParam(required = false) String orgId) {
        purchaseInboundApplicationService.deletePurchaseInbound(id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除采购入库单。
     *
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    @DeleteMapping("/purchase-inbound")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDeletePurchaseInbound(@RequestParam(required = false) String orgId,
                                                             @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchDeletePurchaseInbound(orgId, request);
        return CodeDataResponse.ok();
    }

    @PostMapping("/purchase-inbound/batch-approve")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 批量审核采购入库单。
     *
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    public CodeDataResponse<Void> batchApprove(@RequestParam(required = false) String orgId,
                                               @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchApprove(orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量反审核采购入库单。
     *
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    @PostMapping("/purchase-inbound/batch-unapprove")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchUnapprove(@RequestParam(required = false) String orgId,
                                                  @Valid @RequestBody PurchaseInboundBatchRequest request) {
        purchaseInboundApplicationService.batchUnapprove(orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 查询库存余额列表。
     *
     * @param warehouse 仓库
     * @param itemName 商品名称
     * @param orgId 机构标识
     * @return 库存余额列表
     */
    @GetMapping("/balances")
    public CodeDataResponse<PageData<InventoryBalanceRow>> listBalances(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                                        @RequestParam(required = false) String warehouse,
                                                                        @RequestParam(required = false) String itemName,
                                                                        @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseInboundApplicationService.listBalances(pageNum, pageSize, warehouse, itemName, orgId));
    }
}
