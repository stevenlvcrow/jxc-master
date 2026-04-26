package com.boboboom.jxc.identity.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;

/** 组织作用域服务，负责解析并校验平台、集团和门店作用域。 */
@Service
public class OrgScopeService {

    private static final String PLATFORM_SCOPE_LITERAL = "platform";
    public static final String SCOPE_PLATFORM = "PLATFORM";
    public static final String SCOPE_GROUP = "GROUP";
    public static final String SCOPE_STORE = "STORE";

    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String PLATFORM_ADMIN_ROLE_CODE = "PLATFORM_ADMIN";
    private static final String DATA_SCOPE_GROUP = "GROUP";

    private final RoleRepository roleRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreRepository storeRepository;
    private final GroupRepository groupRepository;
    private final DictionaryLookupService dictionaryLookupService;

    /** 组织作用域服务，负责解析并校验平台、集团和门店作用域。 */
    public OrgScopeService(RoleRepository roleRepositoryValue,
                           UserRoleRelRepository userRoleRelRepositoryValue,
                           StoreRepository storeRepositoryValue,
                           GroupRepository groupRepositoryValue,
                           DictionaryLookupService dictionaryLookupServiceValue) {
        this.roleRepository = roleRepositoryValue;
        this.userRoleRelRepository = userRoleRelRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.groupRepository = groupRepositoryValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 判断PlatformAdmin。 */
    public boolean isPlatformAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return hasPlatformRole(userId, PLATFORM_SUPER_ADMIN_ROLE_CODE) || hasPlatformRole(userId, PLATFORM_ADMIN_ROLE_CODE);
    }

