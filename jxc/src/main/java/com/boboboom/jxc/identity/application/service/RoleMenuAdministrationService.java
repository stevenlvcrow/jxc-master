package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoleMenuAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String PLATFORM_SUPER_ADMIN_ROLE_CODE = "PLATFORM_SUPER_ADMIN";
    private static final String GROUP_ADMIN_ROLE_CODE = "GROUP_ADMIN";
    private static final String PLATFORM_ROLE_TYPE = "PLATFORM";
    private static final String GROUP_ROLE_TYPE = "GROUP";
    private static final String STORE_ROLE_TYPE = "STORE";
    private static final String GROUP_MENU_PREFIX = "GROUP_";
    private static final String STORE_MENU_PREFIX = "STORE_BIZ_";
    private static final LinkedHashSet<String> PLATFORM_TEMPLATE_MENU_CODES = new LinkedHashSet<>(List.of(
            "STORE_BIZ_MOD_08",
            "STORE_BIZ_GRP_08_01",
            "STORE_BIZ_MENU_08_01_01",
            "STORE_BIZ_MENU_08_01_02",
            "STORE_BIZ_MENU_08_01_03",
            "STORE_BIZ_MENU_08_01_04",
            "STORE_BIZ_MENU_08_01_05"
    ));

    private final MenuRepository menuRepository;
    private final RoleMenuRelRepository roleMenuRelRepository;
    private final RoleRepository roleRepository;
    private final OrgScopeService orgScopeService;

    public RoleMenuAdministrationService(MenuRepository menuRepository,
                                         RoleMenuRelRepository roleMenuRelRepository,
                                         RoleRepository roleRepository,
                                         OrgScopeService orgScopeService) {
        this.menuRepository = menuRepository;
        this.roleMenuRelRepository = roleMenuRelRepository;
        this.roleRepository = roleRepository;
        this.orgScopeService = orgScopeService;
    }

    @Transactional
    public void ensureGroupMgmtMenusForGroupAdmin() {
        MenuDO groupMgmt = menuRepository.findByMenuCode("GROUP_MGMT").orElse(null);
        if (groupMgmt == null) {
            return;
        }

        MenuDO userMgmt = ensureMenu(
                "GROUP_USER_ROLE_MGMT",
                "用户管理",
                groupMgmt.getId(),
                "/group/user-role",
                "group/user-role/index",
                "group:user-role:manage",
                "user",
                46
        );
        MenuDO roleMgmt = ensureMenu(
                "GROUP_ROLE_MGMT",
                "角色管理",
                groupMgmt.getId(),
                "/group/roles",
                "group/roles/index",
                "group:role:manage",
                "team",
                47
        );
        MenuDO menuPermMgmt = ensureMenu(
                "GROUP_MENU_PERMISSION_MGMT",
                "菜单权限管理",
                groupMgmt.getId(),
                "/group/menu-permissions",
                "group/menu-permissions/index",
                "group:menu-permission:manage",
                "setting",
                48
        );
        MenuDO archiveRoot = ensureMenu(
                "STORE_BIZ_MOD_08",
                "档案管理",
                null,
                null,
                null,
                null,
                "document",
                108
        );
        MenuDO archiveGroup = ensureMenu(
                "STORE_BIZ_GRP_08_01",
                "物品",
                archiveRoot.getId(),
                null,
                null,
                null,
                null,
                1801
        );
        MenuDO archiveItem = ensureMenu(
                "STORE_BIZ_MENU_08_01_01",
                "物品管理",
                archiveGroup.getId(),
                "/archive/1/1",
                "views/feature/index",
                "store:biz:08:01:01:view",
                null,
                108011
        );
        MenuDO archiveCategory = ensureMenu(
                "STORE_BIZ_MENU_08_01_02",
                "物品类别管理",
                archiveGroup.getId(),
                "/archive/1/2",
                "views/feature/index",
                "store:biz:08:01:02:view",
                null,
                108012
        );
        MenuDO archiveUnit = ensureMenu(
                "STORE_BIZ_MENU_08_01_03",
                "单位管理",
                archiveGroup.getId(),
                "/archive/1/3",
                "views/feature/index",
                "store:biz:08:01:03:view",
                null,
                108013
        );
        MenuDO archiveStatistics = ensureMenu(
                "STORE_BIZ_MENU_08_01_04",
                "统计类型管理",
                archiveGroup.getId(),
                "/archive/1/4",
                "views/feature/index",
                "store:biz:08:01:04:view",
                null,
                108014
        );
        MenuDO archiveTag = ensureMenu(
                "STORE_BIZ_MENU_08_01_05",
                "物品标签管理",
                archiveGroup.getId(),
                "/archive/1/5",
                "views/feature/index",
                "store:biz:08:01:05:view",
                null,
                108015
        );

        RoleDO groupAdminRole = roleRepository.findByRoleCode(GROUP_ADMIN_ROLE_CODE).orElse(null);
        RoleDO platformSuperAdminRole = roleRepository.findByRoleCode(PLATFORM_SUPER_ADMIN_ROLE_CODE).orElse(null);
        if (groupAdminRole != null) {
            ensureRoleMenuRel(groupAdminRole.getId(), userMgmt.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), roleMgmt.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), menuPermMgmt.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveRoot.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveGroup.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveItem.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveCategory.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveUnit.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveStatistics.getId());
            ensureRoleMenuRel(groupAdminRole.getId(), archiveTag.getId());
        }
        if (platformSuperAdminRole != null) {
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveRoot.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveGroup.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveItem.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveCategory.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveUnit.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveStatistics.getId());
            ensureRoleMenuRel(platformSuperAdminRole.getId(), archiveTag.getId());
        }
    }

    public List<MenuAssignmentOption> listAssignableMenus(Long operatorId,
                                                          boolean platformAdmin,
                                                          String orgId) {
        if (!platformAdmin) {
            resolveTenantGroupId(operatorId, false, orgId);
        }
        return menuRepository.findAllOrdered()
                .stream()
                .filter(menu -> STATUS_ENABLED.equals(menu.getStatus()))
                .filter(menu -> platformAdmin || isAssignableForManagedRole(menu.getMenuCode()))
                .map(this::toOption)
                .toList();
    }

    public List<Long> listRoleMenuIds(Long roleId) {
        return roleMenuRelRepository.findByRoleId(roleId).stream()
                .map(RoleMenuRelDO::getMenuId)
                .distinct()
                .toList();
    }

    @Transactional
    public void saveRoleMenus(RoleDO role, List<Long> menuIds) {
        if (role == null || role.getId() == null) {
            throw new BusinessException("角色不存在");
        }

        LinkedHashSet<Long> dedupedMenuIds = menuIds == null
                ? new LinkedHashSet<>()
                : menuIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        validateRoleMenusByType(role, dedupedMenuIds);
        roleMenuRelRepository.deleteByRoleId(role.getId());
        if (dedupedMenuIds.isEmpty()) {
            return;
        }

        List<MenuDO> validMenus = menuRepository.findByIds(new ArrayList<>(dedupedMenuIds));
        Map<Long, MenuDO> validMenuMap = validMenus.stream().collect(Collectors.toMap(MenuDO::getId, item -> item));
        for (Long menuId : dedupedMenuIds) {
            MenuDO menu = validMenuMap.get(menuId);
            if (menu == null || !isMenuAssignableToRole(role, menu.getMenuCode())) {
                continue;
            }
            RoleMenuRelDO rel = new RoleMenuRelDO();
            rel.setRoleId(role.getId());
            rel.setMenuId(menuId);
            roleMenuRelRepository.save(rel);
        }
    }

    private MenuDO ensureMenu(String menuCode,
                              String menuName,
                              Long parentId,
                              String routePath,
                              String componentPath,
                              String permissionCode,
                              String icon,
                              int sortNo) {
        MenuDO menu = menuRepository.findByMenuCode(menuCode).orElse(null);
        if (menu == null) {
            menu = new MenuDO();
            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setParentId(parentId);
            menu.setMenuType("MENU");
            menu.setRoutePath(routePath);
            menu.setComponentPath(componentPath);
            menu.setPermissionCode(permissionCode);
            menu.setIcon(icon);
            menu.setSortNo(sortNo);
            menu.setVisible(Boolean.TRUE);
            menu.setStatus(STATUS_ENABLED);
            menuRepository.save(menu);
            return menu;
        }

        boolean changed = false;
        if (!Objects.equals(menu.getMenuName(), menuName)) {
            menu.setMenuName(menuName);
            changed = true;
        }
        if (!Objects.equals(menu.getParentId(), parentId)) {
            menu.setParentId(parentId);
            changed = true;
        }
        if (changed) {
            menuRepository.update(menu);
        }
        return menu;
    }

    private void ensureRoleMenuRel(Long roleId, Long menuId) {
        if (roleId == null || menuId == null) {
            return;
        }
        RoleMenuRelDO exists = roleMenuRelRepository.findByRoleIdAndMenuId(roleId, menuId).orElse(null);
        if (exists != null) {
            return;
        }
        RoleMenuRelDO rel = new RoleMenuRelDO();
        rel.setRoleId(roleId);
        rel.setMenuId(menuId);
        roleMenuRelRepository.save(rel);
    }

    private void validateRoleMenusByType(RoleDO role, LinkedHashSet<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        List<MenuDO> menus = menuRepository.findByIds(new ArrayList<>(menuIds));
        Map<Long, MenuDO> menuMap = menus.stream().collect(Collectors.toMap(MenuDO::getId, item -> item));
        for (Long menuId : menuIds) {
            MenuDO menu = menuMap.get(menuId);
            if (menu == null) {
                continue;
            }
            if (PLATFORM_SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
                continue;
            }
            if (!isMenuAssignableToRole(role, menu.getMenuCode())) {
                throw new BusinessException(buildRoleMenuValidationMessage(role.getRoleType()));
            }
        }
    }

    private boolean isMenuAssignableToRole(RoleDO role, String menuCode) {
        if (role == null) {
            return false;
        }
        if (PLATFORM_SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            return true;
        }
        return isMenuAssignableToRoleType(role.getRoleType(), menuCode);
    }

    private boolean isMenuAssignableToRoleType(String roleType, String menuCode) {
        String normalizedRoleType = trimToNull(roleType);
        String normalizedMenuCode = trimToNull(menuCode);
        if (normalizedMenuCode == null) {
            return false;
        }
        if (PLATFORM_ROLE_TYPE.equals(normalizedRoleType)) {
            return PLATFORM_TEMPLATE_MENU_CODES.contains(normalizedMenuCode);
        }
        if (GROUP_ROLE_TYPE.equals(normalizedRoleType)) {
            return normalizedMenuCode.startsWith(GROUP_MENU_PREFIX) || PLATFORM_TEMPLATE_MENU_CODES.contains(normalizedMenuCode);
        }
        if (STORE_ROLE_TYPE.equals(normalizedRoleType)) {
            return normalizedMenuCode.startsWith(STORE_MENU_PREFIX);
        }
        return false;
    }

    private boolean isAssignableForManagedRole(String menuCode) {
        String normalizedMenuCode = trimToNull(menuCode);
        if (normalizedMenuCode == null) {
            return false;
        }
        return normalizedMenuCode.startsWith(GROUP_MENU_PREFIX) || normalizedMenuCode.startsWith(STORE_MENU_PREFIX);
    }

    private String buildRoleMenuValidationMessage(String roleType) {
        String normalizedRoleType = trimToNull(roleType);
        if (PLATFORM_ROLE_TYPE.equals(normalizedRoleType)) {
            return "平台角色仅可分配基础模板菜单";
        }
        if (GROUP_ROLE_TYPE.equals(normalizedRoleType)) {
            return "集团角色仅可分配集团菜单和基础模板菜单";
        }
        if (STORE_ROLE_TYPE.equals(normalizedRoleType)) {
            return "门店角色仅可分配门店菜单";
        }
        return "当前角色类型不支持菜单分配";
    }

    private MenuAssignmentOption toOption(MenuDO menu) {
        return new MenuAssignmentOption(
                menu.getId(),
                menu.getMenuCode(),
                menu.getMenuName(),
                menu.getParentId(),
                menu.getMenuType(),
                menu.getRoutePath(),
                menu.getPermissionCode(),
                menu.getStatus(),
                menu.getSortNo()
        );
    }

    private Long resolveTenantGroupId(Long operatorId, boolean platformAdmin, String orgId) {
        if (platformAdmin && trimToNull(orgId) == null) {
            return null;
        }
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScopeAllowAnonymous(operatorId, orgId);
        return scope.groupId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record MenuAssignmentOption(Long id,
                                       String menuCode,
                                       String menuName,
                                       Long parentId,
                                       String menuType,
                                       String routePath,
                                       String permissionCode,
                                       String status,
                                       Integer sortNo) {
    }
}
