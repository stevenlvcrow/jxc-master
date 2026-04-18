package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IdentityAccessControlService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreRepository storeRepository;
    private final RoleRepository roleRepository;
    private final OrgScopeService orgScopeService;

    public IdentityAccessControlService(UserRoleRelRepository userRoleRelRepository,
                                        StoreRepository storeRepository,
                                        RoleRepository roleRepository,
                                        OrgScopeService orgScopeService) {
        this.userRoleRelRepository = userRoleRelRepository;
        this.storeRepository = storeRepository;
        this.roleRepository = roleRepository;
        this.orgScopeService = orgScopeService;
    }

    public List<Long> listManagedGroupIds(Long operatorId) {
        return userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(operatorId, "GROUP", STATUS_ENABLED)
                .stream()
                .map(UserRoleRelDO::getScopeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

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

    public void ensureCanManageGroup(Long operatorId, Long groupId) {
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            return;
        }
        RoleDO groupAdminRole = requireRoleByCode("GROUP_ADMIN");
        UserRoleRelDO rel = userRoleRelRepository.findByUserIdRoleAndScope(
                operatorId, groupAdminRole.getId(), "GROUP", groupId
        ).orElse(null);
        if (rel == null) {
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

    public Set<Long> listManageableRoleIds(Long operatorId) {
        List<Long> managedGroupIds = listManagedGroupIds(operatorId);
        if (managedGroupIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> managedStoreIds = new HashSet<>(listManagedStoreIds(new HashSet<>(managedGroupIds)));
        List<UserRoleRelDO> rels = userRoleRelRepository.findByStatusAndGroupOrStoreScopes(
                STATUS_ENABLED, new LinkedHashSet<>(managedGroupIds), managedStoreIds
        );
        return rels.stream()
                .map(UserRoleRelDO::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void ensureCanManageRole(Long operatorId, RoleDO role) {
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            return;
        }
        if (!"GROUP".equals(role.getRoleType()) && !"STORE".equals(role.getRoleType())) {
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

    public void ensureRoleMenuAssignable(Long operatorId, RoleDO role) {
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            return;
        }
        if (role != null && "GROUP_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException("集团管理员角色菜单权限仅允许平台管理员配置");
        }
    }

    private RoleDO requireRoleByCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("角色不存在: " + roleCode));
    }
}
