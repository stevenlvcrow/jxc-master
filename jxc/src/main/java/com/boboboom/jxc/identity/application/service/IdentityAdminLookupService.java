package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class IdentityAdminLookupService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final UserAccountRepository userAccountRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final WarehouseRepository warehouseRepository;

    public IdentityAdminLookupService(UserAccountRepository userAccountRepository,
                                      GroupRepository groupRepository,
                                      RoleRepository roleRepository,
                                      WarehouseRepository warehouseRepository) {
        this.userAccountRepository = userAccountRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public UserAccountDO requireUser(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public GroupDO requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("集团不存在"));
    }

    public RoleDO requireRoleByCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("角色不存在: " + roleCode));
    }

    public WarehouseDO requireWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("仓库不存在"));
    }

    public String normalizeStatus(String rawStatus) {
        String status = trimNullable(rawStatus);
        if (status == null) {
            return STATUS_ENABLED;
        }
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException("状态仅支持 ENABLED 或 DISABLED");
        }
        return status;
    }

    public String normalizePhone(String phone) {
        String value = trim(phone);
        if (value.length() < 6) {
            throw new BusinessException("手机号格式不正确");
        }
        return value;
    }

    public String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    public String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        for (RoleDO template : roleRepository.findBuiltinTemplateRoles()) {
            RoleDO existing = roleRepository.findByTenantGroupIdAndRoleCode(groupId, template.getRoleCode()).orElse(null);
            if (existing != null) {
                if (!STATUS_ENABLED.equals(existing.getStatus())) {
                    existing.setStatus(STATUS_ENABLED);
                    roleRepository.update(existing);
                }
                continue;
            }
            RoleDO role = new RoleDO();
            role.setTenantGroupId(groupId);
            role.setRoleCode(template.getRoleCode());
            role.setRoleName(template.getRoleName());
            role.setRoleType(template.getRoleType());
            role.setDataScopeType(template.getDataScopeType());
            role.setDescription(template.getDescription());
            role.setStatus(STATUS_ENABLED);
            role.setCreatedBy(operatorId);
            roleRepository.save(role);
        }
    }

    public WarehouseAdminSnapshot toWarehouseAdminSnapshot(WarehouseDO warehouse) {
        StringBuilder fullAddress = new StringBuilder();
        if (warehouse.getRegionPath() != null && !warehouse.getRegionPath().isBlank()) {
            fullAddress.append(warehouse.getRegionPath()).append(" ");
        }
        if (warehouse.getAddress() != null && !warehouse.getAddress().isBlank()) {
            fullAddress.append(warehouse.getAddress());
        }
        return new WarehouseAdminSnapshot(
                warehouse.getId(),
                warehouse.getWarehouseCode(),
                warehouse.getWarehouseName(),
                warehouse.getDepartment(),
                warehouse.getStatus(),
                warehouse.getWarehouseType(),
                warehouse.getContactName(),
                warehouse.getContactPhone(),
                fullAddress.toString().trim(),
                warehouse.getTargetGrossMargin(),
                warehouse.getIdealPurchaseSaleRatio(),
                warehouse.getIsDefault() != null && warehouse.getIsDefault(),
                warehouse.getUpdatedAt()
        );
    }

    public record WarehouseAdminSnapshot(Long id,
                                         String warehouseCode,
                                         String warehouseName,
                                         String department,
                                         String status,
                                         String warehouseType,
                                         String contactName,
                                         String contactPhone,
                                         String address,
                                         String targetGrossMargin,
                                         String idealPurchaseSaleRatio,
                                         boolean isDefault,
                                         LocalDateTime updatedAt) {
    }
}
