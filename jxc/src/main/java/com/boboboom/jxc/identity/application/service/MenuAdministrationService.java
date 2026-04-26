package com.boboboom.jxc.identity.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.domain.repository.RoleMenuRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.interfaces.rest.request.MenuSortRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.MenuUpsertRequest;

/** 菜单维护业务服务，负责菜单配置的新增、编辑、排序和显示状态维护。 */
@Service
public class MenuAdministrationService {

    private static final Set<String> MENU_TYPES = Set.of("DIRECTORY", "MENU", "BUTTON", "API");

    private final MenuRepository menuRepository;
    private final RoleMenuRelRepository roleMenuRelRepository;

    /** 菜单维护业务服务，负责菜单配置的新增、编辑、排序和显示状态维护。 */
    public MenuAdministrationService(MenuRepository menuRepositoryValue,
                                     RoleMenuRelRepository roleMenuRelRepositoryValue) {
        this.menuRepository = menuRepositoryValue;
        this.roleMenuRelRepository = roleMenuRelRepositoryValue;
    }

    /** 查询菜单列表。 */
    public List<MenuDO> listMenus() {
        return menuRepository.findAllOrdered();
    }

    /** 创建业务记录。 */
    @Transactional
    public MenuDO create(MenuUpsertRequest request) {
        validateRequest(null, request);
        MenuDO menu = new MenuDO();
        apply(menu, request);
        menuRepository.save(menu);
        return menu;
    }

    /** 更新业务记录。 */
    @Transactional
    public void update(Long id, MenuUpsertRequest request) {
        MenuDO menu = requireMenu(id);
        validateRequest(id, request);
        apply(menu, request);
        menuRepository.update(menu);
    }

    /** 删除业务记录。 */
    @Transactional
    public void delete(Long id) {
        MenuDO menu = requireMenu(id);
        if (!menuRepository.findByParentId(menu.getId()).isEmpty()) {
            throw new BusinessException("存在子菜单，不能删除");
        }
        roleMenuRelRepository.deleteByMenuId(menu.getId());
        menuRepository.deleteById(menu.getId());
    }

    /** 处理sort。 */
    @Transactional
    public void sort(MenuSortRequest request) {
        List<MenuSortRequest.MenuSortItem> items = request.getItems();
        for (MenuSortRequest.MenuSortItem item : items) {
            MenuDO menu = requireMenu(item.getId());
            validateParent(menu.getId(), item.getParentId());
            menu.setParentId(item.getParentId());
            menu.setSortNo(item.getSortNo());
            menuRepository.update(menu);
        }
    }

    private MenuDO requireMenu(Long id) {
        return menuRepository.findById(id).orElseThrow(() -> new BusinessException("菜单不存在"));
    }

    private void validateRequest(Long id, MenuUpsertRequest request) {
        String menuCode = trimToNull(request.getMenuCode());
        String menuName = trimToNull(request.getMenuName());
        String menuType = trimToNull(request.getMenuType());
        String status = trimToNull(request.getStatus());
        if (menuCode == null) {
            throw new BusinessException("菜单编码不能为空");
        }
        if (menuName == null) {
            throw new BusinessException("菜单名称不能为空");
        }
        if (!MENU_TYPES.contains(menuType)) {
            throw new BusinessException("菜单类型不正确");
        }
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException("状态不正确");
        }
        if (request.getSortNo() == null) {
            throw new BusinessException("排序号不能为空");
        }
        if (request.getVisible() == null) {
            throw new BusinessException("是否可见不能为空");
        }
        MenuDO exists = menuRepository.findByMenuCode(menuCode).orElse(null);
        if (exists != null && !Objects.equals(exists.getId(), id)) {
            throw new BusinessException("菜单编码已存在");
        }
        validateParent(id, request.getParentId());
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == null) {
            return;
        }
        MenuDO parent = requireMenu(parentId);
        if ("BUTTON".equals(parent.getMenuType()) || "API".equals(parent.getMenuType())) {
            throw new BusinessException("按钮或接口不能作为父级");
        }
        if (id == null) {
            return;
        }
        if (Objects.equals(id, parentId)) {
            throw new BusinessException("父级不能选择当前菜单");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parent.getParentId();
        while (cursor != null) {
            if (!visited.add(cursor)) {
                throw new BusinessException("菜单层级存在循环");
            }
            if (Objects.equals(cursor, id)) {
                throw new BusinessException("父级不能选择当前菜单的下级");
            }
            cursor = requireMenu(cursor).getParentId();
        }
    }

    private void apply(MenuDO menu, MenuUpsertRequest request) {
        menu.setMenuCode(trimToNull(request.getMenuCode()));
        menu.setMenuName(trimToNull(request.getMenuName()));
        menu.setParentId(request.getParentId());
        menu.setMenuType(trimToNull(request.getMenuType()));
        menu.setRoutePath(trimToNull(request.getRoutePath()));
        menu.setComponentKey(trimToNull(request.getComponentKey()));
        menu.setPermissionCode(trimToNull(request.getPermissionCode()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSortNo(request.getSortNo());
        menu.setVisible(request.getVisible());
        menu.setStatus(trimToNull(request.getStatus()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
