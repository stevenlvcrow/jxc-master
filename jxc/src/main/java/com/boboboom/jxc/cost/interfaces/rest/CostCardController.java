package com.boboboom.jxc.cost.interfaces.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.cost.application.service.CostCardApplicationService;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardDetail;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardRow;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardSaveRequest;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.DishCostCardBindingRequest;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.IdPayload;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;

import jakarta.validation.Valid;

/**
 * 成本卡接口，提供成本卡档案、版本启停和菜品绑定能力。
 */
@Validated
@RestController
@RequestMapping("/api/cost/cards")
public class CostCardController {

    private final CostCardApplicationService costCardApplicationService;

    /** 成本卡接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public CostCardController(CostCardApplicationService costCardApplicationServiceValue) {
        this.costCardApplicationService = costCardApplicationServiceValue;
    }

    /**
     * 分页查询成本卡。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param status 状态
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<PageData<CostCardRow>> page(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(costCardApplicationService.page(pageNo, pageSize, keyword, status, orgId));
    }

    /**
     * 查询成本卡详情。
     *
     * @param id 成本卡 ID
     * @param orgId 机构标识
     * @return 成本卡详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<CostCardDetail> detail(@PathVariable Long id,
                                                   @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(costCardApplicationService.detail(id, orgId));
    }

    /**
     * 创建成本卡。
     *
     * @param request 请求
     * @param orgId 机构标识
     * @return 创建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@Valid @RequestBody CostCardSaveRequest request,
                                              @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(costCardApplicationService.create(request, orgId));
    }

    /**
     * 更新成本卡。
     *
     * @param id 成本卡 ID
     * @param request 请求
     * @param orgId 机构标识
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @Valid @RequestBody CostCardSaveRequest request,
                                         @RequestParam(required = false) String orgId) {
        costCardApplicationService.update(id, request, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 启用成本卡版本。
     *
     * @param id 成本卡 ID
     * @param versionId 版本 ID
     * @param orgId 机构标识
     * @return 空响应
     */
    @PostMapping("/{id}/versions/{versionId}/activate")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> activate(@PathVariable Long id,
                                           @PathVariable Long versionId,
                                           @RequestParam(required = false) String orgId) {
        costCardApplicationService.activateVersion(id, versionId, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 停用成本卡。
     *
     * @param id 成本卡 ID
     * @param orgId 机构标识
     * @return 空响应
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> disable(@PathVariable Long id,
                                          @RequestParam(required = false) String orgId) {
        costCardApplicationService.disable(id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 绑定菜品和默认扣减仓。
     *
     * @param id 成本卡 ID
     * @param request 请求
     * @param orgId 机构标识
     * @return 空响应
     */
    @PostMapping("/{id}/dish-bindings")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> bindDish(@PathVariable Long id,
                                           @Valid @RequestBody DishCostCardBindingRequest request,
                                           @RequestParam(required = false) String orgId) {
        costCardApplicationService.bindDish(id, request, orgId);
        return CodeDataResponse.ok();
    }
}
