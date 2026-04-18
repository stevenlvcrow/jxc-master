package com.boboboom.jxc.inventory.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentApplicationService;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentSaveRequest;
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
 * 通用库存单据接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory")
public class InventoryDocumentController {

    private final InventoryDocumentApplicationService inventoryDocumentApplicationService;

    public InventoryDocumentController(InventoryDocumentApplicationService inventoryDocumentApplicationService) {
        this.inventoryDocumentApplicationService = inventoryDocumentApplicationService;
    }

    /**
     * 查询列表。
     *
     * @param documentType 业务类型路径
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param documentCode 单据编号
     * @param primaryName 主体一
     * @param itemName 物品名称
     * @param status 单据状态
     * @param remark 备注
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping("/{documentType}")
    public CodeDataResponse<PageData<InventoryDocumentApplicationService.InventoryDocumentRow>> list(@PathVariable String documentType,
                                                                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                                                                     @RequestParam(required = false) String startDate,
                                                                                                     @RequestParam(required = false) String endDate,
                                                                                                     @RequestParam(required = false) String documentCode,
                                                                                                     @RequestParam(required = false) String primaryName,
                                                                                                     @RequestParam(required = false) String itemName,
                                                                                                     @RequestParam(required = false) String status,
                                                                                                     @RequestParam(required = false) String remark,
                                                                                                     @RequestParam(required = false) String orgId) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryDocumentApplicationService.list(
                type,
                pageNum,
                pageSize,
                startDate,
                endDate,
                documentCode,
                primaryName,
                itemName,
                status,
                remark,
                orgId
        ));
    }

    /**
     * 查询权限。
     *
     * @param documentType 业务类型路径
     * @param orgId 机构标识
     * @return 权限视图
     */
    @GetMapping("/{documentType}/permissions")
    public CodeDataResponse<InventoryDocumentApplicationService.InventoryDocumentPermissionView> permissions(@PathVariable String documentType,
                                                                                                             @RequestParam(required = false) String orgId) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryDocumentApplicationService.permissions(type, orgId));
    }

    /**
     * 创建单据。
     *
     * @param documentType 业务类型路径
     * @param orgId 机构标识
     * @param request 保存请求
     * @return 创建结果
     */
    @PostMapping("/{documentType}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<InventoryDocumentApplicationService.IdPayload> create(@PathVariable String documentType,
                                                                                  @RequestParam(required = false) String orgId,
                                                                                  @Valid @RequestBody InventoryDocumentSaveRequest request) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryDocumentApplicationService.create(type, orgId, request));
    }

    /**
     * 查询详情。
     *
     * @param documentType 业务类型路径
     * @param id 主键
     * @param orgId 机构标识
     * @return 详情
     */
    @GetMapping("/{documentType}/{id}")
    public CodeDataResponse<InventoryDocumentApplicationService.InventoryDocumentDetail> detail(@PathVariable String documentType,
                                                                                                @PathVariable Long id,
                                                                                                @RequestParam(required = false) String orgId) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        return CodeDataResponse.ok(inventoryDocumentApplicationService.detail(type, id, orgId));
    }

    /**
     * 更新单据。
     *
     * @param documentType 业务类型路径
     * @param id 主键
     * @param orgId 机构标识
     * @param request 保存请求
     * @return 空响应
     */
    @PutMapping("/{documentType}/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody InventoryDocumentSaveRequest request) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        inventoryDocumentApplicationService.update(type, id, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 删除单据。
     *
     * @param documentType 业务类型路径
     * @param id 主键
     * @param orgId 机构标识
     * @return 空响应
     */
    @DeleteMapping("/{documentType}/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> delete(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        inventoryDocumentApplicationService.delete(type, id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除。
     *
     * @param documentType 业务类型路径
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    @DeleteMapping("/{documentType}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDelete(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody InventoryDocumentBatchRequest request) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        inventoryDocumentApplicationService.batchDelete(type, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量审核。
     *
     * @param documentType 业务类型路径
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    @PostMapping("/{documentType}/batch-approve")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchApprove(@PathVariable String documentType,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody InventoryDocumentBatchRequest request) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        inventoryDocumentApplicationService.batchApprove(type, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量反审核。
     *
     * @param documentType 业务类型路径
     * @param orgId 机构标识
     * @param request 批量请求
     * @return 空响应
     */
    @PostMapping("/{documentType}/batch-unapprove")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchUnapprove(@PathVariable String documentType,
                                                 @RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody InventoryDocumentBatchRequest request) {
        InventoryDocumentType type = InventoryDocumentType.fromPathSegment(documentType);
        inventoryDocumentApplicationService.batchUnapprove(type, orgId, request);
        return CodeDataResponse.ok();
    }
}
