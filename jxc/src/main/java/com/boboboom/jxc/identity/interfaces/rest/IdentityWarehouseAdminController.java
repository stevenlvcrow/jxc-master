package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.WarehouseAdministrationService;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin")
/**
 * 仓库管理接口，负责分组仓库的查询、新增、更新与状态维护。
 */
public class IdentityWarehouseAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final WarehouseAdministrationService warehouseAdministrationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造仓库管理接口。
     *
     * @param identityAccessControlService 组织权限控制服务
     * @param warehouseAdministrationService 仓库管理服务
     * @param identityAdminLookupService 仓库查询辅助服务
     * @param identityAdminSupport 当前登录管理员辅助服务
     */
    public IdentityWarehouseAdminController(IdentityAccessControlService identityAccessControlService,
                                            WarehouseAdministrationService warehouseAdministrationService,
                                            IdentityAdminLookupService identityAdminLookupService,
                                            IdentityAdminSupport identityAdminSupport) {
        this.identityAccessControlService = identityAccessControlService;
        this.warehouseAdministrationService = warehouseAdministrationService;
        this.identityAdminLookupService = identityAdminLookupService;
        this.identityAdminSupport = identityAdminSupport;
    }

    @GetMapping("/groups/{groupId}/warehouses")
    /**
     * 查询分组下的仓库列表。
     *
     * @param groupId 分组主键
     * @param keyword 关键字
     * @param status 状态
     * @param warehouseType 仓库类型
     * @return 仓库列表响应
     */
    public CodeDataResponse<List<WarehouseAdminView>> listGroupWarehouses(@PathVariable Long groupId,
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
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/stores/{storeId}/warehouses")
    /**
     * 查询门店下的仓库列表。
     *
     * @param storeId 门店主键
     * @param keyword 关键字
     * @param status 状态
     * @param warehouseType 仓库类型
     * @return 仓库列表响应
     */
    public CodeDataResponse<List<WarehouseAdminView>> listStoreWarehouses(@PathVariable Long storeId,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String status,
                                                                          @RequestParam(required = false) String warehouseType) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), store.getGroupId());
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
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/groups/{groupId}/warehouses")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 当前接口保留创建仓库入口，但明确提示必须通过门店维度创建。
     *
     * @param groupId 分组主键
     * @param request 仓库新增请求
     * @return 不可用异常
     */
    public CodeDataResponse<IdPayload> createWarehouse(@PathVariable Long groupId,
                                                       @Valid @RequestBody WarehouseCreateRequest request) {
        throw new BusinessException("请通过门店创建仓库");
    }

    @PostMapping("/stores/{storeId}/warehouses")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新增门店仓库。
     *
     * @param storeId 门店主键
     * @param request 仓库新增请求
     * @return 新建结果
     */
    public CodeDataResponse<IdPayload> createStoreWarehouse(@PathVariable Long storeId,
                                                            @Valid @RequestBody WarehouseCreateRequest request) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), store.getGroupId());
        WarehouseDO warehouse = warehouseAdministrationService.createWarehouse(storeId, request);
        return CodeDataResponse.ok(new IdPayload(warehouse.getId()));
    }

    @PutMapping("/warehouses/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新仓库信息。
     *
     * @param id 仓库主键
     * @param request 仓库更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateWarehouse(@PathVariable Long id,
                                                  @Valid @RequestBody WarehouseUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.updateWarehouse(id, request);
        return CodeDataResponse.ok(null);
    }

    @DeleteMapping("/warehouses/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除仓库。
     *
     * @param id 仓库主键
     * @return 空响应
     */
    public CodeDataResponse<Void> deleteWarehouse(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.deleteWarehouse(id);
        return CodeDataResponse.ok(null);
    }

    @PutMapping("/warehouses/{id}/default")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 设置默认仓库。
     *
     * @param id 仓库主键
     * @return 空响应
     */
    public CodeDataResponse<Void> setWarehouseDefault(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.setWarehouseDefault(id);
        return CodeDataResponse.ok(null);
    }

    @PutMapping("/warehouses/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新仓库状态。
     *
     * @param id 仓库主键
     * @param request 状态更新请求
     * @return 空响应
     */
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
        // 门店仓库先回溯到所属门店，再按门店所属分组校验权限。
        if (warehouse.getStoreId() != null) {
            StoreDO store = identityAdminLookupService.requireStore(warehouse.getStoreId());
            identityAccessControlService.ensureCanManageGroup(operatorId, store.getGroupId());
            return;
        }
        // 分组仓库直接按分组权限校验。
        identityAccessControlService.ensureCanManageGroup(operatorId, warehouse.getGroupId());
    }
}
