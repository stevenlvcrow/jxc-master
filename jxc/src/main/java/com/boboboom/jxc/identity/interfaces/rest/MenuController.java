package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.MenuApplicationService;
import com.boboboom.jxc.identity.application.service.MenuApplicationService.MenuItemData;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;

/** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
@Validated
@RestController
@RequestMapping("/api/identity/menus")
public class MenuController {

    private final MenuApplicationService menuApplicationService;

    /** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public MenuController(MenuApplicationService menuApplicationServiceValue) {
        this.menuApplicationService = menuApplicationServiceValue;
    }

    /** 处理GetMapping。 */
    @GetMapping("/current")
    public CodeDataResponse<List<MenuItemData>> current(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(menuApplicationService.current(orgId));
    }
}
