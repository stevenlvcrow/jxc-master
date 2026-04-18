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
@RequestMapping("/api/workflow/processes")
/**
 * 流程模板接口，负责流程模板列表、门店列表、创建、修改、绑定和删除。
 */
public class WorkflowProcessController {

    private final WorkflowProcessApplicationService workflowProcessApplicationService;

    /**
     * 构造流程模板接口。
     *
     * @param workflowProcessApplicationService 流程模板服务
     */
    public WorkflowProcessController(WorkflowProcessApplicationService workflowProcessApplicationService) {
        this.workflowProcessApplicationService = workflowProcessApplicationService;
    }

    /**
     * 查询流程模板列表。
     *
     * @param orgId 机构标识
     * @return 流程模板列表
     */
    @GetMapping
    public CodeDataResponse<List<WorkflowProcessView>> list(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(workflowProcessApplicationService.list(orgId));
    }

    /**
     * 查询可绑定的门店列表。
     *
     * @param orgId 机构标识
     * @return 门店列表
     */
    @GetMapping("/stores")
    public CodeDataResponse<List<StoreOptionView>> listStores(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(workflowProcessApplicationService.listStores(orgId));
    }

    /**
     * 新建流程模板。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        return CodeDataResponse.ok(workflowProcessApplicationService.create(orgId, request));
    }

    /**
     * 更新流程模板。
     *
     * @param id 流程主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        workflowProcessApplicationService.update(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/bind-template")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 绑定流程模板到业务模板。
     *
     * @param id 流程主键
     * @param orgId 机构标识
     * @param request 绑定请求
     * @return 空响应
     */
    public CodeDataResponse<Void> bindTemplate(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody WorkflowTemplateBindRequest request) {
        workflowProcessApplicationService.bindTemplate(id, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 绑定流程模板到门店。
     *
     * @param id 流程主键
     * @param orgId 机构标识
     * @param request 绑定请求
     * @return 空响应
     */
    @PutMapping("/{id}/bind-stores")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> bindStores(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody WorkflowProcessStoreBindRequest request) {
        workflowProcessApplicationService.bindStores(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除流程模板。
     *
     * @param id 流程主键
     * @param orgId 机构标识
     * @return 空响应
     */
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        workflowProcessApplicationService.delete(id, orgId);
        return CodeDataResponse.ok();
    }
}
