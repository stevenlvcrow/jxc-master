package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.OrgAdministrationService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.OrgNodeResult;

/** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
@Validated
@RestController
@RequestMapping({"/org", "/api/identity/org"})
public class OrgController {

    private final OrgAdministrationService orgAdministrationService;

    /** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public OrgController(OrgAdministrationService orgAdministrationServiceValue) {
        this.orgAdministrationService = orgAdministrationServiceValue;
    }

    /** 查询树形业务数据。 */
    @GetMapping("/tree")
    public CodeDataResponse<List<OrgNodeResult>> tree() {
        return CodeDataResponse.ok(orgAdministrationService.tree());
    }
}
