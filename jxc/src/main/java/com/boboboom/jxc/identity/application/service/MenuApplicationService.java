package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class MenuApplicationService {

    private final MenuRepository menuRepository;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;

    public MenuApplicationService(MenuRepository menuRepository,
                                  OrgScopeService orgScopeService,
                                  DictionaryLookupService dictionaryLookupService) {
        this.menuRepository = menuRepository;
        this.orgScopeService = orgScopeService;
        this.dictionaryLookupService = dictionaryLookupService;
    }

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
