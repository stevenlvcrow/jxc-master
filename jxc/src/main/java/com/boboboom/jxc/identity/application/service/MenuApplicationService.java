package com.boboboom.jxc.identity.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentWorkflowService;

/** 菜单业务服务，负责当前用户菜单树和菜单权限数据组装。 */
@Service
public class MenuApplicationService {

    private static final String PURCHASE_APPLICATION_REVIEW_MENU = "STORE_BIZ_MENU_PURCHASE_APPLICATION_REVIEW";
    private static final String PURCHASE_APPLICATION_BUSINESS_CODE = "PURCHASE_APPLICATION";

    private final MenuRepository menuRepository;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;
    private final InventoryDocumentWorkflowService workflowService;

    /** 菜单业务服务，负责当前用户菜单树和菜单权限数据组装。 */
    public MenuApplicationService(MenuRepository menuRepositoryValue,
                                  OrgScopeService orgScopeServiceValue,
                                  DictionaryLookupService dictionaryLookupServiceValue,
                                  InventoryDocumentWorkflowService workflowServiceValue) {
        this.menuRepository = menuRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
        this.workflowService = workflowServiceValue;
    }

    /** 查询当前登录用户菜单和权限信息。 */
    public List<MenuItemData> current(String orgId) {
        LoginSession session = AuthContextHolder.require();
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(session.getUserId(), orgId);
        List<MenuPermissionView> rows = menuRepository.findMenusByUserContext(
                session.getUserId(),
                scope.scopeType(),
                scope.scopeId()
        );

        LinkedHashMap<Long, MenuItemData> deduped = new LinkedHashMap<>();
        for (MenuPermissionView row : rows) {
            if (!enabledStatus().equals(row.getStatus())) {
                continue;
            }
            if (!"DIRECTORY".equals(row.getMenuType()) && !"MENU".equals(row.getMenuType())) {
                continue;
            }
            if (!Boolean.TRUE.equals(row.getVisible())) {
                continue;
            }
            if (!allParentsVisible(row, rows)) {
                continue;
            }
            if (!allowedSpecialMenu(row, scope, session.getUserId())) {
                continue;
            }
            deduped.putIfAbsent(row.getId(), new MenuItemData(
                    row.getId(),
                    row.getMenuCode(),
                    row.getMenuName(),
                    row.getParentId(),
                    row.getMenuType(),
                    row.getRoutePath(),
                    row.getComponentKey(),
                    row.getIcon(),
                    row.getSortNo()
            ));
        }
        return new ArrayList<>(deduped.values());
    }

    private boolean allowedSpecialMenu(MenuPermissionView row,
                                       OrgScopeService.AccessibleScope scope,
                                       Long operatorId) {
        if (!PURCHASE_APPLICATION_REVIEW_MENU.equals(row.getMenuCode())) {
            return true;
        }
        return workflowService.hasBusinessReviewPermission(
                PURCHASE_APPLICATION_BUSINESS_CODE,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
    }

    private boolean allParentsVisible(MenuPermissionView row, List<MenuPermissionView> rows) {
        LinkedHashMap<Long, MenuPermissionView> rowMap = new LinkedHashMap<>();
        for (MenuPermissionView item : rows) {
            rowMap.putIfAbsent(item.getId(), item);
        }
        Set<Long> visited = new HashSet<>();
        Long parentId = row.getParentId();
        while (parentId != null) {
            if (!visited.add(parentId)) {
                return false;
            }
            MenuPermissionView parent = rowMap.get(parentId);
            if (parent == null) {
                return true;
            }
            if (!enabledStatus().equals(parent.getStatus()) || !Boolean.TRUE.equals(parent.getVisible())) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return true;
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public record MenuItemData(Long id,
                               String menuCode,
                               String menuName,
                               Long parentId,
                               String menuType,
                               String routePath,
                               String componentKey,
                               String icon,
                               Integer sortNo) {
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }
}
