package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import com.boboboom.jxc.identity.interfaces.rest.response.OrgNodeResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgAdministrationService {

    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final OrgScopeService orgScopeService;

    public OrgAdministrationService(GroupRepository groupRepository,
                                    StoreRepository storeRepository,
                                    UserRoleRelRepository userRoleRelRepository,
                                    OrgScopeService orgScopeService) {
        this.groupRepository = groupRepository;
        this.storeRepository = storeRepository;
        this.userRoleRelRepository = userRoleRelRepository;
        this.orgScopeService = orgScopeService;
    }

    public List<OrgNodeResult> tree() {
        Long userId = AuthContextHolder.require().getUserId();
        boolean platformAdmin = orgScopeService.isPlatformAdmin(userId);
        List<GroupStoreSummary> groups = groupRepository.findActiveGroupStoreSummaries("ENABLED");

        Set<Long> directGroupScopeIds = new HashSet<>();
        Set<Long> storeScopeIds = new HashSet<>();
        if (!platformAdmin) {
            List<UserRoleRelDO> rels = userRoleRelRepository.findByUserIdAndStatus(userId, "ENABLED");
            for (UserRoleRelDO rel : rels) {
                if ("GROUP".equals(rel.getScopeType()) && rel.getScopeId() != null) {
                    directGroupScopeIds.add(rel.getScopeId());
                }
                if ("STORE".equals(rel.getScopeType()) && rel.getScopeId() != null) {
                    storeScopeIds.add(rel.getScopeId());
                }
            }
        }

        List<OrgNodeResult> result = new ArrayList<>();
        for (GroupStoreSummary group : groups) {
            boolean fullGroupAccess = platformAdmin || directGroupScopeIds.contains(group.getId());
            if (!fullGroupAccess && !platformAdmin) {
                result.addAll(listStoreNodes(group.getId(), storeScopeIds));
                continue;
            }
            OrgNodeResult groupNode = new OrgNodeResult();
            groupNode.setId("group-" + group.getId());
            groupNode.setName(group.getGroupName());
            groupNode.setMerchantNo(group.getGroupCode());
            groupNode.setCode(group.getGroupCode());
            groupNode.setCity("未知");
            groupNode.setType("group");
            groupNode.setChildren(fullGroupAccess
                    ? listStoreNodes(group.getId(), null)
                    : listStoreNodes(group.getId(), storeScopeIds));
            result.add(groupNode);
        }
        return result;
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
        if (marker > 0 && marker <= 4) {
            return address.substring(0, marker + 1);
        }
        return address.length() <= 8 ? address : address.substring(0, 8);
    }
}
