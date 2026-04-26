package com.boboboom.jxc.identity.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;

/**
 * 组织管理员初始化服务，负责创建集团/门店管理员账号并绑定内置角色。
 */
@Service
public class OrgAdminProvisioningService {

    private static final String SOURCE_TYPE_MANUAL = "MANUAL";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final String GROUP_ADMIN_ROLE_CODE = "GROUP_ADMIN";
    private static final String GROUP_MEMBER_ROLE_CODE = "GROUP_MEMBER";
    private static final String STORE_ADMIN_ROLE_CODE = "STORE_ADMIN";
    private static final String DEFAULT_PASSWORD = "123654";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreAdminRelRepository storeAdminRelRepository;
    private final UserCodeGenerator userCodeGenerator;
    private final DictionaryLookupService dictionaryLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public OrgAdminProvisioningService(UserAccountRepository userAccountRepositoryValue,
                                       RoleRepository roleRepositoryValue,
                                       UserRoleRelRepository userRoleRelRepositoryValue,
                                       StoreAdminRelRepository storeAdminRelRepositoryValue,
                                       UserCodeGenerator userCodeGeneratorValue,
                                       DictionaryLookupService dictionaryLookupServiceValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.storeAdminRelRepository = storeAdminRelRepositoryValue;
        this.userCodeGenerator = userCodeGeneratorValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /**
     * 创建集团管理员并绑定集团管理员角色。
     *
     * @param groupId 集团 ID
     * @param operatorId 操作人 ID
     * @param realName 管理员姓名
     * @param phone 管理员手机号
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createGroupAdmin(Long groupId, Long operatorId, String realName, String phone) {
        UserAccountDO user = createAdminUser(realName, phone, SCOPE_GROUP, groupId);
        RoleDO role = roleRepository.findByTenantGroupIdAndRoleCode(groupId, GROUP_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BusinessException("集团管理员角色未初始化"));
        bindRole(user.getId(), role.getId(), SCOPE_GROUP, groupId, operatorId);
    }

    /**
     * 创建门店管理员并绑定门店管理员角色及门店管理员关系。
     *
     * @param groupId 集团 ID
     * @param storeId 门店 ID
     * @param operatorId 操作人 ID
     * @param realName 管理员姓名
     * @param phone 管理员手机号
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createStoreAdmin(Long groupId, Long storeId, Long operatorId, String realName, String phone) {
        UserAccountDO user = createAdminUser(realName, phone, SCOPE_STORE, storeId);
        assignStoreAdmin(groupId, storeId, operatorId, user.getId());
    }

    /** 处理Transactional。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void assignStoreAdmin(Long groupId, Long storeId, Long operatorId, Long adminUserId) {
        UserAccountDO user = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException("请选择有效的门店管理员"));
        RoleDO role = roleRepository.findByTenantGroupIdAndRoleCode(groupId, STORE_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BusinessException("门店管理员角色未初始化"));
        RoleDO groupMemberRole = roleRepository.findByTenantGroupIdAndRoleCode(groupId, GROUP_MEMBER_ROLE_CODE)
                .orElseThrow(() -> new BusinessException("集团成员角色未初始化"));
        bindRole(user.getId(), groupMemberRole.getId(), SCOPE_GROUP, groupId, operatorId);
        bindRole(user.getId(), role.getId(), SCOPE_STORE, storeId, operatorId);
        bindStoreAdmin(storeId, user.getId(), operatorId);
    }

    private UserAccountDO createAdminUser(String realName, String phone, String scopeType, Long scopeId) {
        if (userAccountRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("管理员手机号已存在");
        }
        UserAccountDO user = new UserAccountDO();
        user.setUsername(userCodeGenerator.generate(realName, phone));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setPasswordHash(PasswordCodec.encode(DEFAULT_PASSWORD));
        user.setPasswordSalt(null);
        user.setStatus(enabledStatus());
        user.setSourceType(SOURCE_TYPE_MANUAL);
        user.setCreatedScopeType(scopeType);
        user.setCreatedScopeId(scopeId);
        user.setFirstLoginChangedPwd(Boolean.FALSE);
        userAccountRepository.save(user);
        return user;
    }

    private void bindRole(Long userId, Long roleId, String scopeType, Long scopeId, Long operatorId) {
        UserRoleRelDO existing = userRoleRelRepository.findByUserIdRoleAndScope(userId, roleId, scopeType, scopeId)
                .orElse(null);
        if (existing != null) {
            if (!enabledStatus().equals(existing.getStatus())) {
                existing.setStatus(enabledStatus());
                existing.setAssignedBy(operatorId);
                existing.setAssignedAt(LocalDateTime.now());
                userRoleRelRepository.update(existing);
            }
            return;
        }
        UserRoleRelDO rel = new UserRoleRelDO();
        rel.setUserId(userId);
        rel.setRoleId(roleId);
        rel.setScopeType(scopeType);
        rel.setScopeId(scopeId);
        rel.setAssignedBy(operatorId);
        rel.setAssignedAt(LocalDateTime.now());
        rel.setStatus(enabledStatus());
        userRoleRelRepository.save(rel);
    }

    private void bindStoreAdmin(Long storeId, Long userId, Long operatorId) {
        StoreAdminRelDO existingByStore = storeAdminRelRepository.findByStoreId(storeId).orElse(null);
        if (existingByStore != null && !userId.equals(existingByStore.getUserId())) {
            throw new BusinessException("门店已绑定管理员");
        }
        StoreAdminRelDO existingByUser = storeAdminRelRepository.findByUserId(userId).orElse(null);
        if (existingByUser != null && !storeId.equals(existingByUser.getStoreId())) {
            throw new BusinessException("管理员已绑定其他门店");
        }
        StoreAdminRelDO rel = existingByStore == null ? new StoreAdminRelDO() : existingByStore;
        rel.setStoreId(storeId);
        rel.setUserId(userId);
        rel.setAssignedBy(operatorId);
        rel.setAssignedAt(LocalDateTime.now());
        rel.setStatus(enabledStatus());
        if (rel.getId() == null) {
            storeAdminRelRepository.save(rel);
        } else {
            storeAdminRelRepository.update(rel);
        }
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }
}
