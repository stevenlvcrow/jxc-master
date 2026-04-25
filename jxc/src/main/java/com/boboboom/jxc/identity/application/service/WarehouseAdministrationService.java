package com.boboboom.jxc.identity.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseUpdateRequest;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class WarehouseAdministrationService {

    private static final String WAREHOUSE_CODE_PREFIX = "CKBM";

    private final WarehouseRepository warehouseRepository;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final DictionaryLookupService dictionaryLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public WarehouseAdministrationService(WarehouseRepository warehouseRepositoryValue,
                                          IdentityAdminLookupService identityAdminLookupServiceValue,
                                          BusinessCodeGenerator businessCodeGeneratorValue,
                                          DictionaryLookupService dictionaryLookupServiceValue) {
        this.warehouseRepository = warehouseRepositoryValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
        this.businessCodeGenerator = businessCodeGeneratorValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 查询集团仓库列表。 */
    public List<WarehouseDO> listGroupWarehouses(Long groupId,
                                                 String keyword,
                                                 String status,
                                                 String warehouseType) {
        return filterWarehouses(warehouseRepository.findByGroupId(groupId), keyword, status, warehouseType);
    }

    /** 查询门店仓库列表。 */
    public List<WarehouseDO> listStoreWarehouses(Long storeId,
                                                 String keyword,
                                                 String status,
                                                 String warehouseType) {
        return filterWarehouses(warehouseRepository.findByStoreId(storeId), keyword, status, warehouseType);
    }

    /** 创建仓库。 */
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
        warehouse.setWarehouseType(normalizeWarehouseType(request.getWarehouseType()));
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

    /** 更新仓库。 */
    @Transactional
    public WarehouseDO updateWarehouse(Long id, WarehouseUpdateRequest request) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);

        warehouse.setWarehouseName(identityAdminLookupService.trim(request.getWarehouseName()));
        warehouse.setDepartment(identityAdminLookupService.trimNullable(request.getDepartment()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            warehouse.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        }
        if (request.getWarehouseType() != null && !request.getWarehouseType().isBlank()) {
            warehouse.setWarehouseType(normalizeWarehouseType(request.getWarehouseType()));
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

    /** 删除仓库。 */
    @Transactional
    public void deleteWarehouse(Long id) {
        identityAdminLookupService.requireWarehouse(id);
        warehouseRepository.deleteById(id);
    }

    /** 设置WarehouseDefault。 */
    @Transactional
    public WarehouseDO setWarehouseDefault(Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        warehouseRepository.resetDefaultByStoreId(warehouse.getStoreId());
        warehouse.setIsDefault(true);
        warehouseRepository.update(warehouse);
        return warehouse;
    }

    /** 更新仓库状态。 */
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

    private String normalizeWarehouseType(String value) {
        String normalized = identityAdminLookupService.trimNullable(value);
        if (normalized == null) {
            return dictionaryLookupService.codeOf(DictionaryCodes.WAREHOUSE_TYPE, "NORMAL_WAREHOUSE");
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.WAREHOUSE_TYPE, normalized);
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
                .filter(warehouse -> matchesKeyword(warehouse, normalizedKeyword))
                .filter(warehouse -> matchesStatus(warehouse, normalizedStatus))
                .filter(warehouse -> matchesWarehouseType(warehouse, normalizedWarehouseType))
                .toList();
    }

    private boolean matchesKeyword(WarehouseDO warehouse, String keyword) {
        return keyword == null || keyword.isEmpty() || containsAny(warehouse, keyword);
    }

    private boolean matchesStatus(WarehouseDO warehouse, String status) {
        return status == null || status.isEmpty() || status.equals(warehouse.getStatus());
    }

    private boolean matchesWarehouseType(WarehouseDO warehouse, String warehouseType) {
        return warehouseType == null || warehouseType.isEmpty() || warehouseType.equals(warehouse.getWarehouseType());
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
