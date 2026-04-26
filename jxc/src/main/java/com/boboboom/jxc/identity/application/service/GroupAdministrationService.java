package com.boboboom.jxc.identity.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.event.DomainEventPublisher;
import com.boboboom.jxc.identity.application.event.GroupCreatedEvent;
import com.boboboom.jxc.identity.application.event.StoreCreatedEvent;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class GroupAdministrationService {

    private static final String GROUP_CODE_PREFIX = "JTBM";
    private static final String STORE_CODE_PREFIX = "MDBM";

    private final UserRoleRelRepository userRoleRelRepository;
    private final UserAccountRepository userAccountRepository;
    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final StoreAdminRelRepository storeAdminRelRepository;
    private final WorkflowProcessStoreBindingRepository workflowProcessStoreBindingRepository;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final DomainEventPublisher domainEventPublisher;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public GroupAdministrationService(UserRoleRelRepository userRoleRelRepositoryValue,
                                      UserAccountRepository userAccountRepositoryValue,
                                      GroupRepository groupRepositoryValue,
                                      StoreRepository storeRepositoryValue,
                                      StoreAdminRelRepository storeAdminRelRepositoryValue,
                                      WorkflowProcessStoreBindingRepository workflowProcessStoreBindingRepositoryValue,
                                      IdentityAdminLookupService identityAdminLookupServiceValue,
                                      BusinessCodeGenerator businessCodeGeneratorValue,
                                      DomainEventPublisher domainEventPublisherValue) {
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.userAccountRepository = userAccountRepositoryValue;
        this.groupRepository = groupRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.storeAdminRelRepository = storeAdminRelRepositoryValue;
        this.workflowProcessStoreBindingRepository = workflowProcessStoreBindingRepositoryValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
        this.businessCodeGenerator = businessCodeGeneratorValue;
        this.domainEventPublisher = domainEventPublisherValue;
    }

    /** 查询集团列表。 */
    public List<GroupDO> listGroups(Long operatorId, boolean platformAdmin) {
        if (platformAdmin) {
            return groupRepository.findAllOrdered();
        }
        List<Long> groupIds = userRoleRelRepository.findByUserIdAndStatus(operatorId, identityAdminLookupService.enabledStatus())
                .stream()
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

    /** 创建集团。 */
    @Transactional
    public GroupDO createGroup(GroupUpsertRequest request, Long operatorId) {
        String groupCode = generateGroupCode();
        if (groupRepository.findByGroupCode(groupCode).isPresent()) {
            throw new BusinessException("集团编码已存在");
        }
        String adminRealName = identityAdminLookupService.trim(request.getAdminRealName());
        String adminPhone = identityAdminLookupService.normalizePhone(request.getAdminPhone());
        GroupDO group = new GroupDO();
        group.setGroupCode(groupCode);
        group.setGroupName(identityAdminLookupService.trim(request.getGroupName()));
        group.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        group.setRemark(identityAdminLookupService.trimNullable(request.getRemark()));
        groupRepository.save(group);
        domainEventPublisher.publish(new GroupCreatedEvent(group.getId(), operatorId, adminRealName, adminPhone));
        return group;
    }

    /** 更新集团。 */
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

    /** 删除集团。 */
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

    /** 更新集团状态。 */
    @Transactional
    public GroupDO updateGroupStatus(Long id, String status) {
        GroupDO group = identityAdminLookupService.requireGroup(id);
        group.setStatus(identityAdminLookupService.normalizeStatus(status));
        groupRepository.update(group);
        return group;
    }

    /** 查询集团门店列表。 */
    public List<com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO> listGroupStores(Long groupId) {
        return storeRepository.findByGroupId(groupId);
    }

    /** 创建集团门店。 */
    @Transactional
    public StoreDO createGroupStore(Long groupId, GroupStoreCreateRequest request, Long operatorId) {
        identityAdminLookupService.requireGroup(groupId);

        String storeCode = generateStoreCode();
        if (storeRepository.findByStoreCode(storeCode).isPresent()) {
            throw new BusinessException("门店编码已存在");
        }
        Long adminUserId = request.getAdminUserId();
        if (adminUserId == null || adminUserId <= 0) {
            throw new BusinessException("请选择门店管理员");
        }
        boolean userInGroup = userAccountRepository.findByGroupScope(groupId).stream()
                .anyMatch(user -> adminUserId.equals(user.getId()));
        if (!userInGroup) {
            throw new BusinessException("门店管理员不属于当前集团");
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
        domainEventPublisher.publish(new StoreCreatedEvent(groupId, store.getId(), operatorId, adminUserId));
        return store;
    }

    /** 更新集团门店。 */
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

    /** 删除集团门店。 */
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

    /** 校验并保证集团Builtin角色满足业务规则。 */
    public void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        identityAdminLookupService.ensureGroupBuiltinRoles(groupId, operatorId);
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
