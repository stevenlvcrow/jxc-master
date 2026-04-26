package com.boboboom.jxc.identity.application.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class IdentityAccessControlService {

    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;
    private static final String SCOPE_GROUP = "GROUP";
    private static final String DATA_SCOPE_GROUP = "GROUP";

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public IdentityAccessControlService(UserRoleRelRepository userRoleRelRepositoryValue,
                                        RoleRepository roleRepositoryValue,
                                        StoreRepository storeRepositoryValue,
                                        OrgScopeService orgScopeServiceValue,
                                        DictionaryLookupService dictionaryLookupServiceValue) {
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 查询Managed集团标识列表。 */
    public List<Long> listManagedGroupIds(Long operatorId) {
        return userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(operatorId, "GROUP", enabledStatus())
                .stream()
                .filter(this::isGroupManagementRel)
                .map(UserRoleRelDO::getScopeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 查询Managed门店标识列表。 */
    public List<Long> listManagedStoreIds(Set<Long> managedGroupIds) {
        if (managedGroupIds == null || managedGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return storeRepository.findByGroupIds(new java.util.ArrayList<>(managedGroupIds))
                .stream()
                .map(StoreDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 校验并保证CanManage集团满足业务规则。 */
    public void ensureCanManageGroup(Long operatorId, Long groupId) {
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            return;
        }
        boolean hasGroupScope = userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(operatorId, "GROUP", enabledStatus())
                .stream()
                .filter(this::isGroupManagementRel)
                .map(UserRoleRelDO::getScopeId)
                .filter(Objects::nonNull)
                .anyMatch(scopeId -> scopeId.equals(groupId));
        if (!hasGroupScope) {
            throw new BusinessException("当前账号无该集团管理权限");
        }
    }

    /**
     * 校验当前操作者是否可以访问目标门店。
     *
     * @param operatorId 操作者主键
     * @param storeId 门店主键
     */
    public void ensureCanAccessStore(Long operatorId, Long storeId) {
        orgScopeService.resolveAccessibleScope(operatorId, "store-" + storeId);
    }

    /** 查询Manageable角色标识列表。 */
    public Set<Long> listManageableRoleIds(Long operatorId) {
        List<Long> managedGroupIds = listManagedGroupIds(operatorId);
        if (managedGroupIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> managedStoreIds = new HashSet<>(listManagedStoreIds(new HashSet<>(managedGroupIds)));
        List<UserRoleRelDO> rels = userRoleRelRepository.findByStatusAndGroupOrStoreScopes(
                enabledStatus(), new LinkedHashSet<>(managedGroupIds), managedStoreIds
        );
        return rels.stream()
                .map(UserRoleRelDO::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 校验并保证CanManage角色满足业务规则。 */
    public void ensureCanManageRole(Long operatorId, RoleDO role) {
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            ensurePlatformTenantRole(role);
            return;
        }
        if (!isGroupOrStoreRole(role)) {
            throw new BusinessException("当前账号仅可操作集团/门店角色");
        }
        List<Long> managedGroupIds = listManagedGroupIds(operatorId);
        if (!managedGroupIds.isEmpty()
                && role.getTenantGroupId() != null
                && role.getTenantGroupId() > 0
                && managedGroupIds.contains(role.getTenantGroupId())) {
            return;
        }
        Set<Long> manageableRoleIds = listManageableRoleIds(operatorId);
        if (!manageableRoleIds.contains(role.getId())) {
            throw new BusinessException("当前账号无该角色操作权限");
        }
    }

    private void ensurePlatformTenantRole(RoleDO role) {
        if (role.getTenantGroupId() == null || role.getTenantGroupId() != 0L) {
            throw new BusinessException("当前账号仅可操作平台租户角色");
        }
    }

    private boolean isGroupOrStoreRole(RoleDO role) {
        return "GROUP".equals(role.getRoleType()) || "STORE".equals(role.getRoleType());
    }

    private boolean isGroupManagementRel(UserRoleRelDO rel) {
        if (rel == null || !SCOPE_GROUP.equals(rel.getScopeType()) || rel.getRoleId() == null) {
            return false;
        }
        return roleRepository.findById(rel.getRoleId())
                .filter(role -> SCOPE_GROUP.equals(role.getRoleType()))
                .filter(role -> DATA_SCOPE_GROUP.equals(role.getDataScopeType()))
                .isPresent();
    }

    /** 校验并保证角色菜单Assignable满足业务规则。 */
    public void ensureRoleMenuAssignable(Long operatorId, RoleDO role) {
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            if (role == null || role.getTenantGroupId() == null || role.getTenantGroupId() != 0L) {
                throw new BusinessException("当前账号仅可配置平台租户角色菜单");
            }
            return;
        }
        if (role != null && "GROUP_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException("集团管理员角色菜单权限仅允许平台管理员配置");
        }
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }
}
