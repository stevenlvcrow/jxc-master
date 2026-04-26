package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

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

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.WarehouseAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;

import jakarta.validation.Valid;

/**
 * 仓库管理接口，负责分组仓库的查询、新增、更新与状态维护。
 */
@Validated
@RestController
@RequestMapping("/api/identity/admin")
public class IdentityWarehouseAdminController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final IdentityAccessControlService identityAccessControlService;
    private final WarehouseAdministrationService warehouseAdministrationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造仓库管理接口。
     *
     * @param identityAccessControlServiceValue 组织权限控制服务
     * @param warehouseAdministrationServiceValue 仓库管理服务
     * @param identityAdminLookupServiceValue 仓库查询辅助服务
     * @param identityAdminSupportValue 当前登录管理员辅助服务
     */
    public IdentityWarehouseAdminController(IdentityAccessControlService identityAccessControlServiceValue,
                                            WarehouseAdministrationService warehouseAdministrationServiceValue,
                                            IdentityAdminLookupService identityAdminLookupServiceValue,
                                            IdentityAdminSupport identityAdminSupportValue) {
        this.identityAccessControlService = identityAccessControlServiceValue;
        this.warehouseAdministrationService = warehouseAdministrationServiceValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
        this.identityAdminSupport = identityAdminSupportValue;
    }

    /**
     * 查询分组下的仓库列表。
     *
     * @param groupId 分组主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param status 状态
     * @param warehouseType 仓库类型
     * @return 仓库列表响应
     */
    @GetMapping("/groups/{groupId}/warehouses")
    public CodeDataResponse<PageData<WarehouseAdminView>> listGroupWarehouses(@PathVariable Long groupId,
                                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) String warehouseType) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        List<WarehouseAdminView> result = warehouseAdministrationService
                .listGroupWarehouses(groupId, keyword, status, warehouseType)
                .stream()
                .map(identityAdminLookupService::toWarehouseAdminSnapshot)
                .map(snapshot -> new WarehouseAdminView(
                        snapshot.id(),
                        snapshot.warehouseCode(),
                        snapshot.warehouseName(),
                        snapshot.department(),
                        snapshot.status(),
                        snapshot.warehouseType(),
                        snapshot.contactName(),
                        snapshot.contactPhone(),
                        snapshot.address(),
                        snapshot.targetGrossMargin(),
                        snapshot.idealPurchaseSaleRatio(),
                        snapshot.isDefault(),
                        snapshot.updatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 查询门店下的仓库列表。
     *
     * @param storeId 门店主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param status 状态
     * @param warehouseType 仓库类型
     * @return 仓库列表响应
     */
    @GetMapping("/stores/{storeId}/warehouses")
    public CodeDataResponse<PageData<WarehouseAdminView>> listStoreWarehouses(@PathVariable Long storeId,
                                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) String warehouseType) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        identityAccessControlService.ensureCanAccessStore(identityAdminSupport.currentOperatorId(), store.getId());
        List<WarehouseAdminView> result = warehouseAdministrationService
                .listStoreWarehouses(storeId, keyword, status, warehouseType)
                .stream()
                .map(identityAdminLookupService::toWarehouseAdminSnapshot)
                .map(snapshot -> new WarehouseAdminView(
                        snapshot.id(),
                        snapshot.warehouseCode(),
                        snapshot.warehouseName(),
                        snapshot.department(),
                        snapshot.status(),
                        snapshot.warehouseType(),
                        snapshot.contactName(),
                        snapshot.contactPhone(),
                        snapshot.address(),
                        snapshot.targetGrossMargin(),
                        snapshot.idealPurchaseSaleRatio(),
                        snapshot.isDefault(),
                        snapshot.updatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 当前接口保留创建仓库入口，但明确提示必须通过门店维度创建。
     *
     * @param groupId 分组主键
     * @param request 仓库新增请求
     * @return 不可用异常
     */
    @PostMapping("/groups/{groupId}/warehouses")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createWarehouse(@PathVariable Long groupId,
                                                       @Valid @RequestBody WarehouseCreateRequest request) {
        throw new BusinessException("请通过门店创建仓库");
    }

    /**
     * 新增门店仓库。
     *
     * @param storeId 门店主键
     * @param request 仓库新增请求
     * @return 新建结果
     */
    @PostMapping("/stores/{storeId}/warehouses")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createStoreWarehouse(@PathVariable Long storeId,
                                                            @Valid @RequestBody WarehouseCreateRequest request) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        identityAccessControlService.ensureCanAccessStore(identityAdminSupport.currentOperatorId(), store.getId());
        WarehouseDO warehouse = warehouseAdministrationService.createWarehouse(storeId, request);
        return CodeDataResponse.ok(new IdPayload(warehouse.getId()));
    }

    /**
     * 更新仓库信息。
     *
     * @param id 仓库主键
     * @param request 仓库更新请求
     * @return 空响应
     */
    @PutMapping("/warehouses/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateWarehouse(@PathVariable Long id,
                                                  @Valid @RequestBody WarehouseUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.updateWarehouse(id, request);
        return CodeDataResponse.ok(null);
    }

    /**
     * 删除仓库。
     *
     * @param id 仓库主键
     * @return 空响应
     */
    @DeleteMapping("/warehouses/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteWarehouse(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.deleteWarehouse(id);
        return CodeDataResponse.ok(null);
    }

    /**
     * 设置默认仓库。
     *
     * @param id 仓库主键
     * @return 空响应
     */
    @PutMapping("/warehouses/{id}/default")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> setWarehouseDefault(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.setWarehouseDefault(id);
        return CodeDataResponse.ok(null);
    }

    /**
     * 更新仓库状态。
     *
     * @param id 仓库主键
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/warehouses/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateWarehouseStatus(@PathVariable Long id,
                                                        @RequestBody StatusUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.updateWarehouseStatus(id, request.getStatus());
        return CodeDataResponse.ok(null);
    }

    /**
     * 校验当前操作者是否可以管理目标仓库。
     *
     * @param warehouse 仓库实体
     */
    private void ensureCanManageWarehouse(WarehouseDO warehouse) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        // 门店仓库按门店权限校验，避免出现可查看但不可维护的权限割裂。
        if (warehouse.getStoreId() != null) {
            identityAccessControlService.ensureCanAccessStore(operatorId, warehouse.getStoreId());
            return;
        }
        // 分组仓库直接按分组权限校验。
        identityAccessControlService.ensureCanManageGroup(operatorId, warehouse.getGroupId());
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }
}
