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
public class IdentityWarehouseAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final WarehouseAdministrationService warehouseAdministrationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

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
    public CodeDataResponse<IdPayload> createWarehouse(@PathVariable Long groupId,
                                                       @Valid @RequestBody WarehouseCreateRequest request) {
        throw new BusinessException("请通过门店创建仓库");
    }

    @PostMapping("/stores/{storeId}/warehouses")
    public CodeDataResponse<IdPayload> createStoreWarehouse(@PathVariable Long storeId,
                                                            @Valid @RequestBody WarehouseCreateRequest request) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), store.getGroupId());
        WarehouseDO warehouse = warehouseAdministrationService.createWarehouse(storeId, request);
        return CodeDataResponse.ok(new IdPayload(warehouse.getId()));
    }

    @PutMapping("/warehouses/{id}")
    public CodeDataResponse<Void> updateWarehouse(@PathVariable Long id,
                                                  @Valid @RequestBody WarehouseUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.updateWarehouse(id, request);
        return CodeDataResponse.ok(null);
    }

    @DeleteMapping("/warehouses/{id}")
    public CodeDataResponse<Void> deleteWarehouse(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.deleteWarehouse(id);
        return CodeDataResponse.ok(null);
    }

    @PutMapping("/warehouses/{id}/default")
    public CodeDataResponse<Void> setWarehouseDefault(@PathVariable Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.setWarehouseDefault(id);
        return CodeDataResponse.ok(null);
    }

    @PutMapping("/warehouses/{id}/status")
    public CodeDataResponse<Void> updateWarehouseStatus(@PathVariable Long id,
                                                        @RequestBody StatusUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        ensureCanManageWarehouse(warehouse);
        warehouseAdministrationService.updateWarehouseStatus(id, request.getStatus());
        return CodeDataResponse.ok(null);
    }

    private void ensureCanManageWarehouse(WarehouseDO warehouse) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        if (warehouse.getStoreId() != null) {
            StoreDO store = identityAdminLookupService.requireStore(warehouse.getStoreId());
            identityAccessControlService.ensureCanManageGroup(operatorId, store.getGroupId());
            return;
        }
        identityAccessControlService.ensureCanManageGroup(operatorId, warehouse.getGroupId());
    }
}
