package com.boboboom.jxc.identity.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMenuRelMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UnitMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleMenuAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupAdminBindRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserRoleAssignRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemCategoryMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemStatisticsTypeMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemTagMapper;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/identity/admin")
public class IdentityAdminController {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String GROUP_ROLE_TEMPLATE_DESC = "GROUP_ROLE_TEMPLATE";
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("GROUP_ADMIN", "STORE_ADMIN");

    private final UserAccountMapper userAccountMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuRelMapper roleMenuRelMapper;
    private final MenuMapper menuMapper;
    private final GroupMapper groupMapper;
    private final StoreMapper storeMapper;
    private final UnitMapper unitMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemStatisticsTypeMapper itemStatisticsTypeMapper;
    private final ItemTagMapper itemTagMapper;

    public IdentityAdminController(UserAccountMapper userAccountMapper,
                                   UserRoleRelMapper userRoleRelMapper,
                                   RoleMapper roleMapper,
                                   RoleMenuRelMapper roleMenuRelMapper,
                                   MenuMapper menuMapper,
                                   GroupMapper groupMapper,
                                   StoreMapper storeMapper,
                                   UnitMapper unitMapper,
                                   ItemCategoryMapper itemCategoryMapper,
                                   ItemStatisticsTypeMapper itemStatisticsTypeMapper,
                                   ItemTagMapper itemTagMapper) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.roleMapper = roleMapper;
        this.roleMenuRelMapper = roleMenuRelMapper;
        this.menuMapper = menuMapper;
        this.groupMapper = groupMapper;
        this.storeMapper = storeMapper;
        this.unitMapper = unitMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.itemStatisticsTypeMapper = itemStatisticsTypeMapper;
        this.itemTagMapper = itemTagMapper;
    }

    @GetMapping("/users")
    public CodeDataResponse<List<UserAdminView>> listUsers() {
        Long operatorId = currentOperatorId();
        List<UserAccountDO> users;
        if (isPlatformAdmin(operatorId)) {
            users = userAccountMapper.selectList(
                    new LambdaQueryWrapper<UserAccountDO>()
                            .orderByDesc(UserAccountDO::getCreatedAt)
                            .orderByDesc(UserAccountDO::getId)
            );
        } else {
            List<Long> managedGroupIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                            .eq(UserRoleRelDO::getUserId, operatorId)
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                            .isNotNull(UserRoleRelDO::getScopeId))
                    .stream()
                    .map(UserRoleRelDO::getScopeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (managedGroupIds.isEmpty()) {
                return CodeDataResponse.ok(Collections.emptyList());
            }

            List<Long> managedStoreIds = storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                            .select(StoreDO::getId)
                            .in(StoreDO::getGroupId, managedGroupIds))
                    .stream()
                    .map(StoreDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            LambdaQueryWrapper<UserRoleRelDO> scopeRelQuery = new LambdaQueryWrapper<UserRoleRelDO>()
                    .select(UserRoleRelDO::getUserId)
                    .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                    .and(wrapper -> {
                        wrapper.and(groupScoped -> groupScoped
                                        .eq(UserRoleRelDO::getScopeType, "GROUP")
                                        .in(UserRoleRelDO::getScopeId, managedGroupIds));
                        if (!managedStoreIds.isEmpty()) {
                            wrapper.or(storeScoped -> storeScoped
                                    .eq(UserRoleRelDO::getScopeType, "STORE")
                                    .in(UserRoleRelDO::getScopeId, managedStoreIds));
                        }
                    });

            List<Long> scopedUserIds = userRoleRelMapper.selectList(scopeRelQuery).stream()
                    .map(UserRoleRelDO::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<Long> unassignedUserIds = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                            .select(UserAccountDO::getId)
                            .notInSql(UserAccountDO::getId, "select distinct user_id from sys_user_role_rel where status = 'ENABLED'"))
                    .stream()
                    .map(UserAccountDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            LinkedHashSet<Long> visibleUserIds = new LinkedHashSet<>(scopedUserIds);
            visibleUserIds.addAll(unassignedUserIds);
            if (visibleUserIds.isEmpty()) {
                return CodeDataResponse.ok(Collections.emptyList());
            }

            users = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                    .in(UserAccountDO::getId, visibleUserIds)
                    .orderByDesc(UserAccountDO::getCreatedAt)
                    .orderByDesc(UserAccountDO::getId));
        }

        List<UserAdminView> result = new ArrayList<>(users.size());
        for (UserAccountDO user : users) {
            List<UserRoleView> userRoles = userAccountMapper.selectUserRoles(user.getId());
            List<RoleAssignmentView> roles = userRoles.stream()
                    .map(v -> new RoleAssignmentView(
                            v.getRoleId(),
                            v.getRoleCode(),
                            v.getRoleName(),
                            v.getRoleType(),
                            v.getScopeType(),
                            v.getScopeId(),
                            v.getScopeName()
                    ))
                    .toList();
            result.add(new UserAdminView(
                    user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    user.getPhone(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    roles
            ));
        }
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/groups")
    public CodeDataResponse<List<GroupAdminView>> listGroups() {
        List<GroupDO> groups;
        Long operatorId = currentOperatorId();
        if (isPlatformAdmin(operatorId)) {
            groups = groupMapper.selectList(
                    new LambdaQueryWrapper<GroupDO>()
                            .orderByDesc(GroupDO::getCreatedAt)
                            .orderByDesc(GroupDO::getId)
            );
        } else {
            RoleDO groupAdminRole = requireRoleByCode("GROUP_ADMIN");
            List<UserRoleRelDO> rels = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                    .eq(UserRoleRelDO::getUserId, operatorId)
                    .eq(UserRoleRelDO::getRoleId, groupAdminRole.getId())
                    .eq(UserRoleRelDO::getScopeType, "GROUP")
                    .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                    .isNotNull(UserRoleRelDO::getScopeId));
            List<Long> groupIds = rels.stream()
                    .map(UserRoleRelDO::getScopeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (groupIds.isEmpty()) {
                groups = Collections.emptyList();
            } else {
                groups = groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                        .in(GroupDO::getId, groupIds)
                        .orderByDesc(GroupDO::getCreatedAt)
                        .orderByDesc(GroupDO::getId));
            }
        }
        List<GroupAdminView> result = groups.stream()
                .map(group -> new GroupAdminView(
                        group.getId(),
                        group.getGroupCode(),
                        group.getGroupName(),
                        group.getStatus(),
                        group.getRemark(),
                        group.getCreatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/groups")
    @Transactional
    public CodeDataResponse<IdPayload> createGroup(@Valid @RequestBody GroupUpsertRequest request) {
        requirePlatformAdmin();
        String groupCode = trim(request.getGroupCode());
        GroupDO codeExists = groupMapper.selectOne(
                new LambdaQueryWrapper<GroupDO>().eq(GroupDO::getGroupCode, groupCode).last("limit 1")
        );
        if (codeExists != null) {
            throw new BusinessException("集团编码已存在");
        }

        GroupDO group = new GroupDO();
        group.setGroupCode(groupCode);
        group.setGroupName(trim(request.getGroupName()));
        group.setStatus(normalizeStatus(request.getStatus()));
        group.setRemark(trimNullable(request.getRemark()));
        groupMapper.insert(group);
        ensureGroupBuiltinRoles(group.getId(), currentOperatorId());
        return CodeDataResponse.ok(new IdPayload(group.getId()));
    }

    @PutMapping("/groups/{id}")
    @Transactional
    public CodeDataResponse<Void> updateGroup(@PathVariable Long id,
                                              @Valid @RequestBody GroupUpsertRequest request) {
        if (!isPlatformAdmin(currentOperatorId())) {
            ensureCanManageGroup(id);
        }
        GroupDO group = requireGroup(id);
        String groupCode = trim(request.getGroupCode());
        GroupDO codeExists = groupMapper.selectOne(
                new LambdaQueryWrapper<GroupDO>()
                        .eq(GroupDO::getGroupCode, groupCode)
                        .ne(GroupDO::getId, id)
                        .last("limit 1")
        );
        if (codeExists != null) {
            throw new BusinessException("集团编码已存在");
        }
        group.setGroupCode(groupCode);
        group.setGroupName(trim(request.getGroupName()));
        group.setStatus(normalizeStatus(request.getStatus()));
        group.setRemark(trimNullable(request.getRemark()));
        groupMapper.updateById(group);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/groups/{id}")
    @Transactional
    public CodeDataResponse<Void> deleteGroup(@PathVariable Long id) {
        requirePlatformAdmin();
        requireGroup(id);
        Long storeCount = storeMapper.selectCount(new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getGroupId, id));
        if (storeCount != null && storeCount > 0) {
            throw new BusinessException("集团下存在门店，无法删除");
        }
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .eq(UserRoleRelDO::getScopeId, id));
        groupMapper.deleteById(id);
        return CodeDataResponse.ok();
    }

    @PutMapping("/groups/{id}/status")
    @Transactional
    public CodeDataResponse<Void> updateGroupStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusUpdateRequest request) {
        if (!isPlatformAdmin(currentOperatorId())) {
            ensureCanManageGroup(id);
        }
        GroupDO group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("集团不存在");
        }
        group.setStatus(normalizeStatus(request.getStatus()));
        groupMapper.updateById(group);
        return CodeDataResponse.ok();
    }

    @PostMapping("/groups/{groupId}/bind-admin")
    @Transactional
    public CodeDataResponse<BindGroupAdminResult> bindGroupAdmin(@PathVariable Long groupId,
                                                                 @Valid @RequestBody GroupAdminBindRequest request) {
        requirePlatformAdmin();
        GroupDO group = requireGroup(groupId);

        String phone = normalizePhone(request.getPhone());
        UserAccountDO user = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccountDO>().eq(UserAccountDO::getPhone, phone).last("limit 1")
        );

        if (user == null) {
            user = new UserAccountDO();
            user.setUsername(phone);
            user.setRealName(trimNullable(request.getRealName()) == null ? phone : trim(request.getRealName()));
            user.setPhone(phone);
            user.setPasswordHash(PasswordCodec.encode("123654"));
            user.setPasswordSalt(null);
            user.setStatus(STATUS_ENABLED);
            user.setSourceType("MANUAL");
            user.setFirstLoginChangedPwd(Boolean.FALSE);
            userAccountMapper.insert(user);
        } else if (trimNullable(request.getRealName()) != null) {
            user.setRealName(trim(request.getRealName()));
            userAccountMapper.updateById(user);
        }

        RoleDO groupAdminRole = requireRoleByCode("GROUP_ADMIN");
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, user.getId())
                .eq(UserRoleRelDO::getRoleId, groupAdminRole.getId())
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .eq(UserRoleRelDO::getScopeId, groupId)
                .last("limit 1"));
        if (rel == null) {
            rel = new UserRoleRelDO();
            rel.setUserId(user.getId());
            rel.setRoleId(groupAdminRole.getId());
            rel.setScopeType("GROUP");
            rel.setScopeId(groupId);
            rel.setAssignedBy(currentOperatorId());
            rel.setStatus(STATUS_ENABLED);
            userRoleRelMapper.insert(rel);
        } else if (!STATUS_ENABLED.equals(rel.getStatus())) {
            rel.setStatus(STATUS_ENABLED);
            userRoleRelMapper.updateById(rel);
        }

        return CodeDataResponse.ok(new BindGroupAdminResult(
                group.getId(),
                group.getGroupName(),
                user.getId(),
                user.getPhone(),
                user.getRealName()
        ));
    }

    @GetMapping("/groups/{groupId}/stores")
    public CodeDataResponse<List<StoreAdminView>> listGroupStores(@PathVariable Long groupId) {
        ensureCanManageGroup(groupId);
        List<StoreDO> stores = storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .eq(StoreDO::getGroupId, groupId)
                .orderByDesc(StoreDO::getCreatedAt)
                .orderByDesc(StoreDO::getId));
        List<StoreAdminView> result = stores.stream()
                .map(store -> new StoreAdminView(
                        store.getId(),
                        store.getGroupId(),
                        store.getStoreCode(),
                        store.getStoreName(),
                        store.getStatus(),
                        store.getContactName(),
                        store.getContactPhone(),
                        store.getAddress(),
                        store.getRemark(),
                        store.getCreatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/groups/{groupId}/admin-candidates")
    public CodeDataResponse<List<GroupAdminCandidateView>> listGroupAdminCandidates(@PathVariable Long groupId) {
        ensureCanManageGroup(groupId);
        List<com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView> rows =
                storeMapper.selectStoreAdminViewByGroupId(groupId, STATUS_ENABLED);

        LinkedHashMap<Long, GroupAdminCandidateView> deduped = new LinkedHashMap<>();
        for (com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView row : rows) {
            if (row.getAdminUserId() == null) {
                continue;
            }
            deduped.putIfAbsent(
                    row.getAdminUserId(),
                    new GroupAdminCandidateView(
                            row.getAdminUserId(),
                            row.getAdminRealName(),
                            row.getAdminPhone(),
                            row.getStoreId(),
                            row.getStoreCode(),
                            row.getStoreName()
                    )
            );
        }
        return CodeDataResponse.ok(new ArrayList<>(deduped.values()));
    }

    @PostMapping("/groups/{groupId}/stores")
    @Transactional
    public CodeDataResponse<IdPayload> createGroupStore(@PathVariable Long groupId,
                                                        @Valid @RequestBody GroupStoreCreateRequest request) {
        ensureCanManageGroup(groupId);
        requireGroup(groupId);

        String storeCode = trim(request.getStoreCode());
        StoreDO codeExists = storeMapper.selectOne(
                new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getStoreCode, storeCode).last("limit 1")
        );
        if (codeExists != null) {
            throw new BusinessException("门店编码已存在");
        }

        StoreDO store = new StoreDO();
        store.setGroupId(groupId);
        store.setStoreCode(storeCode);
        store.setStoreName(trim(request.getStoreName()));
        store.setStatus(normalizeStatus(request.getStatus()));
        store.setContactName(trimNullable(request.getContactName()));
        store.setContactPhone(trimNullable(request.getContactPhone()));
        store.setAddress(trimNullable(request.getAddress()));
        store.setRemark(trimNullable(request.getRemark()));
        storeMapper.insert(store);
        initializeStoreSampleData(store.getId());
        return CodeDataResponse.ok(new IdPayload(store.getId()));
    }

    private void initializeStoreSampleData(Long storeId) {
        copyUnitSamplesToStore(storeId);
        copyItemCategorySamplesToStore(storeId);
        copyStatisticsTypeSamplesToStore(storeId);
        copyItemTagSamplesToStore(storeId);
    }

    private void copyUnitSamplesToStore(Long storeId) {
        List<UnitDO> templates = unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, "PLATFORM")
                .eq(UnitDO::getScopeId, 0L)
                .orderByAsc(UnitDO::getCreatedAt)
                .orderByAsc(UnitDO::getId));
        if (templates.isEmpty()) {
            return;
        }
        List<UnitDO> existingRows = unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, "STORE")
                .eq(UnitDO::getScopeId, storeId)
                .select(UnitDO::getUnitCode, UnitDO::getUnitName));
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (UnitDO row : existingRows) {
            if (row.getUnitCode() != null) {
                existingCodes.add(row.getUnitCode());
            }
            if (row.getUnitName() != null) {
                existingNames.add(row.getUnitName());
            }
        }

        for (UnitDO template : templates) {
            if (existingCodes.contains(template.getUnitCode()) || existingNames.contains(template.getUnitName())) {
                continue;
            }
            UnitDO toInsert = new UnitDO();
            toInsert.setScopeType("STORE");
            toInsert.setScopeId(storeId);
            toInsert.setUnitCode(template.getUnitCode());
            toInsert.setUnitName(template.getUnitName());
            toInsert.setUnitType(template.getUnitType());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            unitMapper.insert(toInsert);
            existingCodes.add(template.getUnitCode());
            existingNames.add(template.getUnitName());
        }
    }

    private void copyItemCategorySamplesToStore(Long storeId) {
        List<ItemCategoryDO> templates = itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, "PLATFORM")
                .eq(ItemCategoryDO::getScopeId, 0L)
                .orderByAsc(ItemCategoryDO::getCreatedAt)
                .orderByAsc(ItemCategoryDO::getId));
        if (templates.isEmpty()) {
            return;
        }
        List<ItemCategoryDO> existingRows = itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, "STORE")
                .eq(ItemCategoryDO::getScopeId, storeId)
                .select(ItemCategoryDO::getCategoryCode, ItemCategoryDO::getCategoryName));
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (ItemCategoryDO row : existingRows) {
            if (row.getCategoryCode() != null) {
                existingCodes.add(row.getCategoryCode());
            }
            if (row.getCategoryName() != null) {
                existingNames.add(row.getCategoryName());
            }
        }

        for (ItemCategoryDO template : templates) {
            if (existingCodes.contains(template.getCategoryCode()) || existingNames.contains(template.getCategoryName())) {
                continue;
            }
            ItemCategoryDO toInsert = new ItemCategoryDO();
            toInsert.setScopeType("STORE");
            toInsert.setScopeId(storeId);
            toInsert.setCategoryCode(template.getCategoryCode());
            toInsert.setCategoryName(template.getCategoryName());
            toInsert.setParentCategory(template.getParentCategory());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            itemCategoryMapper.insert(toInsert);
            existingCodes.add(template.getCategoryCode());
            existingNames.add(template.getCategoryName());
        }
    }

    private void copyStatisticsTypeSamplesToStore(Long storeId) {
        List<ItemStatisticsTypeDO> templates = itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, "PLATFORM")
                .eq(ItemStatisticsTypeDO::getScopeId, 0L)
                .orderByAsc(ItemStatisticsTypeDO::getCreatedAt)
                .orderByAsc(ItemStatisticsTypeDO::getId));
        if (templates.isEmpty()) {
            return;
        }
        List<ItemStatisticsTypeDO> existingRows = itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, "STORE")
                .eq(ItemStatisticsTypeDO::getScopeId, storeId)
                .select(ItemStatisticsTypeDO::getCode, ItemStatisticsTypeDO::getName, ItemStatisticsTypeDO::getStatisticsCategory));
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNameCategoryPairs = new HashSet<>();
        for (ItemStatisticsTypeDO row : existingRows) {
            if (row.getCode() != null) {
                existingCodes.add(row.getCode());
            }
            existingNameCategoryPairs.add((row.getName() == null ? "" : row.getName()) + "#" +
                    (row.getStatisticsCategory() == null ? "" : row.getStatisticsCategory()));
        }

        for (ItemStatisticsTypeDO template : templates) {
            String pair = (template.getName() == null ? "" : template.getName()) + "#" +
                    (template.getStatisticsCategory() == null ? "" : template.getStatisticsCategory());
            if (existingCodes.contains(template.getCode()) || existingNameCategoryPairs.contains(pair)) {
                continue;
            }
            ItemStatisticsTypeDO toInsert = new ItemStatisticsTypeDO();
            toInsert.setScopeType("STORE");
            toInsert.setScopeId(storeId);
            toInsert.setCode(template.getCode());
            toInsert.setName(template.getName());
            toInsert.setStatisticsCategory(template.getStatisticsCategory());
            toInsert.setCreateType(template.getCreateType());
            itemStatisticsTypeMapper.insert(toInsert);
            existingCodes.add(template.getCode());
            existingNameCategoryPairs.add(pair);
        }
    }

    private void copyItemTagSamplesToStore(Long storeId) {
        List<ItemTagDO> templates = itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, "PLATFORM")
                .eq(ItemTagDO::getScopeId, 0L)
                .orderByAsc(ItemTagDO::getCreatedAt)
                .orderByAsc(ItemTagDO::getId));
        if (templates.isEmpty()) {
            return;
        }
        List<ItemTagDO> existingRows = itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, "STORE")
                .eq(ItemTagDO::getScopeId, storeId)
                .select(ItemTagDO::getTagCode, ItemTagDO::getTagName));
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (ItemTagDO row : existingRows) {
            if (row.getTagCode() != null) {
                existingCodes.add(row.getTagCode());
            }
            if (row.getTagName() != null) {
                existingNames.add(row.getTagName());
            }
        }

        for (ItemTagDO template : templates) {
            if (existingCodes.contains(template.getTagCode()) || existingNames.contains(template.getTagName())) {
                continue;
            }
            ItemTagDO toInsert = new ItemTagDO();
            toInsert.setScopeType("STORE");
            toInsert.setScopeId(storeId);
            toInsert.setTagCode(template.getTagCode());
            toInsert.setTagName(template.getTagName());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            itemTagMapper.insert(toInsert);
            existingCodes.add(template.getTagCode());
            existingNames.add(template.getTagName());
        }
    }

    @PostMapping("/users")
    @Transactional
    public CodeDataResponse<IdPayload> createUser(@Valid @RequestBody UserUpsertRequest request) {
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        if (!platformAdmin && listManagedGroupIds(operatorId).isEmpty()) {
            throw new BusinessException("当前账号无用户创建权限");
        }
        String phone = normalizePhone(request.getPhone());
        UserAccountDO exists = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccountDO>().eq(UserAccountDO::getPhone, phone).last("limit 1")
        );
        if (exists != null) {
            throw new BusinessException("手机号已存在");
        }

        UserAccountDO user = new UserAccountDO();
        user.setUsername(phone);
        user.setRealName(trim(request.getRealName()));
        user.setPhone(phone);
        user.setPasswordHash(PasswordCodec.encode("123654"));
        user.setPasswordSalt(null);
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setSourceType("MANUAL");
        user.setFirstLoginChangedPwd(Boolean.FALSE);
        userAccountMapper.insert(user);
        return CodeDataResponse.ok(new IdPayload(user.getId()));
    }

    @PutMapping("/users/{id}/status")
    @Transactional
    public CodeDataResponse<Void> updateUserStatus(@PathVariable Long id,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        if (!platformAdmin) {
            Set<Long> managedGroupIds = new HashSet<>(listManagedGroupIds(operatorId));
            Set<Long> managedStoreIds = new HashSet<>(listManagedStoreIds(managedGroupIds));
            ensureCanManageUser(id, managedGroupIds, managedStoreIds);
        }
        UserAccountDO user = requireUser(id);
        user.setStatus(normalizeStatus(request.getStatus()));
        userAccountMapper.updateById(user);
        return CodeDataResponse.ok();
    }

    @PutMapping("/users/{id}/roles")
    @Transactional
    public CodeDataResponse<Void> assignUserRoles(@PathVariable Long id,
                                                  @Valid @RequestBody UserRoleAssignRequest request) {
        requireUser(id);
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        Set<Long> managedGroupIds = platformAdmin
                ? Collections.emptySet()
                : new HashSet<>(listManagedGroupIds(operatorId));
        Set<Long> managedStoreIds = platformAdmin
                ? Collections.emptySet()
                : new HashSet<>(listManagedStoreIds(managedGroupIds));
        if (!platformAdmin) {
            if (hasEnabledRoleAssignments(id)) {
                ensureCanManageUser(id, managedGroupIds, managedStoreIds);
            }
        }

        List<UserRoleRelDO> toInsert = new ArrayList<>();
        LinkedHashMap<String, UserRoleAssignRequest.UserRoleAssignment> deduped = new LinkedHashMap<>();
        for (UserRoleAssignRequest.UserRoleAssignment assignment : request.getAssignments()) {
            if (assignment.getRoleId() == null) {
                throw new BusinessException("角色ID不能为空");
            }
            RoleDO role = roleMapper.selectById(assignment.getRoleId());
            if (role == null) {
                throw new BusinessException("角色不存在");
            }
            if (!platformAdmin && "PLATFORM".equals(role.getRoleType())) {
                throw new BusinessException("当前账号无平台角色授权权限");
            }
            String scopeType = normalizeScopeType(assignment.getScopeType(), role.getRoleType());
            Long scopeId = normalizeScopeId(scopeType, assignment.getScopeId());
            if (!platformAdmin && !isAllowedScope(scopeType, scopeId, managedGroupIds, managedStoreIds)) {
                throw new BusinessException("包含无权限授权范围");
            }
            String key = assignment.getRoleId() + ":" + scopeType + ":" + String.valueOf(scopeId);
            if (deduped.containsKey(key)) {
                continue;
            }
            deduped.put(key, assignment);

            UserRoleRelDO rel = new UserRoleRelDO();
            rel.setUserId(id);
            rel.setRoleId(assignment.getRoleId());
            rel.setScopeType(scopeType);
            rel.setScopeId(scopeId);
            rel.setAssignedBy(operatorId);
            rel.setStatus(STATUS_ENABLED);
            toInsert.add(rel);
        }

        if (platformAdmin) {
            userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRelDO>().eq(UserRoleRelDO::getUserId, id));
        } else {
            LambdaQueryWrapper<UserRoleRelDO> deleteQuery = new LambdaQueryWrapper<UserRoleRelDO>()
                    .eq(UserRoleRelDO::getUserId, id);
            if (!managedGroupIds.isEmpty() && !managedStoreIds.isEmpty()) {
                deleteQuery.and(wrapper -> wrapper
                        .and(groupScoped -> groupScoped
                                .eq(UserRoleRelDO::getScopeType, "GROUP")
                                .in(UserRoleRelDO::getScopeId, managedGroupIds))
                        .or(storeScoped -> storeScoped
                                .eq(UserRoleRelDO::getScopeType, "STORE")
                                .in(UserRoleRelDO::getScopeId, managedStoreIds)));
            } else if (!managedGroupIds.isEmpty()) {
                deleteQuery.eq(UserRoleRelDO::getScopeType, "GROUP")
                        .in(UserRoleRelDO::getScopeId, managedGroupIds);
            } else {
                deleteQuery.eq(UserRoleRelDO::getScopeType, "STORE")
                        .in(UserRoleRelDO::getScopeId, managedStoreIds);
            }
            userRoleRelMapper.delete(deleteQuery);
        }

        for (UserRoleRelDO rel : toInsert) {
            userRoleRelMapper.insert(rel);
        }
        return CodeDataResponse.ok();
    }

    @GetMapping("/roles")
    public CodeDataResponse<List<RoleAdminView>> listRoles() {
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        Set<Long> managedGroupIdsForDisplay = platformAdmin
                ? Collections.emptySet()
                : new LinkedHashSet<>(listManagedGroupIds(operatorId));
        List<RoleDO> roles;
        if (platformAdmin) {
            roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                    .orderByDesc(RoleDO::getCreatedAt)
                    .orderByDesc(RoleDO::getId));
        } else {
            if (managedGroupIdsForDisplay.isEmpty()) {
                return CodeDataResponse.ok(Collections.emptyList());
            }
            for (Long groupId : managedGroupIdsForDisplay) {
                ensureGroupBuiltinRoles(groupId, operatorId);
            }
            Set<Long> manageableRoleIds = listManageableRoleIds(operatorId);
            Set<Long> tenantRoleIds = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                            .select(RoleDO::getId)
                            .in(RoleDO::getTenantGroupId, managedGroupIdsForDisplay)
                            .in(RoleDO::getRoleType, List.of("GROUP", "STORE")))
                    .stream()
                    .map(RoleDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            tenantRoleIds.addAll(manageableRoleIds);
            if (tenantRoleIds.isEmpty()) {
                return CodeDataResponse.ok(Collections.emptyList());
            }
            roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                    .in(RoleDO::getId, tenantRoleIds)
                    .in(RoleDO::getRoleType, List.of("GROUP", "STORE"))
                    .orderByDesc(RoleDO::getCreatedAt)
                    .orderByDesc(RoleDO::getId));
        }

        List<RoleAdminView> result = new ArrayList<>(roles.size());
        for (RoleDO role : roles) {
            List<Long> menuIds = roleMenuRelMapper.selectByRoleId(role.getId()).stream()
                    .map(RoleMenuRelDO::getMenuId)
                    .distinct()
                    .toList();
            String displayRoleCode = platformAdmin
                    ? role.getRoleCode()
                    : toTenantDisplayRoleCode(role.getRoleCode(), managedGroupIdsForDisplay);
            result.add(new RoleAdminView(
                    role.getId(),
                    displayRoleCode,
                    role.getRoleName(),
                    role.getRoleType(),
                    role.getDataScopeType(),
                    role.getDescription(),
                    role.getStatus(),
                    menuIds,
                    isRoleBuiltin(role),
                    isRoleMutable(role)
            ));
        }
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/roles")
    @Transactional
    public CodeDataResponse<IdPayload> createRole(@Valid @RequestBody RoleUpsertRequest request) {
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        String roleType = trim(request.getRoleType());
        if (!platformAdmin && !"GROUP".equals(roleType) && !"STORE".equals(roleType)) {
            throw new BusinessException("集团账号仅可创建集团/门店角色");
        }
        String inputRoleCode = trim(request.getRoleCode());
        String persistentRoleCode = inputRoleCode;
        Long tenantGroupId = 0L;
        if (!platformAdmin) {
            List<Long> managedGroups = listManagedGroupIds(operatorId);
            if (managedGroups.isEmpty()) {
                throw new BusinessException("当前账号无可管理集团，无法创建角色");
            }
            tenantGroupId = managedGroups.get(0);
        }
        RoleDO exists = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleDO>()
                        .eq(RoleDO::getTenantGroupId, tenantGroupId)
                        .eq(RoleDO::getRoleCode, persistentRoleCode)
                        .last("limit 1")
        );
        if (exists != null) {
            throw new BusinessException("角色编码已存在（租户内唯一）");
        }

        RoleDO role = new RoleDO();
        role.setTenantGroupId(tenantGroupId);
        role.setRoleCode(persistentRoleCode);
        role.setRoleName(trim(request.getRoleName()));
        role.setRoleType(roleType);
        role.setDataScopeType(trim(request.getDataScopeType()));
        role.setDescription(trimNullable(request.getDescription()));
        role.setStatus(normalizeStatus(request.getStatus()));
        role.setCreatedBy(operatorId);
        roleMapper.insert(role);

        validateRoleMenusByType(role, request.getMenuIds());
        saveRoleMenus(role.getId(), request.getMenuIds());
        return CodeDataResponse.ok(new IdPayload(role.getId()));
    }

    @PutMapping("/roles/{id}")
    @Transactional
    public CodeDataResponse<Void> updateRole(@PathVariable Long id,
                                             @Valid @RequestBody RoleUpsertRequest request) {
        Long operatorId = currentOperatorId();
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        ensureCanManageRole(operatorId, role);
        ensureRoleMutable(role);
        String roleType = trim(request.getRoleType());
        if (!isPlatformAdmin(operatorId) && !"GROUP".equals(roleType) && !"STORE".equals(roleType)) {
            throw new BusinessException("集团账号仅可设置集团/门店角色");
        }
        role.setRoleName(trim(request.getRoleName()));
        role.setRoleType(roleType);
        role.setDataScopeType(trim(request.getDataScopeType()));
        role.setDescription(trimNullable(request.getDescription()));
        role.setStatus(normalizeStatus(request.getStatus()));
        roleMapper.updateById(role);

        validateRoleMenusByType(role, request.getMenuIds());
        saveRoleMenus(id, request.getMenuIds());
        return CodeDataResponse.ok();
    }

    @PutMapping("/roles/{id}/status")
    @Transactional
    public CodeDataResponse<Void> updateRoleStatus(@PathVariable Long id,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = currentOperatorId();
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        ensureCanManageRole(operatorId, role);
        String nextStatus = normalizeStatus(request.getStatus());
        if (STATUS_DISABLED.equals(nextStatus)) {
            ensureRoleMutable(role);
        }
        role.setStatus(nextStatus);
        roleMapper.updateById(role);
        return CodeDataResponse.ok();
    }

    @PutMapping("/roles/{id}/menus")
    @Transactional
    public CodeDataResponse<Void> assignRoleMenus(@PathVariable Long id,
                                                   @Valid @RequestBody RoleMenuAssignRequest request) {
        Long operatorId = currentOperatorId();
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        ensureCanManageRole(operatorId, role);
        ensureRoleMenuAssignable(operatorId, role);
        validateRoleMenusByType(role, request.getMenuIds());
        saveRoleMenus(id, request.getMenuIds());
        return CodeDataResponse.ok();
    }

    @GetMapping("/menus")
    public CodeDataResponse<List<MenuAdminView>> listMenus() {
        ensureGroupMgmtMenusForGroupAdmin();
        Long operatorId = currentOperatorId();
        boolean platformAdmin = isPlatformAdmin(operatorId);
        List<MenuAdminView> result;
        if (platformAdmin) {
            List<MenuDO> menus = menuMapper.selectList(
                    new LambdaQueryWrapper<MenuDO>()
                            .orderByAsc(MenuDO::getSortNo)
                            .orderByAsc(MenuDO::getId)
            );
            result = menus.stream()
                    .map(menu -> new MenuAdminView(
                            menu.getId(),
                            menu.getMenuCode(),
                            menu.getMenuName(),
                            menu.getParentId(),
                            menu.getMenuType(),
                            menu.getRoutePath(),
                            menu.getPermissionCode(),
                            menu.getStatus(),
                            menu.getSortNo()
                    ))
                    .toList();
        } else {
            List<Long> managedGroups = listManagedGroupIds(operatorId);
            if (managedGroups.isEmpty()) {
                result = Collections.emptyList();
            } else {
                List<MenuPermissionView> scopedMenus = new ArrayList<>();
                for (Long groupId : managedGroups) {
                    scopedMenus.addAll(menuMapper.selectMenusByUserContext(operatorId, "GROUP", groupId));
                }
                List<Long> managedStoreIds = listManagedStoreIds(new LinkedHashSet<>(managedGroups));
                for (Long storeId : managedStoreIds) {
                    scopedMenus.addAll(menuMapper.selectMenusByUserContext(operatorId, "STORE", storeId));
                }
                LinkedHashMap<Long, MenuAdminView> deduped = new LinkedHashMap<>();
                for (MenuPermissionView row : scopedMenus) {
                    if (!"ENABLED".equals(row.getStatus())) {
                        continue;
                    }
                    deduped.putIfAbsent(row.getId(), new MenuAdminView(
                            row.getId(),
                            row.getMenuCode(),
                            row.getMenuName(),
                            row.getParentId(),
                            row.getMenuType(),
                            row.getRoutePath(),
                            row.getPermissionCode(),
                            row.getStatus(),
                            row.getSortNo()
                    ));
                }
                result = new ArrayList<>(deduped.values());
            }
        }
        return CodeDataResponse.ok(result);
    }

    private void ensureGroupMgmtMenusForGroupAdmin() {
        MenuDO groupMgmt = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, "GROUP_MGMT")
                .last("limit 1"));
        if (groupMgmt == null) {
            return;
        }

        MenuDO userMgmt = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, "GROUP_USER_ROLE_MGMT")
                .last("limit 1"));
        if (userMgmt == null) {
            userMgmt = new MenuDO();
            userMgmt.setMenuCode("GROUP_USER_ROLE_MGMT");
            userMgmt.setMenuName("用户管理");
            userMgmt.setParentId(groupMgmt.getId());
            userMgmt.setMenuType("MENU");
            userMgmt.setRoutePath("/group/user-role");
            userMgmt.setComponentPath("group/user-role/index");
            userMgmt.setPermissionCode("group:user-role:manage");
            userMgmt.setIcon("user");
            userMgmt.setSortNo(46);
            userMgmt.setVisible(Boolean.TRUE);
            userMgmt.setStatus(STATUS_ENABLED);
            menuMapper.insert(userMgmt);
        } else {
            boolean changed = false;
            if (!Objects.equals(userMgmt.getMenuName(), "用户管理")) {
                userMgmt.setMenuName("用户管理");
                changed = true;
            }
            if (!Objects.equals(userMgmt.getParentId(), groupMgmt.getId())) {
                userMgmt.setParentId(groupMgmt.getId());
                changed = true;
            }
            if (changed) {
                menuMapper.updateById(userMgmt);
            }
        }

        MenuDO roleMgmt = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, "GROUP_ROLE_MGMT")
                .last("limit 1"));
        if (roleMgmt == null) {
            roleMgmt = new MenuDO();
            roleMgmt.setMenuCode("GROUP_ROLE_MGMT");
            roleMgmt.setMenuName("角色管理");
            roleMgmt.setParentId(groupMgmt.getId());
            roleMgmt.setMenuType("MENU");
            roleMgmt.setRoutePath("/group/roles");
            roleMgmt.setComponentPath("group/roles/index");
            roleMgmt.setPermissionCode("group:role:manage");
            roleMgmt.setIcon("team");
            roleMgmt.setSortNo(47);
            roleMgmt.setVisible(Boolean.TRUE);
            roleMgmt.setStatus(STATUS_ENABLED);
            menuMapper.insert(roleMgmt);
        }

        MenuDO menuPermMgmt = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuCode, "GROUP_MENU_PERMISSION_MGMT")
                .last("limit 1"));
        if (menuPermMgmt == null) {
            menuPermMgmt = new MenuDO();
            menuPermMgmt.setMenuCode("GROUP_MENU_PERMISSION_MGMT");
            menuPermMgmt.setMenuName("菜单权限管理");
            menuPermMgmt.setParentId(groupMgmt.getId());
            menuPermMgmt.setMenuType("MENU");
            menuPermMgmt.setRoutePath("/group/menu-permissions");
            menuPermMgmt.setComponentPath("group/menu-permissions/index");
            menuPermMgmt.setPermissionCode("group:menu-permission:manage");
            menuPermMgmt.setIcon("setting");
            menuPermMgmt.setSortNo(48);
            menuPermMgmt.setVisible(Boolean.TRUE);
            menuPermMgmt.setStatus(STATUS_ENABLED);
            menuMapper.insert(menuPermMgmt);
        } else if (!Objects.equals(menuPermMgmt.getParentId(), groupMgmt.getId())) {
            menuPermMgmt.setParentId(groupMgmt.getId());
            menuMapper.updateById(menuPermMgmt);
        }

        RoleDO groupAdminRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "GROUP_ADMIN")
                .last("limit 1"));
        if (groupAdminRole == null) {
            return;
        }

        ensureRoleMenuRel(groupAdminRole.getId(), userMgmt.getId());
        ensureRoleMenuRel(groupAdminRole.getId(), roleMgmt.getId());
        ensureRoleMenuRel(groupAdminRole.getId(), menuPermMgmt.getId());
    }

    private void ensureRoleMenuRel(Long roleId, Long menuId) {
        if (roleId == null || menuId == null) {
            return;
        }
        RoleMenuRelDO exists = roleMenuRelMapper.selectOne(new LambdaQueryWrapper<RoleMenuRelDO>()
                .eq(RoleMenuRelDO::getRoleId, roleId)
                .eq(RoleMenuRelDO::getMenuId, menuId)
                .last("limit 1"));
        if (exists != null) {
            return;
        }
        RoleMenuRelDO rel = new RoleMenuRelDO();
        rel.setRoleId(roleId);
        rel.setMenuId(menuId);
        roleMenuRelMapper.insert(rel);
    }

    @GetMapping("/roles/{id}/menu-ids")
    public CodeDataResponse<List<Long>> listRoleMenuIds(@PathVariable Long id) {
        Long operatorId = currentOperatorId();
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        ensureCanManageRole(operatorId, role);
        List<Long> menuIds = roleMenuRelMapper.selectByRoleId(id).stream()
                .map(RoleMenuRelDO::getMenuId)
                .distinct()
                .toList();
        return CodeDataResponse.ok(menuIds);
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        RoleDO role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRelDO>().eq(RoleMenuRelDO::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> dedupedMenuIds = menuIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dedupedMenuIds.isEmpty()) {
            return;
        }

        List<MenuDO> validMenus = menuMapper.selectBatchIds(dedupedMenuIds);
        Map<Long, MenuDO> validMenuMap = validMenus.stream().collect(Collectors.toMap(MenuDO::getId, m -> m));
        for (Long menuId : dedupedMenuIds) {
            MenuDO menu = validMenuMap.get(menuId);
            if (menu == null) {
                continue;
            }
            if (!isMenuAssignableToRoleType(role.getRoleType(), menu.getMenuCode())) {
                continue;
            }
            RoleMenuRelDO rel = new RoleMenuRelDO();
            rel.setRoleId(roleId);
            rel.setMenuId(menuId);
            roleMenuRelMapper.insert(rel);
        }
    }

    private boolean isMenuAssignableToRoleType(String roleType, String menuCode) {
        String normalizedRoleType = trimNullable(roleType);
        String normalizedMenuCode = trimNullable(menuCode);
        if (normalizedMenuCode == null) {
            return false;
        }
        if ("GROUP".equals(normalizedRoleType)) {
            return normalizedMenuCode.startsWith("GROUP_");
        }
        if ("STORE".equals(normalizedRoleType)) {
            return normalizedMenuCode.startsWith("STORE_BIZ_");
        }
        return true;
    }

    private void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        List<RoleDO> templateRoles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getTenantGroupId, 0L)
                .eq(RoleDO::getStatus, STATUS_ENABLED)
                .eq(RoleDO::getDescription, GROUP_ROLE_TEMPLATE_DESC)
                .in(RoleDO::getRoleType, List.of("GROUP", "STORE"))
                .orderByAsc(RoleDO::getId));
        for (RoleDO template : templateRoles) {
            RoleDO existing = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                    .eq(RoleDO::getTenantGroupId, groupId)
                    .eq(RoleDO::getRoleCode, template.getRoleCode())
                    .last("limit 1"));
            if (existing != null) {
                if (!STATUS_ENABLED.equals(existing.getStatus())) {
                    existing.setStatus(STATUS_ENABLED);
                    roleMapper.updateById(existing);
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
            roleMapper.insert(role);
        }
    }

    private void validateRoleMenusByType(RoleDO role, List<Long> menuIds) {
        if (role == null || menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Set<Long> dedupedMenuIds = menuIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dedupedMenuIds.isEmpty()) {
            return;
        }
        List<MenuDO> menus = menuMapper.selectBatchIds(dedupedMenuIds);
        Map<Long, MenuDO> menuMap = menus.stream().collect(Collectors.toMap(MenuDO::getId, m -> m));
        for (Long menuId : dedupedMenuIds) {
            MenuDO menu = menuMap.get(menuId);
            if (menu == null) {
                continue;
            }
            boolean isStoreMenu = menu.getMenuCode() != null && menu.getMenuCode().startsWith("STORE_BIZ_");
            if ("STORE".equals(role.getRoleType()) && !isStoreMenu) {
                throw new BusinessException("门店角色仅可分配门店菜单");
            }
            if ("GROUP".equals(role.getRoleType()) && isStoreMenu) {
                throw new BusinessException("集团角色仅可分配集团菜单");
            }
        }
    }

    private UserAccountDO requireUser(Long id) {
        UserAccountDO user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String normalizeStatus(String rawStatus) {
        String status = trimNullable(rawStatus);
        if (status == null) {
            return STATUS_ENABLED;
        }
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException("状态仅支持 ENABLED 或 DISABLED");
        }
        return status;
    }

    private Long currentOperatorId() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            return 1L;
        }
        return AuthContextHolder.get().getUserId();
    }

    private String normalizePhone(String phone) {
        String value = trim(phone);
        if (value.length() < 6) {
            throw new BusinessException("手机号格式不正确");
        }
        return value;
    }

    private String normalizeScopeType(String rawScopeType, String roleType) {
        String scopeType = trimNullable(rawScopeType);
        if (scopeType != null) {
            return scopeType;
        }
        String roleTypeValue = trimNullable(roleType);
        if ("GROUP".equals(roleTypeValue)) {
            return "GROUP";
        }
        if ("STORE".equals(roleTypeValue)) {
            return "STORE";
        }
        return "PLATFORM";
    }

    private Long normalizeScopeId(String scopeType, Long rawScopeId) {
        if ("PLATFORM".equals(scopeType)) {
            if (rawScopeId != null) {
                throw new BusinessException("平台角色不支持指定作用域ID");
            }
            return null;
        }
        if ("GROUP".equals(scopeType)) {
            if (rawScopeId == null) {
                throw new BusinessException("集团角色必须指定集团作用域");
            }
            return rawScopeId;
        }
        if ("STORE".equals(scopeType)) {
            if (rawScopeId == null) {
                throw new BusinessException("门店角色必须指定门店作用域");
            }
            return rawScopeId;
        }
        return rawScopeId;
    }

    private String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record IdPayload(Long id) {
    }

    private GroupDO requireGroup(Long groupId) {
        GroupDO group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("集团不存在");
        }
        return group;
    }

    private RoleDO requireRoleByCode(String roleCode) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, roleCode)
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException("角色不存在: " + roleCode);
        }
        return role;
    }

    private void requirePlatformAdmin() {
        if (!isPlatformAdmin(currentOperatorId())) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
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
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        return rel != null;
    }

    private void ensureCanManageGroup(Long groupId) {
        Long userId = currentOperatorId();
        if (isPlatformAdmin(userId)) {
            return;
        }
        RoleDO groupAdminRole = requireRoleByCode("GROUP_ADMIN");
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, groupAdminRole.getId())
                .eq(UserRoleRelDO::getScopeType, "GROUP")
                .eq(UserRoleRelDO::getScopeId, groupId)
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        if (rel == null) {
            throw new BusinessException("当前账号无该集团管理权限");
        }
    }

    public record RoleAssignmentView(Long roleId,
                                     String roleCode,
                                     String roleName,
                                     String roleType,
                                     String scopeType,
                                     Long scopeId,
                                     String scopeName) {
    }

    public record GroupAdminView(Long id,
                                 String groupCode,
                                 String groupName,
                                 String status,
                                 String remark,
                                 LocalDateTime createdAt) {
    }

    public record BindGroupAdminResult(Long groupId,
                                       String groupName,
                                       Long userId,
                                       String phone,
                                       String realName) {
    }

    public record GroupAdminCandidateView(Long userId,
                                          String realName,
                                          String phone,
                                          Long storeId,
                                          String storeCode,
                                          String storeName) {
    }

    public record StoreAdminView(Long id,
                                 Long groupId,
                                 String storeCode,
                                 String storeName,
                                 String status,
                                 String contactName,
                                 String contactPhone,
                                 String address,
                                 String remark,
                                 LocalDateTime createdAt) {
    }

    public record UserAdminView(Long id,
                                String username,
                                String realName,
                                String phone,
                                String status,
                                LocalDateTime createdAt,
                                List<RoleAssignmentView> roles) {
    }

    public record RoleAdminView(Long id,
                                String roleCode,
                                String roleName,
                                String roleType,
                                String dataScopeType,
                                String description,
                                String status,
                                List<Long> menuIds,
                                Boolean builtin,
                                Boolean editable) {
    }

    public record MenuAdminView(Long id,
                                String menuCode,
                                String menuName,
                                Long parentId,
                                String menuType,
                                String routePath,
                                String permissionCode,
                                String status,
                                Integer sortNo) {
    }

    private List<Long> listManagedGroupIds(Long operatorId) {
        return userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                        .eq(UserRoleRelDO::getUserId, operatorId)
                        .eq(UserRoleRelDO::getScopeType, "GROUP")
                        .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                        .isNotNull(UserRoleRelDO::getScopeId))
                .stream()
                .map(UserRoleRelDO::getScopeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Long> listManagedStoreIds(Set<Long> managedGroupIds) {
        if (managedGroupIds == null || managedGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                        .select(StoreDO::getId)
                        .in(StoreDO::getGroupId, managedGroupIds))
                .stream()
                .map(StoreDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean isAllowedScope(String scopeType,
                                   Long scopeId,
                                   Set<Long> managedGroupIds,
                                   Set<Long> managedStoreIds) {
        if ("GROUP".equals(scopeType)) {
            return scopeId != null && managedGroupIds.contains(scopeId);
        }
        if ("STORE".equals(scopeType)) {
            return scopeId != null && managedStoreIds.contains(scopeId);
        }
        return false;
    }

    private void ensureCanManageUser(Long targetUserId,
                                     Set<Long> managedGroupIds,
                                     Set<Long> managedStoreIds) {
        if ((managedGroupIds == null || managedGroupIds.isEmpty())
                && (managedStoreIds == null || managedStoreIds.isEmpty())) {
            throw new BusinessException("当前账号无可管理用户范围");
        }

        LambdaQueryWrapper<UserRoleRelDO> query = new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, targetUserId)
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED);

        boolean hasGroupScope = managedGroupIds != null && !managedGroupIds.isEmpty();
        boolean hasStoreScope = managedStoreIds != null && !managedStoreIds.isEmpty();
        if (hasGroupScope && hasStoreScope) {
            query.and(wrapper -> wrapper
                    .and(groupScoped -> groupScoped
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .in(UserRoleRelDO::getScopeId, managedGroupIds))
                    .or(storeScoped -> storeScoped
                            .eq(UserRoleRelDO::getScopeType, "STORE")
                            .in(UserRoleRelDO::getScopeId, managedStoreIds)));
        } else if (hasGroupScope) {
            query.eq(UserRoleRelDO::getScopeType, "GROUP")
                    .in(UserRoleRelDO::getScopeId, managedGroupIds);
        } else {
            query.eq(UserRoleRelDO::getScopeType, "STORE")
                    .in(UserRoleRelDO::getScopeId, managedStoreIds);
        }

        Long matched = userRoleRelMapper.selectCount(query);
        if (matched == null || matched == 0) {
            throw new BusinessException("当前账号无该用户操作权限");
        }
    }

    private String toTenantDisplayRoleCode(String roleCode, Set<Long> managedGroupIds) {
        if (roleCode == null || managedGroupIds == null || managedGroupIds.isEmpty()) {
            return roleCode;
        }
        for (Long groupId : managedGroupIds) {
            String prefix = "G" + groupId + "__";
            if (roleCode.startsWith(prefix)) {
                return roleCode.substring(prefix.length());
            }
        }
        return roleCode;
    }

    private Set<Long> listManageableRoleIds(Long operatorId) {
        List<Long> managedGroupIds = listManagedGroupIds(operatorId);
        if (managedGroupIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> managedStoreIds = new HashSet<>(listManagedStoreIds(new HashSet<>(managedGroupIds)));
        List<UserRoleRelDO> rels = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                .and(wrapper -> {
                    wrapper.and(groupScoped -> groupScoped
                            .eq(UserRoleRelDO::getScopeType, "GROUP")
                            .in(UserRoleRelDO::getScopeId, managedGroupIds));
                    if (!managedStoreIds.isEmpty()) {
                        wrapper.or(storeScoped -> storeScoped
                                .eq(UserRoleRelDO::getScopeType, "STORE")
                                .in(UserRoleRelDO::getScopeId, managedStoreIds));
                    }
                }));
        return rels.stream()
                .map(UserRoleRelDO::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void ensureCanManageRole(Long operatorId, RoleDO role) {
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (isPlatformAdmin(operatorId)) {
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

    private boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED));
        return count != null && count > 0;
    }

    private void ensureRoleMutable(RoleDO role) {
        if (!isRoleMutable(role)) {
            throw new BusinessException("内置角色不允许修改或删除");
        }
    }

    private void ensureRoleMenuAssignable(Long operatorId, RoleDO role) {
        if (isPlatformAdmin(operatorId)) {
            return;
        }
        if (role != null && "GROUP_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException("集团管理员角色菜单权限仅允许平台管理员配置");
        }
    }

    private boolean isRoleMutable(RoleDO role) {
        if (role == null) {
            return true;
        }
        return !isRoleBuiltin(role);
    }

    private boolean isRoleBuiltin(RoleDO role) {
        if (role == null) {
            return false;
        }
        if (PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
            return true;
        }
        return GROUP_ROLE_TEMPLATE_DESC.equals(role.getDescription());
    }

}