    /** 解析Accessible作用域。 */
    public AccessibleScope resolveAccessibleScope(Long userId, String orgId) {
        ScopeRequest requested = parseAccessibleScope(orgId);
        if (isPlatformAdmin(userId)) {
            return toAccessibleScope(requested);
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new OrgScopeAccessDeniedException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasGroupWideScope(userId, requested.scopeId())) {
                throw new OrgScopeAccessDeniedException("当前账号无该集团权限");
            }
            return toAccessibleScope(requested);
        }
        if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
            return toAccessibleScope(requested);
        }
        Long groupId = findGroupIdByStoreId(requested.scopeId());
        if (groupId != null && hasGroupWideScope(userId, groupId)) {
            return new AccessibleScope(SCOPE_STORE, requested.scopeId(), groupId);
        }
        throw new OrgScopeAccessDeniedException("当前账号无该门店权限");
    }

    /** 解析Accessible作用域AllowAnonymous。 */
    public AccessibleScope resolveAccessibleScopeAllowAnonymous(Long userId, String orgId) {
        if (userId == null) {
            return toAccessibleScope(parseAccessibleScope(orgId));
        }
        return resolveAccessibleScope(userId, orgId);
    }

    /** 解析PlatformOr门店作用域。 */
    public AccessibleScope resolvePlatformOrStoreScope(Long userId, String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null || PLATFORM_SCOPE_LITERAL.equals(normalizedOrgId)) {
            if (isPlatformAdmin(userId)) {
                return new AccessibleScope(SCOPE_PLATFORM, 0L, 0L);
            }
            throw new OrgScopeAccessDeniedException("请先选择门店机构");
        }
        if (!normalizedOrgId.startsWith("store-")) {
            throw new OrgScopeAccessDeniedException("请先选择门店机构");
        }

        Long storeId = parseNumericId(normalizedOrgId.substring("store-".length()));
        StoreDO store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            throw new OrgScopeAccessDeniedException("门店不存在，请重新选择机构");
        }
        if (store.getGroupId() == null) {
            throw new BusinessException("门店未绑定集团");
        }
        if (isPlatformAdmin(userId)) {
            return new AccessibleScope(SCOPE_STORE, storeId, store.getGroupId());
        }
        if (hasScope(userId, SCOPE_STORE, storeId)) {
            return new AccessibleScope(SCOPE_STORE, storeId, store.getGroupId());
        }
        Long groupId = store.getGroupId();
        if (hasGroupWideScope(userId, groupId)) {
            return new AccessibleScope(SCOPE_STORE, storeId, groupId);
        }
        throw new OrgScopeAccessDeniedException("当前账号无该门店权限");
    }

    /** 解析流程作用域。 */
    public WorkflowScope resolveWorkflowScope(Long userId, String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null) {
            throw new OrgScopeAccessDeniedException("请先选择集团或门店机构");
        }
        ScopeRequest requested = parseWorkflowScopeRequest(normalizedOrgId);
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            return resolveGroupWorkflowScope(userId, requested.scopeId());
        }

        StoreDO store = storeRepository.findById(requested.scopeId()).orElse(null);
        if (store == null) {
            throw new BusinessException("门店不存在");
        }
        if (store.getGroupId() == null) {
            throw new BusinessException("门店未绑定集团");
        }
        if (!isPlatformAdmin(userId)) {
            boolean hasStoreScope = hasScope(userId, SCOPE_STORE, requested.scopeId());
            boolean hasGroupScope = hasGroupWideScope(userId, store.getGroupId());
            if (!hasStoreScope && !hasGroupScope) {
                throw new OrgScopeAccessDeniedException("当前账号无该门店权限");
            }
        }
        return new WorkflowScope(SCOPE_STORE, requested.scopeId(), store.getGroupId());
    }

    private WorkflowScope resolveGroupWorkflowScope(Long userId, Long groupId) {
        GroupDO group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new BusinessException("集团不存在");
        }
        if (!isPlatformAdmin(userId) && !hasGroupWideScope(userId, groupId)) {
            throw new OrgScopeAccessDeniedException("当前账号无该集团权限");
        }
        return new WorkflowScope(SCOPE_GROUP, groupId, groupId);
    }

    /** 解析菜单作用域。 */
    public MenuScope resolveMenuScope(String orgId) {
        ScopeRequest requested = parseAccessibleScope(orgId);
        return new MenuScope(requested.scopeType(), requested.scopeId());
    }

    /** 解析集团流程作用域。 */
    public Long resolveGroupWorkflowScope(Long userId, String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null) {
            throw new OrgScopeAccessDeniedException("请先选择集团机构");
        }
        if (!normalizedOrgId.startsWith("group-")) {
            throw new BusinessException("流程管理仅支持集团机构");
        }
        Long groupId = parseNumericId(normalizedOrgId.substring("group-".length()));
        GroupDO group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new BusinessException("集团不存在");
        }
        if (isPlatformAdmin(userId) || hasGroupWideScope(userId, groupId)) {
            return groupId;
        }
        throw new OrgScopeAccessDeniedException("当前账号无该集团权限");
    }

    private AccessibleScope toAccessibleScope(ScopeRequest requested) {
        if (SCOPE_STORE.equals(requested.scopeType())) {
            return new AccessibleScope(SCOPE_STORE, requested.scopeId(), findGroupIdByStoreId(requested.scopeId()));
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            return new AccessibleScope(SCOPE_GROUP, requested.scopeId(), requested.scopeId());
        }
        return new AccessibleScope(SCOPE_PLATFORM, requested.scopeId(), requested.scopeId());
    }

    private ScopeRequest parseAccessibleScope(String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null) {
            throw new OrgScopeAccessDeniedException("请先选择机构");
        }
        if (PLATFORM_SCOPE_LITERAL.equals(normalizedOrgId)) {
            return new ScopeRequest(SCOPE_PLATFORM, 0L);
        }
        return parseRequiredGroupOrStoreScope(normalizedOrgId);
    }

    private ScopeRequest parseRequiredGroupOrStoreScope(String orgId) {
        if (orgId.startsWith("group-")) {
            return new ScopeRequest(SCOPE_GROUP, parseNumericId(orgId.substring("group-".length())));
        }
        if (orgId.startsWith("store-")) {
            return new ScopeRequest(SCOPE_STORE, parseNumericId(orgId.substring("store-".length())));
        }
        throw new OrgScopeAccessDeniedException("机构参数非法，请重新选择机构");
    }

    private ScopeRequest parseWorkflowScopeRequest(String orgId) {
        if (orgId.startsWith("group-") || orgId.startsWith("store-")) {
            return parseRequiredGroupOrStoreScope(orgId);
        }
        throw new BusinessException("请在集团或门店工作台中配置流程");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private boolean hasScope(Long userId, String scopeType, Long scopeId) {
        return userRoleRelRepository.existsByUserIdAndScopeTypeAndScopeIdAndStatus(
                userId,
                scopeType,
                scopeId,
                enabledStatus()
        );
    }

    private boolean hasGroupWideScope(Long userId, Long groupId) {
        if (userId == null || groupId == null) {
            return false;
        }
        return userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(userId, SCOPE_GROUP, enabledStatus())
                .stream()
                .filter(rel -> groupId.equals(rel.getScopeId()))
                .anyMatch(rel -> roleRepository.findById(rel.getRoleId())
                        .map(RoleDO::getDataScopeType)
                        .filter(DATA_SCOPE_GROUP::equals)
                        .isPresent());
    }

    private boolean hasPlatformRole(Long userId, String roleCode) {
        RoleDO role = roleRepository.findByRoleCode(roleCode).orElse(null);
        if (role == null) {
            return false;
        }
        return userRoleRelRepository.existsByUserIdAndRoleIdAndScopeTypeAndStatus(
                userId,
                role.getId(),
                SCOPE_PLATFORM,
                enabledStatus()
        );
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }

    private Long findGroupIdByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreDO store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return null;
        }
        return store.getGroupId();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ScopeRequest(String scopeType, Long scopeId) {
    }

    /** 身份与权限数据模型，承载Accessible作用域数据。 */
    public record AccessibleScope(String scopeType, Long scopeId, Long groupId) {
    }

    /** 身份与权限数据模型，承载流程作用域数据。 */
    public record WorkflowScope(String scopeType, Long scopeId, Long groupId) {
    }

    /** 身份与权限数据模型，承载菜单作用域数据。 */
    public record MenuScope(String scopeType, Long scopeId) {
    }
}
