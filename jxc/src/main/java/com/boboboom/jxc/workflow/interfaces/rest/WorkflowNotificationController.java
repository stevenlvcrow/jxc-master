package com.boboboom.jxc.workflow.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService.PageData;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService.WorkflowApprovalNotificationView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程审批消息接口，负责展示当前机构下的审批通过、拒绝消息。
 */
@Validated
@RestController
@RequestMapping("/api/workflow/notifications")
public class WorkflowNotificationController {

    private final WorkflowApprovalNotificationApplicationService notificationApplicationService;

    /**
     * 构造流程审批消息接口。
     *
     * @param notificationApplicationService 流程审批消息服务
     */
    public WorkflowNotificationController(WorkflowApprovalNotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 分页查询审批消息列表。
     *
     * @param orgId 机构标识
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 审批消息分页结果
     */
    @GetMapping
    public CodeDataResponse<PageData<WorkflowApprovalNotificationView>> page(@RequestParam(required = false) String orgId,
                                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return CodeDataResponse.ok(notificationApplicationService.page(orgId, pageNum, pageSize));
    }

    /**
     * 查询当前用户待审批消息数量。
     *
     * @param orgId 机构标识
     * @return 待审批消息数量
     */
    @GetMapping("/pending-count")
    public CodeDataResponse<Long> pendingCount(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(notificationApplicationService.pendingCount(orgId));
    }
}
