package com.boboboom.jxc.identity.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class IdentityAdminLookupService {

    private static final int MIN_PHONE_LENGTH = 6;

    private final UserAccountRepository userAccountRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final DictionaryLookupService dictionaryLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public IdentityAdminLookupService(UserAccountRepository userAccountRepositoryValue,
                                      GroupRepository groupRepositoryValue,
                                      RoleRepository roleRepositoryValue,
                                      StoreRepository storeRepositoryValue,
                                      WarehouseRepository warehouseRepositoryValue,
                                      DictionaryLookupService dictionaryLookupServiceValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.groupRepository = groupRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.warehouseRepository = warehouseRepositoryValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 按用户标识读取并校验用户存在。 */
    public UserAccountDO requireUser(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /** 按集团标识读取并校验集团存在。 */
    public GroupDO requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("集团不存在"));
    }

    /** 按角色编码读取并校验角色存在。 */
    public RoleDO requireRoleByCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("角色不存在: " + roleCode));
    }

    /** 按门店标识读取并校验门店存在。 */
    public StoreDO requireStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("门店不存在"));
    }

    /** 按仓库标识读取并校验仓库存在。 */
    public WarehouseDO requireWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("仓库不存在"));
    }

    /** 规范化并校验状态值。 */
    public String normalizeStatus(String rawStatus) {
        String status = trimNullable(rawStatus);
        if (status == null) {
            return enabledStatus();
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.COMMON_ENABLED_STATUS, status);
    }

    /** 读取启用状态编码。 */
    public String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }

    /** 读取停用状态编码。 */
    public String disabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.DISABLED);
    }

    /** 规范化手机号。 */
    public String normalizePhone(String phone) {
        String value = trim(phone);
        if (value.length() < MIN_PHONE_LENGTH) {
            throw new BusinessException("手机号格式不正确");
        }
        return value;
    }

    /** 裁剪必填字符串。 */
    public String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    /** 裁剪可空字符串。 */
    public String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 校验并保证集团Builtin角色满足业务规则。 */
    public void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        String enabledStatus = enabledStatus();
        for (RoleDO template : roleRepository.findBuiltinTemplateRoles(enabledStatus)) {
            RoleDO existing = roleRepository.findByTenantGroupIdAndRoleCode(groupId, template.getRoleCode()).orElse(null);
            if (existing != null) {
                if (!enabledStatus.equals(existing.getStatus())) {
                    existing.setStatus(enabledStatus);
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
            role.setStatus(enabledStatus);
            role.setCreatedBy(operatorId);
            roleRepository.save(role);
        }
    }

    /** 转换为仓库管理快照。 */
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

    /** 身份与权限快照模型，承载仓库管理快照查询结果。 */
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
