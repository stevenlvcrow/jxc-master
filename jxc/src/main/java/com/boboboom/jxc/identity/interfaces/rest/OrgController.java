package com.boboboom.jxc.identity.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.OrgNodeResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping({"/org", "/api/identity/org"})
public class OrgController {

    private final GroupMapper groupMapper;
    private final StoreMapper storeMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final RoleMapper roleMapper;

    public OrgController(GroupMapper groupMapper,
                         StoreMapper storeMapper,
                         UserRoleRelMapper userRoleRelMapper,
                         RoleMapper roleMapper) {
        this.groupMapper = groupMapper;
        this.storeMapper = storeMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.roleMapper = roleMapper;
    }

    @GetMapping("/tree")
    public CodeDataResponse<List<OrgNodeResult>> tree() {
        LoginSession session = AuthContextHolder.require();
        Long userId = session.getUserId();
        boolean platformAdmin = isPlatformAdmin(userId);
        List<GroupStoreSummary> groups = groupMapper.selectGroupStoreSummary("ENABLED");

        Set<Long> directGroupScopeIds = new HashSet<>();
        Set<Long> storeScopeIds = new HashSet<>();
        if (!platformAdmin) {
            List<UserRoleRelDO> rels = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                    .eq(UserRoleRelDO::getUserId, userId)
                    .eq(UserRoleRelDO::getStatus, "ENABLED")
                    .and(wrapper -> wrapper
                            .and(groupScoped -> groupScoped
                                    .eq(UserRoleRelDO::getScopeType, "GROUP")
                                    .isNotNull(UserRoleRelDO::getScopeId))
                            .or(storeScoped -> storeScoped
                                    .eq(UserRoleRelDO::getScopeType, "STORE")
                                    .isNotNull(UserRoleRelDO::getScopeId))));
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
                // 仅门店权限账号：不返回集团节点，只返回可见门店节点
                List<OrgNodeResult> scopedStores = listStoreNodes(group.getId(), storeScopeIds);
                result.addAll(scopedStores);
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
        return CodeDataResponse.ok(result);
    }

    private List<OrgNodeResult> listStoreNodes(Long groupId, Set<Long> scopedStoreIds) {
        List<StoreDO> stores = storeMapper.selectList(
                Wrappers.<StoreDO>lambdaQuery()
                        .eq(StoreDO::getGroupId, groupId)
                        .eq(StoreDO::getStatus, "ENABLED")
                        .orderByAsc(StoreDO::getId)
        );
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

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, "PLATFORM")
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
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
