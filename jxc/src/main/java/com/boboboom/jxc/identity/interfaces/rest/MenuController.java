package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.MenuApplicationService;
import com.boboboom.jxc.identity.application.service.MenuApplicationService.MenuItemData;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/menus")
public class MenuController {

    private final MenuApplicationService menuApplicationService;

    public MenuController(MenuApplicationService menuApplicationService) {
        this.menuApplicationService = menuApplicationService;
    }

    @GetMapping("/current")
    public CodeDataResponse<List<MenuItemData>> current(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(menuApplicationService.current(orgId));
    }
}
