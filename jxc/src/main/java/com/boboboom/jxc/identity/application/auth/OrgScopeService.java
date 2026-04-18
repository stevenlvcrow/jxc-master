package com.boboboom.jxc.identity.application.auth;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrgScopeService {

    private static final String PLATFORM_SCOPE_LITERAL = "platform";
    public static final String SCOPE_PLATFORM = "PLATFORM";
    public static final String SCOPE_GROUP = "GROUP";
    public static final String SCOPE_STORE = "STORE";

    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String STATUS_ENABLED = "ENABLED";

    private final RoleRepository roleRepository;
    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreRepository storeRepository;
    private final GroupRepository groupRepository;

    public OrgScopeService(RoleRepository roleRepository,
                           UserRoleRelRepository userRoleRelRepository,
                           StoreRepository storeRepository,
                           GroupRepository groupRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRelRepository = userRoleRelRepository;
        this.storeRepository = storeRepository;
        this.groupRepository = groupRepository;
    }

    public boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleRepository.findByRoleCode(PLATFORM_SUPER_ADMIN_ROLE_CODE).orElse(null);
        if (role == null || userId == null) {
            return false;
        }
        return userRoleRelRepository.existsByUserIdAndRoleIdAndScopeTypeAndStatus(
                userId,
                role.getId(),
                SCOPE_PLATFORM,
                STATUS_ENABLED
        );
    }

    public AccessibleScope resolveAccessibleScope(Long userId, String orgId) {
        ScopeRequest requested = parseAccessibleScope(orgId);
        if (isPlatformAdmin(userId)) {
            return toAccessibleScope(requested);
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return toAccessibleScope(requested);
        }
        if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
            return toAccessibleScope(requested);
        }
        Long groupId = findGroupIdByStoreId(requested.scopeId());
        if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
            return new AccessibleScope(SCOPE_STORE, requested.scopeId(), groupId);
        }
        throw new BusinessException("当前账号无该门店权限");
    }

    public AccessibleScope resolveAccessibleScopeAllowAnonymous(Long userId, String orgId) {
        if (userId == null) {
            return toAccessibleScope(parseAccessibleScope(orgId));
        }
        return resolveAccessibleScope(userId, orgId);
    }

    public WorkflowScope resolveWorkflowScope(Long userId, String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null) {
            throw new BusinessException("请先选择集团或门店机构");
        }
        ScopeRequest requested = parseWorkflowScopeRequest(normalizedOrgId);
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            GroupDO group = groupRepository.findById(requested.scopeId()).orElse(null);
            if (group == null) {
                throw new BusinessException("集团不存在");
            }
            if (!isPlatformAdmin(userId) && !hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new WorkflowScope(SCOPE_GROUP, requested.scopeId(), requested.scopeId());
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
            boolean hasGroupScope = hasScope(userId, SCOPE_GROUP, store.getGroupId());
            if (!hasStoreScope && !hasGroupScope) {
                throw new BusinessException("当前账号无该门店权限");
            }
        }
        return new WorkflowScope(SCOPE_STORE, requested.scopeId(), store.getGroupId());
    }

    public MenuScope resolveMenuScope(String orgId) {
        ScopeRequest requested = parseAccessibleScope(orgId);
        return new MenuScope(requested.scopeType(), requested.scopeId());
    }

    public Long resolveGroupWorkflowScope(Long userId, String orgId) {
        String normalizedOrgId = trimToNull(orgId);
        if (normalizedOrgId == null) {
            throw new BusinessException("请先选择集团机构");
        }
        if (!normalizedOrgId.startsWith("group-")) {
            throw new BusinessException("流程管理仅支持集团机构");
        }
        Long groupId = parseNumericId(normalizedOrgId.substring("group-".length()));
        GroupDO group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new BusinessException("集团不存在");
        }
        if (isPlatformAdmin(userId) || hasScope(userId, SCOPE_GROUP, groupId)) {
            return groupId;
        }
        throw new BusinessException("当前账号无该集团权限");
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
            throw new BusinessException("请先选择机构");
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
        throw new BusinessException("机构参数非法");
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
                STATUS_ENABLED
        );
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

    public record AccessibleScope(String scopeType, Long scopeId, Long groupId) {
    }

    public record WorkflowScope(String scopeType, Long scopeId, Long groupId) {
    }

    public record MenuScope(String scopeType, Long scopeId) {
    }
}
