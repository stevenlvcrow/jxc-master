package com.boboboom.jxc.identity.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseAdministrationService {

    private static final String WAREHOUSE_CODE_PREFIX = "CKBM";

    private final WarehouseMapper warehouseMapper;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public WarehouseAdministrationService(WarehouseMapper warehouseMapper,
                                          IdentityAdminLookupService identityAdminLookupService,
                                          BusinessCodeGenerator businessCodeGenerator) {
        this.warehouseMapper = warehouseMapper;
        this.identityAdminLookupService = identityAdminLookupService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<WarehouseDO> listGroupWarehouses(Long groupId,
                                                 String keyword,
                                                 String status,
                                                 String warehouseType) {
        LambdaQueryWrapper<WarehouseDO> wrapper = new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getGroupId, groupId)
                .orderByDesc(WarehouseDO::getCreatedAt)
                .orderByDesc(WarehouseDO::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(WarehouseDO::getWarehouseCode, kw)
                    .or().like(WarehouseDO::getWarehouseName, kw)
                    .or().like(WarehouseDO::getContactName, kw));
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(WarehouseDO::getStatus, status);
        }
        if (warehouseType != null && !warehouseType.trim().isEmpty()) {
            wrapper.eq(WarehouseDO::getWarehouseType, warehouseType);
        }
        return warehouseMapper.selectList(wrapper);
    }

    public WarehouseDO createWarehouse(Long groupId, WarehouseCreateRequest request) {
        identityAdminLookupService.requireGroup(groupId);

        String code = generateWarehouseCode();
        WarehouseDO codeExists = warehouseMapper.selectOne(
                new LambdaQueryWrapper<WarehouseDO>().eq(WarehouseDO::getWarehouseCode, code).last("limit 1"));
        if (codeExists != null) {
            throw new BusinessException("仓库编码已存在");
        }

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setGroupId(groupId);
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
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

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
        warehouseMapper.updateById(warehouse);
        return warehouse;
    }

    public void deleteWarehouse(Long id) {
        identityAdminLookupService.requireWarehouse(id);
        warehouseMapper.deleteById(id);
    }

    public WarehouseDO setWarehouseDefault(Long id) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        WarehouseDO resetDefault = new WarehouseDO();
        resetDefault.setIsDefault(false);
        warehouseMapper.update(resetDefault, new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getGroupId, warehouse.getGroupId()));
        warehouse.setIsDefault(true);
        warehouseMapper.updateById(warehouse);
        return warehouse;
    }

    public WarehouseDO updateWarehouseStatus(Long id, String status) {
        WarehouseDO warehouse = identityAdminLookupService.requireWarehouse(id);
        warehouse.setStatus(identityAdminLookupService.normalizeStatus(status));
        warehouseMapper.updateById(warehouse);
        return warehouse;
    }

    private String generateWarehouseCode() {
        List<String> existingCodes = warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                        .select(WarehouseDO::getWarehouseCode))
                .stream()
                .map(WarehouseDO::getWarehouseCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(WAREHOUSE_CODE_PREFIX, existingCodes);
    }
}
