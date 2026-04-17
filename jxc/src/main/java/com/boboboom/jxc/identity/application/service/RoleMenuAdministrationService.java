package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import org.springframework.stereotype.Service;

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

    private final MenuRepository menuRepository;
    private final RoleMenuRelRepository roleMenuRelRepository;
    private final RoleRepository roleRepository;

    public RoleMenuAdministrationService(MenuRepository menuRepository,
                                         RoleMenuRelRepository roleMenuRelRepository,
                                         RoleRepository roleRepository) {
        this.menuRepository = menuRepository;
        this.roleMenuRelRepository = roleMenuRelRepository;
        this.roleRepository = roleRepository;
    }

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

        RoleDO groupAdminRole = roleRepository.findByRoleCode("GROUP_ADMIN").orElse(null);
        if (groupAdminRole == null) {
            return;
        }

        ensureRoleMenuRel(groupAdminRole.getId(), userMgmt.getId());
        ensureRoleMenuRel(groupAdminRole.getId(), roleMgmt.getId());
        ensureRoleMenuRel(groupAdminRole.getId(), menuPermMgmt.getId());
    }

    public List<MenuAssignmentOption> listAssignableMenus(Long operatorId,
                                                          boolean platformAdmin,
                                                          List<Long> managedGroups,
                                                          List<Long> managedStoreIds) {
        if (platformAdmin) {
            return menuRepository.findAllOrdered()
                    .stream()
                    .map(this::toOption)
                    .toList();
        }
        if (managedGroups == null || managedGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<MenuPermissionView> scopedMenus = new ArrayList<>();
        for (Long groupId : managedGroups) {
            scopedMenus.addAll(menuRepository.findMenusByUserContext(operatorId, "GROUP", groupId));
        }
        if (managedStoreIds != null) {
            for (Long storeId : managedStoreIds) {
                scopedMenus.addAll(menuRepository.findMenusByUserContext(operatorId, "STORE", storeId));
            }
        }

        LinkedHashMap<Long, MenuAssignmentOption> deduped = new LinkedHashMap<>();
        for (MenuPermissionView row : scopedMenus) {
            if (!STATUS_ENABLED.equals(row.getStatus())) {
                continue;
            }
            deduped.putIfAbsent(row.getId(), new MenuAssignmentOption(
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
        return new ArrayList<>(deduped.values());
    }

    public List<Long> listRoleMenuIds(Long roleId) {
        return roleMenuRelRepository.findByRoleId(roleId).stream()
                .map(RoleMenuRelDO::getMenuId)
                .distinct()
                .toList();
    }

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
            if (menu == null || !isMenuAssignableToRoleType(role.getRoleType(), menu.getMenuCode())) {
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
            boolean isStoreMenu = menu.getMenuCode() != null && menu.getMenuCode().startsWith("STORE_BIZ_");
            if ("STORE".equals(role.getRoleType()) && !isStoreMenu) {
                throw new BusinessException("门店角色仅可分配门店菜单");
            }
            if ("GROUP".equals(role.getRoleType()) && isStoreMenu) {
                throw new BusinessException("集团角色仅可分配集团菜单");
            }
        }
    }

    private boolean isMenuAssignableToRoleType(String roleType, String menuCode) {
        String normalizedRoleType = trimToNull(roleType);
        String normalizedMenuCode = trimToNull(menuCode);
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
