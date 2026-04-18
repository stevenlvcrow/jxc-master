package com.boboboom.jxc.workflow.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.PublishResultView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowConfigView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowPublishHistoryManageView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowPublishHistoryView;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowConfigSaveRequest;
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
@RequestMapping("/api/workflow/configs")
public class WorkflowConfigController {

    private final WorkflowConfigApplicationService workflowConfigApplicationService;

    public WorkflowConfigController(WorkflowConfigApplicationService workflowConfigApplicationService) {
        this.workflowConfigApplicationService = workflowConfigApplicationService;
    }

    @GetMapping("/current")
    public CodeDataResponse<WorkflowConfigView> getCurrent(@RequestParam(required = false) String orgId,
                                                           @RequestParam String businessCode,
                                                           @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.getCurrent(orgId, businessCode, workflowCode));
    }

    @PutMapping("/current")
    public CodeDataResponse<Void> save(@RequestParam(required = false) String orgId,
                                       @Valid @RequestBody WorkflowConfigSaveRequest request) {
        workflowConfigApplicationService.save(orgId, request);
        return CodeDataResponse.ok();
    }

    @PostMapping("/current/publish")
    public CodeDataResponse<PublishResultView> publish(@RequestParam(required = false) String orgId,
                                                       @RequestParam String businessCode,
                                                       @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.publish(orgId, businessCode, workflowCode));
    }

    @GetMapping("/history")
    public CodeDataResponse<List<WorkflowPublishHistoryView>> history(@RequestParam(required = false) String orgId,
                                                                      @RequestParam String businessCode,
                                                                      @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.history(orgId, businessCode, workflowCode));
    }

    @GetMapping("/publish-histories")
    public CodeDataResponse<List<WorkflowPublishHistoryManageView>> publishHistories(@RequestParam(required = false) String orgId,
                                                                                     @RequestParam(required = false) Integer limit) {
        return CodeDataResponse.ok(workflowConfigApplicationService.publishHistories(orgId, limit));
    }

    @DeleteMapping("/{id}")
    public CodeDataResponse<Void> deleteConfig(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId) {
        workflowConfigApplicationService.deleteConfig(orgId, id);
        return CodeDataResponse.ok();
    }
}
