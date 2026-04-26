package com.boboboom.jxc.identity.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import com.boboboom.jxc.identity.interfaces.rest.response.OrgNodeResult;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class OrgAdministrationService {

    private static final int SHORT_CITY_MAX_LENGTH = 4;
    private static final int ADDRESS_FALLBACK_CITY_LENGTH = 8;

    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;
    private final OrgScopeService orgScopeService;
    private final IdentityAdminLookupService identityAdminLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public OrgAdministrationService(GroupRepository groupRepositoryValue,
                                    StoreRepository storeRepositoryValue,
                                    UserRoleRelRepository userRoleRelRepositoryValue,
                                    RoleRepository roleRepositoryValue,
                                    OrgScopeService orgScopeServiceValue,
                                    IdentityAdminLookupService identityAdminLookupServiceValue) {
        this.groupRepository = groupRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
    }

    /** 查询树形业务数据。 */
    public List<OrgNodeResult> tree() {
        Long userId = AuthContextHolder.require().getUserId();
        boolean platformAdmin = orgScopeService.isPlatformAdmin(userId);
        List<GroupStoreSummary> groups = groupRepository.findActiveGroupStoreSummaries(identityAdminLookupService.enabledStatus());

        Set<Long> directGroupScopeIds = new HashSet<>();
        Set<Long> storeScopeIds = new HashSet<>();
        if (!platformAdmin) {
            collectUserScopes(userId, directGroupScopeIds, storeScopeIds);
        }

        List<OrgNodeResult> result = new ArrayList<>();
        for (GroupStoreSummary group : groups) {
            boolean fullGroupAccess = platformAdmin || directGroupScopeIds.contains(group.getId());
            boolean hasScopedStores = storeScopeIds.stream()
                    .anyMatch(storeId -> storeRepository.findById(storeId)
                            .map(StoreDO::getGroupId)
                            .filter(group.getId()::equals)
                            .isPresent());
            if (!fullGroupAccess && !platformAdmin && !hasScopedStores) {
                continue;
            }
            OrgNodeResult groupNode = new OrgNodeResult();
            groupNode.setId("group-" + group.getId());
            groupNode.setName(group.getGroupName());
            groupNode.setMerchantNo(group.getGroupCode());
            groupNode.setCode(group.getGroupCode());
            groupNode.setCity("未知");
            groupNode.setType("group");
            groupNode.setSelectable(fullGroupAccess);
            groupNode.setChildren(fullGroupAccess
                    ? listStoreNodes(group.getId(), null)
                    : listStoreNodes(group.getId(), storeScopeIds));
            result.add(groupNode);
        }
        return result;
    }

    private void collectUserScopes(Long userId, Set<Long> directGroupScopeIds, Set<Long> storeScopeIds) {
        List<UserRoleRelDO> rels = userRoleRelRepository.findByUserIdAndStatus(userId, identityAdminLookupService.enabledStatus());
        for (UserRoleRelDO rel : rels) {
            addUserScope(rel, directGroupScopeIds, storeScopeIds);
        }
    }

    private void addUserScope(UserRoleRelDO rel, Set<Long> directGroupScopeIds, Set<Long> storeScopeIds) {
        if ("GROUP".equals(rel.getScopeType()) && rel.getScopeId() != null && isGroupWideRel(rel)) {
            directGroupScopeIds.add(rel.getScopeId());
        }
        if ("STORE".equals(rel.getScopeType()) && rel.getScopeId() != null) {
            storeScopeIds.add(rel.getScopeId());
        }
    }

    private boolean isGroupWideRel(UserRoleRelDO rel) {
        if (rel.getRoleId() == null) {
            return false;
        }
        return roleRepository.findById(rel.getRoleId())
                .filter(role -> "GROUP".equals(role.getRoleType()))
                .map(RoleDO::getDataScopeType)
                .filter("GROUP"::equals)
                .isPresent();
    }

    private List<OrgNodeResult> listStoreNodes(Long groupId, Set<Long> scopedStoreIds) {
        List<StoreDO> stores = storeRepository.findByGroupId(groupId);
        if (scopedStoreIds != null && !scopedStoreIds.isEmpty()) {
            stores = stores.stream()
                    .filter(store -> scopedStoreIds.contains(store.getId()))
                    .collect(Collectors.toList());
        }
        List<OrgNodeResult> children = new ArrayList<>(stores.size());
        for (StoreDO store : stores) {
            OrgNodeResult child = new OrgNodeResult();
            child.setId("store-" + store.getId());
            child.setName(store.getStoreName());
            child.setMerchantNo(store.getStoreCode());
            child.setCode(store.getStoreCode());
            child.setCity(extractCity(store.getAddress()));
            child.setType("store");
            child.setSelectable(Boolean.TRUE);
            child.setChildren(null);
            children.add(child);
        }
        return children;
    }

    private String extractCity(String address) {
        if (address == null || address.isBlank()) {
            return "未知";
        }
        int marker = address.indexOf("市");
        if (marker > 0 && marker <= SHORT_CITY_MAX_LENGTH) {
            return address.substring(0, marker + 1);
        }
        return address.length() <= ADDRESS_FALLBACK_CITY_LENGTH ? address : address.substring(0, ADDRESS_FALLBACK_CITY_LENGTH);
    }
}
