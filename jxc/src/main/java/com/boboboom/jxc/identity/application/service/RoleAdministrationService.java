package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.interfaces.rest.request.RoleUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String GROUP_ROLE_TEMPLATE_DESC = "GROUP_ROLE_TEMPLATE";
    private static final String ROLE_CODE_PREFIX = "JSBM";
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("GROUP_ADMIN", "STORE_ADMIN");
    private static final Set<String> MANAGED_ROLE_TYPES = Set.of("GROUP", "STORE");

    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final RoleMenuRelRepository roleMenuRelRepository;
    private final IdentityAccessControlService identityAccessControlService;
    private final RoleMenuAdministrationService roleMenuAdministrationService;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final OrgScopeService orgScopeService;

    public RoleAdministrationService(RoleRepository roleRepository,
                                     GroupRepository groupRepository,
                                     RoleMenuRelRepository roleMenuRelRepository,
                                     IdentityAccessControlService identityAccessControlService,
                                     RoleMenuAdministrationService roleMenuAdministrationService,
                                     BusinessCodeGenerator businessCodeGenerator,
                                     OrgScopeService orgScopeService) {
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.roleMenuRelRepository = roleMenuRelRepository;
        this.identityAccessControlService = identityAccessControlService;
        this.roleMenuAdministrationService = roleMenuAdministrationService;
        this.businessCodeGenerator = businessCodeGenerator;
        this.orgScopeService = orgScopeService;
    }

    public List<RoleAdminSnapshot> listRoles(Long operatorId, boolean platformAdmin, String orgId) {
        Long tenantGroupId = resolveTenantGroupId(operatorId, platformAdmin, orgId);
        List<RoleDO> roles;
        if (tenantGroupId == null) {
            roles = sortRoles(roleRepository.findAll());
        } else {
            ensureGroupBuiltinRoles(tenantGroupId, operatorId);
            roles = sortRoles(roleRepository.findByTenantGroupId(tenantGroupId).stream()
                    .filter(role -> MANAGED_ROLE_TYPES.contains(role.getRoleType()))
                    .toList());
        }

        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> groupNameMap = roles.stream()
                .map(RoleDO::getTenantGroupId)
                .filter(groupId -> groupId != null && groupId > 0)
                .distinct()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> groupRepository.findById(groupId)
                                .map(GroupDO::getGroupName)
                                .orElse("集团" + groupId),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        List<Long> roleIds = roles.stream().map(RoleDO::getId).toList();
        List<RoleMenuRelDO> allRoleMenuRels = roleMenuRelRepository.findByRoleIds(roleIds);
        Map<Long, List<Long>> roleMenuIdMap = allRoleMenuRels.stream()
                .collect(Collectors.groupingBy(
                        RoleMenuRelDO::getRoleId,
                        Collectors.mapping(RoleMenuRelDO::getMenuId, Collectors.toCollection(LinkedHashSet::new))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue())));

        List<RoleAdminSnapshot> result = new ArrayList<>(roles.size());
        for (RoleDO role : roles) {
            List<Long> menuIds = roleMenuIdMap.getOrDefault(role.getId(), Collections.emptyList());
            String displayRoleCode = tenantGroupId == null
                    ? role.getRoleCode()
                    : toTenantDisplayRoleCode(role.getRoleCode(), tenantGroupId);
            result.add(new RoleAdminSnapshot(
                    role.getId(),
                    displayRoleCode,
                    role.getTenantGroupId(),
                    groupNameMap.get(role.getTenantGroupId()),
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
        return result;
    }

    @Transactional
    public RoleDO createRole(RoleUpsertRequest request, Long operatorId, boolean platformAdmin, String orgId) {
        String roleType = trim(request.getRoleType());
        if (!platformAdmin && !"GROUP".equals(roleType) && !"STORE".equals(roleType)) {
            throw new BusinessException("集团账号仅可创建集团/门店角色");
        }
        Long tenantGroupId = resolveTenantGroupId(operatorId, platformAdmin, orgId);
        if (tenantGroupId == null) {
            tenantGroupId = 0L;
        }

        String roleCode = generateRoleCode(tenantGroupId);
        RoleDO exists = roleRepository.findByTenantGroupIdAndRoleCode(tenantGroupId, roleCode).orElse(null);
        if (exists != null) {
            throw new BusinessException("角色编码已存在（租户内唯一）");
        }

        RoleDO role = new RoleDO();
        role.setTenantGroupId(tenantGroupId);
        role.setRoleCode(roleCode);
        role.setRoleName(trim(request.getRoleName()));
        role.setRoleType(roleType);
        role.setDataScopeType(trim(request.getDataScopeType()));
        role.setDescription(trimNullable(request.getDescription()));
        role.setStatus(normalizeStatus(request.getStatus()));
        role.setCreatedBy(operatorId);
        roleRepository.save(role);
        roleMenuAdministrationService.saveRoleMenus(role, request.getMenuIds());
        return role;
    }

    @Transactional
    public void updateRole(RoleDO role,
                           RoleUpsertRequest request,
                           Long operatorId,
                           boolean platformAdmin,
                           String orgId) {
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        ensureRoleMutable(role);

        String roleType = trim(request.getRoleType());
        if (!platformAdmin && !"GROUP".equals(roleType) && !"STORE".equals(roleType)) {
            throw new BusinessException("集团账号仅可设置集团/门店角色");
        }
        role.setRoleName(trim(request.getRoleName()));
        role.setRoleType(roleType);
        role.setDataScopeType(trim(request.getDataScopeType()));
        role.setDescription(trimNullable(request.getDescription()));
        role.setStatus(normalizeStatus(request.getStatus()));
        roleRepository.update(role);
        roleMenuAdministrationService.saveRoleMenus(role, request.getMenuIds());
    }

    @Transactional
    public void updateRoleStatus(RoleDO role, String nextStatus, Long operatorId) {
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        identityAccessControlService.ensureCanManageRole(operatorId, role);
        String normalizedStatus = normalizeStatus(nextStatus);
        if (STATUS_DISABLED.equals(normalizedStatus)) {
            ensureRoleMutable(role);
        }
        role.setStatus(normalizedStatus);
        roleRepository.update(role);
    }

    public RoleDO requireRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
    }

    public boolean isRoleBuiltin(RoleDO role) {
        if (role == null) {
            return false;
        }
        if (PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
            return true;
        }
        return GROUP_ROLE_TEMPLATE_DESC.equals(role.getDescription());
    }

    public boolean isRoleMutable(RoleDO role) {
        if (role == null) {
            return true;
        }
        return !isRoleBuiltin(role);
    }

    public void ensureRoleMutable(RoleDO role) {
        if (!isRoleMutable(role)) {
            throw new BusinessException("内置角色不允许修改或删除");
        }
    }

    private void ensureGroupBuiltinRoles(Long groupId, Long operatorId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        List<RoleDO> templateRoles = roleRepository.findBuiltinTemplateRoles();
        for (RoleDO template : templateRoles) {
            RoleDO existing = roleRepository.findByTenantGroupIdAndRoleCode(groupId, template.getRoleCode()).orElse(null);
            if (existing != null) {
                if (!STATUS_ENABLED.equals(existing.getStatus())) {
                    existing.setStatus(STATUS_ENABLED);
                    roleRepository.update(existing);
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
            roleRepository.save(role);
        }
    }

    private Long resolveTenantGroupId(Long operatorId, boolean platformAdmin, String orgId) {
        if (platformAdmin && !StringUtils.hasText(orgId)) {
            return null;
        }
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScopeAllowAnonymous(operatorId, orgId);
        return scope.groupId();
    }

    private String toTenantDisplayRoleCode(String roleCode, Long tenantGroupId) {
        if (roleCode == null || tenantGroupId == null || tenantGroupId <= 0) {
            return roleCode;
        }
        String prefix = "G" + tenantGroupId + "__";
        if (roleCode.startsWith(prefix)) {
            return roleCode.substring(prefix.length());
        }
        return roleCode;
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

    private List<RoleDO> sortRoles(List<RoleDO> roles) {
        return roles.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
                        return Long.compare(b.getId() == null ? 0L : b.getId(), a.getId() == null ? 0L : a.getId());
                    }
                    if (a.getCreatedAt() == null) {
                        return 1;
                    }
                    if (b.getCreatedAt() == null) {
                        return -1;
                    }
                    int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
                    if (cmp != 0) {
                        return cmp;
                    }
                    return Long.compare(b.getId() == null ? 0L : b.getId(), a.getId() == null ? 0L : a.getId());
                })
                .toList();
    }

    private String generateRoleCode(Long tenantGroupId) {
        List<String> existingCodes = roleRepository.findByTenantGroupId(tenantGroupId).stream()
                .map(RoleDO::getRoleCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(ROLE_CODE_PREFIX, existingCodes);
    }

    public record RoleAdminSnapshot(Long id,
                                    String roleCode,
                                    Long tenantGroupId,
                                    String tenantGroupName,
                                    String roleName,
                                    String roleType,
                                    String dataScopeType,
                                    String description,
                                    String status,
                                    List<Long> menuIds,
                                    Boolean builtin,
                                    Boolean editable) {
    }
}
