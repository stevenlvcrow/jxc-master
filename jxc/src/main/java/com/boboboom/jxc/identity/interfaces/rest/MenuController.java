package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.MenuRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/menus")
public class MenuController {

    private final MenuRepository menuRepository;
    private final OrgScopeService orgScopeService;

    public MenuController(MenuRepository menuRepository, OrgScopeService orgScopeService) {
        this.menuRepository = menuRepository;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping("/current")
    public CodeDataResponse<List<MenuItemResult>> current(@RequestParam(required = false) String orgId) {
        LoginSession session = AuthContextHolder.require();
        OrgScopeService.MenuScope scope = orgScopeService.resolveMenuScope(orgId);
        List<MenuPermissionView> rows = menuRepository.findMenusByUserContext(session.getUserId(), scope.scopeType(), scope.scopeId());

        LinkedHashMap<Long, MenuItemResult> deduped = new LinkedHashMap<>();
        for (MenuPermissionView row : rows) {
            if (!"ENABLED".equals(row.getStatus())) {
                continue;
            }
            if (!"DIRECTORY".equals(row.getMenuType()) && !"MENU".equals(row.getMenuType())) {
                continue;
            }
            deduped.putIfAbsent(row.getId(), new MenuItemResult(
                    row.getId(),
                    row.getMenuCode(),
                    row.getMenuName(),
                    row.getParentId(),
                    row.getMenuType(),
                    row.getRoutePath(),
                    row.getIcon(),
                    row.getSortNo()
            ));
        }
        return CodeDataResponse.ok(new ArrayList<>(deduped.values()));
    }

    public record MenuItemResult(Long id,
                                 String menuCode,
                                 String menuName,
                                 Long parentId,
                                 String menuType,
                                 String routePath,
                                 String icon,
                                 Integer sortNo) {
    }
}
