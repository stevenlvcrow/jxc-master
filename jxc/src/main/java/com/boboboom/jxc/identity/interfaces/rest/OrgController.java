package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.OrgAdministrationService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.OrgNodeResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping({"/org", "/api/identity/org"})
public class OrgController {

    private final OrgAdministrationService orgAdministrationService;

    public OrgController(OrgAdministrationService orgAdministrationService) {
        this.orgAdministrationService = orgAdministrationService;
    }

    @GetMapping("/tree")
    public CodeDataResponse<List<OrgNodeResult>> tree() {
        return CodeDataResponse.ok(orgAdministrationService.tree());
    }
}
