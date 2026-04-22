package com.boboboom.jxc.inventory.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import com.boboboom.jxc.inventory.application.service.InventoryCheckApplicationService;
import com.boboboom.jxc.inventory.application.service.InventoryCheckKind;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryCheckBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryCheckSaveRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * 盘点单接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory/checks")
public class InventoryCheckController {

    private final InventoryCheckApplicationService inventoryCheckApplicationService;

    public InventoryCheckController(InventoryCheckApplicationService inventoryCheckApplicationService) {
        this.inventoryCheckApplicationService = inventoryCheckApplicationService;
    }

    /**
     * 查询列表。
     */
    @GetMapping("/{documentType}")
    public CodeDataResponse<PageData<InventoryCheckApplicationService.InventoryCheckRow>> list(@PathVariable String documentType,
                                                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                                                               @RequestParam(required = false) String timeType,
                                                                                               @RequestParam(required = false) String startDate,
                                                                                               @RequestParam(required = false) String endDate,
                                                                                               @RequestParam(required = false) String warehouse,
                                                                                               @RequestParam(required = false) String documentCode,
                                                                                               @RequestParam(required = false) String itemName,
                                                                                               @RequestParam(required = false) String status,
                                                                                               @RequestParam(required = false) String checkRangeType,
                                                                                               @RequestParam(required = false) String printStatus,
                                                                                               @RequestParam(required = false) String generatedStatus,
                                                                                               @RequestParam(required = false) String remark,
                                                                                               @RequestParam(required = false) String orgId) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryCheckApplicationService.list(
                kind,
                pageNum,
                pageSize,
                timeType,
                startDate,
                endDate,
                warehouse,
                documentCode,
                itemName,
                status,
                checkRangeType,
                printStatus,
                generatedStatus,
                remark,
                orgId
        ));
    }

    /**
     * 查询权限。
     */
    @GetMapping("/{documentType}/permissions")
    public CodeDataResponse<InventoryCheckApplicationService.InventoryCheckPermissionView> permissions(@PathVariable String documentType,
                                                                                                        @RequestParam(required = false) String orgId) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryCheckApplicationService.permissions(kind, orgId));
    }

    /**
     * 创建单据。
     */
    @PostMapping("/{documentType}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<InventoryCheckApplicationService.IdPayload> create(@PathVariable String documentType,
                                                                              @RequestParam(required = false) String orgId,
                                                                              @Valid @RequestBody InventoryCheckSaveRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryCheckApplicationService.create(kind, orgId, request));
    }

    /**
     * 查询详情。
     */
    @GetMapping("/{documentType}/{id}")
    public CodeDataResponse<InventoryCheckApplicationService.InventoryCheckDetail> detail(@PathVariable String documentType,
                                                                                          @PathVariable Long id,
                                                                                          @RequestParam(required = false) String orgId) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryCheckApplicationService.detail(kind, id, orgId));
    }

    /**
     * 更新单据。
     */
    @PutMapping("/{documentType}/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody InventoryCheckSaveRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.update(kind, id, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 删除单据。
     */
    @DeleteMapping("/{documentType}/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> delete(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.delete(kind, id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除。
     */
    @DeleteMapping("/{documentType}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDelete(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.batchDelete(kind, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量提交。
     */
    @PostMapping("/{documentType}/batch-submit")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchSubmit(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.batchSubmit(kind, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量审核。
     */
    @PostMapping("/{documentType}/batch-approve")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchApprove(@PathVariable String documentType,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.batchApprove(kind, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量反审核。
     */
    @PostMapping("/{documentType}/batch-unapprove")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchUnapprove(@PathVariable String documentType,
                                                 @RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.batchUnapprove(kind, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量打印。
     */
    @PostMapping("/{documentType}/batch-print")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchPrint(@PathVariable String documentType,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.batchPrint(kind, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 生成多人盘点单。
     */
    @PostMapping("/{documentType}/generate")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> generate(@PathVariable String documentType,
                                           @RequestParam(required = false) String orgId,
                                           @Valid @RequestBody InventoryCheckBatchRequest request) {
        InventoryCheckKind kind = InventoryCheckKind.fromPathSegment(documentType);
        inventoryCheckApplicationService.generate(kind, orgId, request);
        return CodeDataResponse.ok();
    }
}
