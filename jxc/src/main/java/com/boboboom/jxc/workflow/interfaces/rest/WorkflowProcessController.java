package com.boboboom.jxc.workflow.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.application.service.WorkflowProcessApplicationService;
import com.boboboom.jxc.workflow.application.service.WorkflowProcessApplicationService.IdPayload;
import com.boboboom.jxc.workflow.application.service.WorkflowProcessApplicationService.StoreOptionView;
import com.boboboom.jxc.workflow.application.service.WorkflowProcessApplicationService.WorkflowProcessView;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessStoreBindRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessUpsertRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowTemplateBindRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/workflow/processes")
public class WorkflowProcessController {

    private final WorkflowProcessApplicationService workflowProcessApplicationService;

    public WorkflowProcessController(WorkflowProcessApplicationService workflowProcessApplicationService) {
        this.workflowProcessApplicationService = workflowProcessApplicationService;
    }

    @GetMapping
    public CodeDataResponse<List<WorkflowProcessView>> list(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(workflowProcessApplicationService.list(orgId));
    }

    @GetMapping("/stores")
    public CodeDataResponse<List<StoreOptionView>> listStores(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(workflowProcessApplicationService.listStores(orgId));
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        return CodeDataResponse.ok(workflowProcessApplicationService.create(orgId, request));
    }

    @PutMapping("/{id}")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        workflowProcessApplicationService.update(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/bind-template")
    public CodeDataResponse<Void> bindTemplate(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody WorkflowTemplateBindRequest request) {
        workflowProcessApplicationService.bindTemplate(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/bind-stores")
    public CodeDataResponse<Void> bindStores(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody WorkflowProcessStoreBindRequest request) {
        workflowProcessApplicationService.bindStores(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        workflowProcessApplicationService.delete(id, orgId);
        return CodeDataResponse.ok();
    }
}
