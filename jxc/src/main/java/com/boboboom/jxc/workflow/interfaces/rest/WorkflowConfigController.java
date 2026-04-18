package com.boboboom.jxc.workflow.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.PublishResultView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowConfigView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowPublishHistoryManageView;
import com.boboboom.jxc.workflow.application.service.WorkflowConfigApplicationService.WorkflowPublishHistoryView;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowConfigSaveRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
/**
 * 流程配置接口，负责当前配置查询、保存、发布、历史查询和删除。
 */
public class WorkflowConfigController {

    private final WorkflowConfigApplicationService workflowConfigApplicationService;

    /**
     * 构造流程配置接口。
     *
     * @param workflowConfigApplicationService 流程配置服务
     */
    public WorkflowConfigController(WorkflowConfigApplicationService workflowConfigApplicationService) {
        this.workflowConfigApplicationService = workflowConfigApplicationService;
    }

    /**
     * 查询当前流程配置。
     *
     * @param orgId 机构标识
     * @param businessCode 业务编码
     * @param workflowCode 流程编码
     * @return 当前配置
     */
    @GetMapping("/current")
    public CodeDataResponse<WorkflowConfigView> getCurrent(@RequestParam(required = false) String orgId,
                                                           @RequestParam String businessCode,
                                                           @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.getCurrent(orgId, businessCode, workflowCode));
    }

    /**
     * 保存当前流程配置。
     *
     * @param orgId 机构标识
     * @param request 保存请求
     * @return 空响应
     */
    @PutMapping("/current")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> save(@RequestParam(required = false) String orgId,
                                       @Valid @RequestBody WorkflowConfigSaveRequest request) {
        workflowConfigApplicationService.save(orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 发布当前流程配置。
     *
     * @param orgId 机构标识
     * @param businessCode 业务编码
     * @param workflowCode 流程编码
     * @return 发布结果
     */
    @PostMapping("/current/publish")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<PublishResultView> publish(@RequestParam(required = false) String orgId,
                                                       @RequestParam String businessCode,
                                                       @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.publish(orgId, businessCode, workflowCode));
    }

    /**
     * 查询流程发布历史。
     *
     * @param orgId 机构标识
     * @param businessCode 业务编码
     * @param workflowCode 流程编码
     * @return 历史列表
     */
    @GetMapping("/history")
    public CodeDataResponse<List<WorkflowPublishHistoryView>> history(@RequestParam(required = false) String orgId,
                                                                      @RequestParam String businessCode,
                                                                      @RequestParam String workflowCode) {
        return CodeDataResponse.ok(workflowConfigApplicationService.history(orgId, businessCode, workflowCode));
    }

    /**
     * 查询发布历史管理列表。
     *
     * @param orgId 机构标识
     * @param limit 返回条数限制
     * @return 历史管理列表
     */
    @GetMapping("/publish-histories")
    public CodeDataResponse<List<WorkflowPublishHistoryManageView>> publishHistories(@RequestParam(required = false) String orgId,
                                                                                     @RequestParam(required = false) Integer limit) {
        return CodeDataResponse.ok(workflowConfigApplicationService.publishHistories(orgId, limit));
    }

    /**
     * 删除流程配置。
     *
     * @param id 配置主键
     * @param orgId 机构标识
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteConfig(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId) {
        workflowConfigApplicationService.deleteConfig(orgId, id);
        return CodeDataResponse.ok();
    }
}
