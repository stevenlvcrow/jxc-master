package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Service
public class GroupAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String GROUP_CODE_PREFIX = "JTBM";
    private static final String STORE_CODE_PREFIX = "MDBM";

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final StoreAdminRelRepository storeAdminRelRepository;
    private final WorkflowProcessStoreBindingRepository workflowProcessStoreBindingRepository;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public GroupAdministrationService(UserAccountRepository userAccountRepository,
                                      UserRoleRelRepository userRoleRelRepository,
                                      GroupRepository groupRepository,
                                      StoreRepository storeRepository,
                                      StoreAdminRelRepository storeAdminRelRepository,
                                      WorkflowProcessStoreBindingRepository workflowProcessStoreBindingRepository,
                                      IdentityAdminLookupService identityAdminLookupService,
                                      BusinessCodeGenerator businessCodeGenerator) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRelRepository = userRoleRelRepository;
        this.groupRepository = groupRepository;
        this.storeRepository = storeRepository;
        this.storeAdminRelRepository = storeAdminRelRepository;
        this.workflowProcessStoreBindingRepository = workflowProcessStoreBindingRepository;
        this.identityAdminLookupService = identityAdminLookupService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    @Transactional
    public BindGroupAdminSnapshot bindGroupAdmin(GroupDO group,
                                                 Long operatorId,
                                                 String phone,
                                                 String realNameOrPhone) {
        UserAccountDO user = userAccountRepository.findByPhone(phone).orElse(null);

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
            userAccountRepository.save(user);
        } else if (realNameOrPhone != null && !realNameOrPhone.equals(user.getRealName())) {
            user.setRealName(realNameOrPhone);
            userAccountRepository.update(user);
        }

        RoleDO groupAdminRole = identityAdminLookupService.requireRoleByCode("GROUP_ADMIN");
        UserRoleRelDO rel = userRoleRelRepository.findByUserIdRoleAndScope(user.getId(), groupAdminRole.getId(), "GROUP", group.getId())
                .orElse(null);
        if (rel == null) {
            rel = new UserRoleRelDO();
            rel.setUserId(user.getId());
            rel.setRoleId(groupAdminRole.getId());
            rel.setScopeType("GROUP");
            rel.setScopeId(group.getId());
            rel.setAssignedBy(operatorId);
            rel.setStatus(STATUS_ENABLED);
            userRoleRelRepository.save(rel);
        } else if (!STATUS_ENABLED.equals(rel.getStatus())) {
            rel.setStatus(STATUS_ENABLED);
            userRoleRelRepository.update(rel);
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
            return groupRepository.findAllOrdered();
        }
        RoleDO groupAdminRole = identityAdminLookupService.requireRoleByCode("GROUP_ADMIN");
        List<Long> groupIds = userRoleRelRepository.findByUserIdAndStatus(operatorId, STATUS_ENABLED)
                .stream()
                .filter(rel -> groupAdminRole.getId().equals(rel.getRoleId()))
                .filter(rel -> "GROUP".equals(rel.getScopeType()))
                .map(UserRoleRelDO::getScopeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return groupRepository.findByIdsOrdered(groupIds);
    }

    @Transactional
    public GroupDO createGroup(GroupUpsertRequest request, Long operatorId) {
        String groupCode = generateGroupCode();
        if (groupRepository.findByGroupCode(groupCode).isPresent()) {
            throw new BusinessException("集团编码已存在");
        }
        GroupDO group = new GroupDO();
        group.setGroupCode(groupCode);
        group.setGroupName(identityAdminLookupService.trim(request.getGroupName()));
        group.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        group.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        groupRepository.save(group);
        ensureGroupBuiltinRoles(group.getId(), operatorId);
        return group;
    }

    @Transactional
    public GroupDO updateGroup(Long id, GroupUpsertRequest request) {
        GroupDO group = identityAdminLookupService.requireGroup(id);
        String groupCode = identityAdminLookupService.trim(request.getGroupCode());
        boolean codeExists = groupRepository.findByGroupCode(groupCode)
                .map(GroupDO::getId)
                .filter(existingId -> !existingId.equals(id))
                .isPresent();
        if (codeExists) {
            throw new BusinessException("集团编码已存在");
        }
        group.setGroupCode(groupCode);
        group.setGroupName(identityAdminLookupService.trim(request.getGroupName()));
        group.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        group.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        groupRepository.update(group);
        return group;
    }

    @Transactional
    public void deleteGroup(Long id) {
        identityAdminLookupService.requireGroup(id);
        Long storeCount = storeRepository.countByGroupId(id);
        if (storeCount != null && storeCount > 0) {
            throw new BusinessException("集团下存在门店，无法删除");
        }
        userRoleRelRepository.deleteByScopeTypeAndScopeId("GROUP", id);
        groupRepository.deleteById(id);
    }

    @Transactional
    public GroupDO updateGroupStatus(Long id, String status) {
        GroupDO group = identityAdminLookupService.requireGroup(id);
        group.setStatus(identityAdminLookupService.normalizeStatus(status));
        groupRepository.update(group);
        return group;
    }

    public List<com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO> listGroupStores(Long groupId) {
        return storeRepository.findByGroupId(groupId);
    }

    @Transactional
    public StoreDO createGroupStore(Long groupId, GroupStoreCreateRequest request) {
        identityAdminLookupService.requireGroup(groupId);

        String storeCode = generateStoreCode();
        if (storeRepository.findByStoreCode(storeCode).isPresent()) {
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
        storeRepository.save(store);
        return store;
    }

    @Transactional
    public StoreDO updateGroupStore(Long groupId, Long storeId, GroupStoreCreateRequest request) {
        identityAdminLookupService.requireGroup(groupId);
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        if (!groupId.equals(store.getGroupId())) {
            throw new BusinessException("门店不属于当前集团");
        }
        store.setStoreName(identityAdminLookupService.trim(request.getStoreName()));
        store.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        store.setContactName(identityAdminLookupService.trimNullable(request.getContactName()));
        store.setContactPhone(identityAdminLookupService.trimNullable(request.getContactPhone()));
        store.setAddress(identityAdminLookupService.trimNullable(request.getAddress()));
        store.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        storeRepository.update(store);
        return store;
    }

    @Transactional
    public void deleteGroupStore(Long groupId, Long storeId) {
        identityAdminLookupService.requireGroup(groupId);
        StoreDO store = identityAdminLookupService.requireStore(storeId);
        if (!groupId.equals(store.getGroupId())) {
            throw new BusinessException("门店不属于当前集团");
        }

        Long storeAdminCount = storeAdminRelRepository.countByStoreId(storeId);
        Long storeRoleBindingCount = userRoleRelRepository.countByScopeTypeAndScopeId("STORE", storeId);
        if ((storeAdminCount != null && storeAdminCount > 0) || (storeRoleBindingCount != null && storeRoleBindingCount > 0)) {
            throw new BusinessException("门店下存在自建角色或已绑定用户，无法删除");
        }
        workflowProcessStoreBindingRepository.deleteByStoreId(storeId);
        storeRepository.deleteById(storeId);
    }

    public void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        identityAdminLookupService.ensureGroupBuiltinRoles(groupId, operatorId);
    }

    public List<GroupAdminCandidateSnapshot> listGroupAdminCandidates(Long groupId) {
        List<StoreAdminView> rows = storeRepository.findStoreAdminViewsByGroupId(groupId, STATUS_ENABLED);

        LinkedHashMap<Long, GroupAdminCandidateSnapshot> deduped = new LinkedHashMap<>();
        for (StoreAdminView row : rows) {
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
        List<String> existingCodes = groupRepository.findAllGroupCodes();
        return businessCodeGenerator.nextCode(GROUP_CODE_PREFIX, existingCodes);
    }

    private String generateStoreCode() {
        List<String> existingCodes = storeRepository.findAllStoreCodes();
        return businessCodeGenerator.nextCode(STORE_CODE_PREFIX, existingCodes);
    }
}
