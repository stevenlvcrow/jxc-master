package com.boboboom.jxc.identity.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class GroupAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String GROUP_CODE_PREFIX = "JTBM";
    private static final String STORE_CODE_PREFIX = "MDBM";

    private final UserAccountMapper userAccountMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final GroupMapper groupMapper;
    private final StoreMapper storeMapper;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public GroupAdministrationService(UserAccountMapper userAccountMapper,
                                      UserRoleRelMapper userRoleRelMapper,
                                      GroupMapper groupMapper,
                                      StoreMapper storeMapper,
                                      IdentityAdminLookupService identityAdminLookupService,
                                      BusinessCodeGenerator businessCodeGenerator) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.groupMapper = groupMapper;
        this.storeMapper = storeMapper;
        this.identityAdminLookupService = identityAdminLookupService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public BindGroupAdminSnapshot bindGroupAdmin(GroupDO group,
                                                 Long operatorId,
                                                 String phone,
                                                 String realNameOrPhone) {
        UserAccountDO user = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccountDO>().eq(UserAccountDO::getPhone, phone).last("limit 1")
        );

        if (user == null) {
            user = new UserAccountDO();
            user.setUsername(phone);
            user.setRealName(realNameOrPhone);
            user.setPhone(phone);
            user.setPasswordHash(PasswordCodec.encode("123654"));
            user.setPasswordSalt(null);
            user.setStatus(STATUS_ENABLED);
            user.setSourceType("MANUAL");
            user.setFirstLoginChangedPwd(Boolean.FALSE);
            userAccountMapper.insert(user);
        } else if (realNameOrPhone != null && !realNameOrPhone.equals(user.getRealName())) {
            user.setRealName(realNameOrPhone);
            userAccountMapper.updateById(user);
        }

        RoleDO groupAdminRole = identityAdminLookupService.requireRoleByCode("GROUP_ADMIN");
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, user.getId())
                .eq(UserRoleRelDO::getRoleId, groupAdminRole.getId())
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .eq(UserRoleRelDO::getScopeId, group.getId())
                .last("limit 1"));
        if (rel == null) {
            rel = new UserRoleRelDO();
            rel.setUserId(user.getId());
            rel.setRoleId(groupAdminRole.getId());
            rel.setScopeType("GROUP");
            rel.setScopeId(group.getId());
            rel.setAssignedBy(operatorId);
            rel.setStatus(STATUS_ENABLED);
            userRoleRelMapper.insert(rel);
        } else if (!STATUS_ENABLED.equals(rel.getStatus())) {
            rel.setStatus(STATUS_ENABLED);
            userRoleRelMapper.updateById(rel);
        }

        return new BindGroupAdminSnapshot(
                group.getId(),
                group.getGroupName(),
                user.getId(),
                user.getPhone(),
                user.getRealName()
        );
    }

    public List<GroupDO> listGroups(Long operatorId, boolean platformAdmin) {
        if (platformAdmin) {
            return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                    .orderByDesc(GroupDO::getCreatedAt)
                    .orderByDesc(GroupDO::getId));
        }
        RoleDO groupAdminRole = identityAdminLookupService.requireRoleByCode("GROUP_ADMIN");
        List<Long> groupIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                        .eq(UserRoleRelDO::getUserId, operatorId)
                        .eq(UserRoleRelDO::getRoleId, groupAdminRole.getId())
                        .eq(UserRoleRelDO::getScopeType, "GROUP")
                        .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                        .isNotNull(UserRoleRelDO::getScopeId))
                .stream()
                .map(UserRoleRelDO::getScopeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .in(GroupDO::getId, groupIds)
                .orderByDesc(GroupDO::getCreatedAt)
                .orderByDesc(GroupDO::getId));
    }

    public GroupDO createGroup(GroupUpsertRequest request, Long operatorId) {
        String groupCode = generateGroupCode();
        GroupDO codeExists = groupMapper.selectOne(
                new LambdaQueryWrapper<GroupDO>().eq(GroupDO::getGroupCode, groupCode).last("limit 1")
        );
        if (codeExists != null) {
            throw new BusinessException("集团编码已存在");
        }
        GroupDO group = new GroupDO();
        group.setGroupCode(groupCode);
        group.setGroupName(identityAdminLookupService.trim(request.getGroupName()));
        group.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        group.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        groupMapper.insert(group);
        ensureGroupBuiltinRoles(group.getId(), operatorId);
        return group;
    }

    public GroupDO updateGroup(Long id, GroupUpsertRequest request) {
        GroupDO group = identityAdminLookupService.requireGroup(id);
        String groupCode = identityAdminLookupService.trim(request.getGroupCode());
        GroupDO codeExists = groupMapper.selectOne(new LambdaQueryWrapper<GroupDO>()
                .eq(GroupDO::getGroupCode, groupCode)
                .ne(GroupDO::getId, id)
                .last("limit 1"));
        if (codeExists != null) {
            throw new BusinessException("集团编码已存在");
        }
        group.setGroupCode(groupCode);
        group.setGroupName(identityAdminLookupService.trim(request.getGroupName()));
        group.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        group.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        groupMapper.updateById(group);
        return group;
    }

    public void deleteGroup(Long id) {
        identityAdminLookupService.requireGroup(id);
        Long storeCount = storeMapper.selectCount(new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getGroupId, id));
        if (storeCount != null && storeCount > 0) {
            throw new BusinessException("集团下存在门店，无法删除");
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .eq(UserRoleRelDO::getScopeId, id));
        groupMapper.deleteById(id);
    }

    public GroupDO updateGroupStatus(Long id, String status) {
        GroupDO group = identityAdminLookupService.requireGroup(id);
        group.setStatus(identityAdminLookupService.normalizeStatus(status));
        groupMapper.updateById(group);
        return group;
    }

    public List<com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO> listGroupStores(Long groupId) {
        return storeMapper.selectList(new LambdaQueryWrapper<com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO>()
                .eq(com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO::getGroupId, groupId)
                .orderByDesc(com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO::getCreatedAt)
                .orderByDesc(com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO::getId));
    }

    public StoreDO createGroupStore(Long groupId, GroupStoreCreateRequest request) {
        identityAdminLookupService.requireGroup(groupId);

        String storeCode = generateStoreCode();
        StoreDO codeExists = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getStoreCode, storeCode).last("limit 1")
        );
        if (codeExists != null) {
            throw new BusinessException("门店编码已存在");
        }

        StoreDO store = new StoreDO();
        store.setGroupId(groupId);
        store.setStoreCode(storeCode);
        store.setStoreName(identityAdminLookupService.trim(request.getStoreName()));
        store.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        store.setContactName(identityAdminLookupService.trimNullable(request.getContactName()));
        store.setContactPhone(identityAdminLookupService.trimNullable(request.getContactPhone()));
        store.setAddress(identityAdminLookupService.trimNullable(request.getAddress()));
        store.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        storeMapper.insert(store);
        return store;
    }

    public void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        identityAdminLookupService.ensureGroupBuiltinRoles(groupId, operatorId);
    }

    public List<GroupAdminCandidateSnapshot> listGroupAdminCandidates(Long groupId) {
        List<com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView> rows =
                storeMapper.selectStoreAdminViewByGroupId(groupId, STATUS_ENABLED);

        LinkedHashMap<Long, GroupAdminCandidateSnapshot> deduped = new LinkedHashMap<>();
        for (com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView row : rows) {
            if (row.getAdminUserId() == null) {
                continue;
            }
            deduped.putIfAbsent(
                    row.getAdminUserId(),
                    new GroupAdminCandidateSnapshot(
                            row.getAdminUserId(),
                            row.getAdminRealName(),
                            row.getAdminPhone(),
                            row.getStoreId(),
                            row.getStoreCode(),
                            row.getStoreName()
                    )
            );
        }
        return new ArrayList<>(deduped.values());
    }

    public record BindGroupAdminSnapshot(Long groupId,
                                         String groupName,
                                         Long userId,
                                         String phone,
                                         String realName) {
    }

    public record GroupAdminCandidateSnapshot(Long userId,
                                              String realName,
                                              String phone,
                                              Long storeId,
                                              String storeCode,
                                              String storeName) {
    }

    private String generateGroupCode() {
        List<String> existingCodes = groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                        .select(GroupDO::getGroupCode))
                .stream()
                .map(GroupDO::getGroupCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(GROUP_CODE_PREFIX, existingCodes);
    }

    private String generateStoreCode() {
        List<String> existingCodes = storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                        .select(StoreDO::getStoreCode))
                .stream()
                .map(StoreDO::getStoreCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(STORE_CODE_PREFIX, existingCodes);
    }
}
