package com.boboboom.jxc.identity.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.MenuMapper;
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

    private final MenuMapper menuMapper;

    public MenuController(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @GetMapping("/current")
    public CodeDataResponse<List<MenuItemResult>> current(@RequestParam(required = false) String orgId) {
        LoginSession session = AuthContextHolder.require();
        Scope scope = parseScope(orgId);
        List<MenuPermissionView> rows = menuMapper.selectMenusByUserContext(session.getUserId(), scope.scopeType(), scope.scopeId());

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

    private Scope parseScope(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return new Scope("PLATFORM", null);
        }
        if (orgId.startsWith("group-")) {
            return new Scope("GROUP", parseNumericId(orgId.substring("group-".length())));
        }
        if (orgId.startsWith("store-")) {
            return new Scope("STORE", parseNumericId(orgId.substring("store-".length())));
        }
        throw new BusinessException("机构参数非法");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private record Scope(String scopeType, Long scopeId) {
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
