package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseAdministrationService {

    private static final String WAREHOUSE_CODE_PREFIX = "CKBM";

    private final WarehouseRepository warehouseRepository;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public WarehouseAdministrationService(WarehouseRepository warehouseRepository,
                                          IdentityAdminLookupService identityAdminLookupService,
                                          BusinessCodeGenerator businessCodeGenerator) {
        this.warehouseRepository = warehouseRepository;
        this.identityAdminLookupService = identityAdminLookupService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<WarehouseDO> listGroupWarehouses(Long groupId,
                                                 String keyword,
                                                 String status,
                                                 String warehouseType) {
        return filterWarehouses(warehouseRepository.findByGroupId(groupId), keyword, status, warehouseType);
    }

    public List<WarehouseDO> listStoreWarehouses(Long storeId,
                                                 String keyword,
                                                 String status,
                                                 String warehouseType) {
        return filterWarehouses(warehouseRepository.findByStoreId(storeId), keyword, status, warehouseType);
    }

    @Transactional
    public WarehouseDO createWarehouse(Long storeId, WarehouseCreateRequest request) {
        StoreDO store = identityAdminLookupService.requireStore(storeId);

        String code = generateWarehouseCode();
        if (warehouseRepository.findByWarehouseCode(code).isPresent()) {
            throw new BusinessException("仓库编码已存在");
        }

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setStoreId(store.getId());
        warehouse.setGroupId(store.getGroupId());
        warehouse.setWarehouseCode(code);
        warehouse.setWarehouseName(identityAdminLookupService.trim(request.getWarehouseName()));
        warehouse.setDepartment(identityAdminLookupService.trimNullable(request.getDepartment()));
        warehouse.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        warehouse.setWarehouseType(identityAdminLookupService.trimNullable(request.getWarehouseType()) != null
                ? identityAdminLookupService.trimNullable(request.getWarehouseType())
                : "普通仓库");
        warehouse.setContactName(identityAdminLookupService.trimNullable(request.getContactName()));
        warehouse.setContactPhone(identityAdminLookupService.trimNullable(request.getContactPhone()));
        warehouse.setRegionPath(identityAdminLookupService.trimNullable(request.getRegionPath()));
        warehouse.setAddress(identityAdminLookupService.trimNullable(request.getAddress()));
        warehouse.setTargetGrossMargin(identityAdminLookupService.trimNullable(request.getTargetGrossMargin()));
        warehouse.setIdealPurchaseSaleRatio(identityAdminLookupService.trimNullable(request.getIdealPurchaseSaleRatio()));
        warehouse.setIsDefault(false);
        warehouseRepository.save(warehouse);
        return warehouse;
    }

    @Transactional
    public WarehouseDO updateWarehouse(Long id, WarehouseUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);

        warehouse.setWarehouseName(identityAdminLookupService.trim(request.getWarehouseName()));
        warehouse.setDepartment(identityAdminLookupService.trimNullable(request.getDepartment()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            warehouse.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        }
        if (request.getWarehouseType() != null && !request.getWarehouseType().isBlank()) {
            warehouse.setWarehouseType(request.getWarehouseType());
        }
        warehouse.setContactName(identityAdminLookupService.trimNullable(request.getContactName()));
        warehouse.setContactPhone(identityAdminLookupService.trimNullable(request.getContactPhone()));
        warehouse.setRegionPath(identityAdminLookupService.trimNullable(request.getRegionPath()));
        warehouse.setAddress(identityAdminLookupService.trimNullable(request.getAddress()));
        warehouse.setTargetGrossMargin(identityAdminLookupService.trimNullable(request.getTargetGrossMargin()));
        warehouse.setIdealPurchaseSaleRatio(identityAdminLookupService.trimNullable(request.getIdealPurchaseSaleRatio()));
        warehouseRepository.update(warehouse);
        return warehouse;
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        identityAdminLookupService.requireWarehouse(id);
        warehouseRepository.deleteById(id);
    }

    @Transactional
    public WarehouseDO setWarehouseDefault(Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        warehouseRepository.resetDefaultByStoreId(warehouse.getStoreId());
        warehouse.setIsDefault(true);
        warehouseRepository.update(warehouse);
        return warehouse;
    }

    @Transactional
    public WarehouseDO updateWarehouseStatus(Long id, String status) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        warehouse.setStatus(identityAdminLookupService.normalizeStatus(status));
        warehouseRepository.update(warehouse);
        return warehouse;
    }

    private String generateWarehouseCode() {
        List<String> existingCodes = warehouseRepository.findAllWarehouseCodes();
        return businessCodeGenerator.nextCode(WAREHOUSE_CODE_PREFIX, existingCodes);
    }

    private List<WarehouseDO> filterWarehouses(List<WarehouseDO> warehouses,
                                               String keyword,
                                               String status,
                                               String warehouseType) {
        if (warehouses == null || warehouses.isEmpty()) {
            return List.of();
        }
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedStatus = status == null ? null : status.trim();
        String normalizedWarehouseType = warehouseType == null ? null : warehouseType.trim();
        return warehouses.stream()
                .filter(warehouse -> normalizedKeyword == null || normalizedKeyword.isEmpty()
                        || containsAny(warehouse, normalizedKeyword))
                .filter(warehouse -> normalizedStatus == null || normalizedStatus.isEmpty()
                        || normalizedStatus.equals(warehouse.getStatus()))
                .filter(warehouse -> normalizedWarehouseType == null || normalizedWarehouseType.isEmpty()
                        || normalizedWarehouseType.equals(warehouse.getWarehouseType()))
                .toList();
    }

    private boolean containsAny(WarehouseDO warehouse, String keyword) {
        return contains(warehouse.getWarehouseCode(), keyword)
                || contains(warehouse.getWarehouseName(), keyword)
                || contains(warehouse.getContactName(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
