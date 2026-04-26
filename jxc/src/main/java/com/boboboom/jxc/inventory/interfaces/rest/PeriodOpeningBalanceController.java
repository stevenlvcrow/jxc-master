package com.boboboom.jxc.inventory.interfaces.rest;

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

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.IdPayload;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningDetail;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningGenerateRequest;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningPage;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningPermissionView;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningRejectRequest;
import com.boboboom.jxc.inventory.application.service.PeriodOpeningBalanceApplicationService.PeriodOpeningSaveRequest;

import jakarta.validation.Valid;

/**
 * 周期期初库存接口。
 */
@Validated
@RestController
@RequestMapping("/api/inventory/period-openings")
public class PeriodOpeningBalanceController {

    private final PeriodOpeningBalanceApplicationService periodOpeningBalanceApplicationService;

    /** 周期期初库存接口。 */
    public PeriodOpeningBalanceController(PeriodOpeningBalanceApplicationService periodOpeningBalanceApplicationServiceValue) {
        this.periodOpeningBalanceApplicationService = periodOpeningBalanceApplicationServiceValue;
    }

    /**
     * 查询期初库存列表。
     *
     * @param orgId         机构标识
     * @param pageNum       页码
     * @param pageSize      每页数量
     * @param documentCode  期初单号
     * @param warehouseName 仓库名称
     * @param periodType    周期类型
     * @param startDate     开始日期
     * @param endDate       结束日期
     * @param status        状态
     * @return 分页结果
     */
    @GetMapping
    public CodeDataResponse<PeriodOpeningPage> list(@RequestParam(required = false) String orgId,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String documentCode,
                                                    @RequestParam(required = false) String warehouseName,
                                                    @RequestParam(required = false) String periodType,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(required = false) String status) {
        return CodeDataResponse.ok(periodOpeningBalanceApplicationService.list(
                orgId,
                pageNum,
                pageSize,
                documentCode,
                warehouseName,
                periodType,
                startDate,
                endDate,
                status
        ));
    }

    /**
     * 查询期初库存页面权限。
     *
     * @param orgId 机构标识
     * @return 权限视图
     */
    @GetMapping("/permissions")
    public CodeDataResponse<PeriodOpeningPermissionView> permissions(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(periodOpeningBalanceApplicationService.permissions(orgId));
    }

    /**
     * 创建手工期初库存。
     *
     * @param orgId   机构标识
     * @param request 保存请求
     * @return 创建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody PeriodOpeningSaveRequest request) {
        return CodeDataResponse.ok(periodOpeningBalanceApplicationService.create(orgId, request));
    }

    /**
     * 从上一周期生成期初库存。
     *
     * @param orgId   机构标识
     * @param request 生成请求
     * @return 创建结果
     */
    @PostMapping("/generate")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> generate(@RequestParam(required = false) String orgId,
                                                @Valid @RequestBody PeriodOpeningGenerateRequest request) {
        return CodeDataResponse.ok(periodOpeningBalanceApplicationService.generate(orgId, request));
    }

    /**
     * 查询详情。
     *
     * @param orgId 机构标识
     * @param id    主键
     * @return 详情
     */
    @GetMapping("/{id}")
    public CodeDataResponse<PeriodOpeningDetail> detail(@RequestParam(required = false) String orgId,
                                                        @PathVariable Long id) {
        return CodeDataResponse.ok(periodOpeningBalanceApplicationService.detail(orgId, id));
    }

    /**
     * 更新期初库存。
     *
     * @param orgId   机构标识
     * @param id      主键
     * @param request 保存请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@RequestParam(required = false) String orgId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PeriodOpeningSaveRequest request) {
        periodOpeningBalanceApplicationService.update(orgId, id, request);
        return CodeDataResponse.ok();
    }

    /**
     * 删除期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> delete(@RequestParam(required = false) String orgId,
                                         @PathVariable Long id) {
        periodOpeningBalanceApplicationService.delete(orgId, id);
        return CodeDataResponse.ok();
    }

    /**
     * 提交期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     * @return 空响应
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> submit(@RequestParam(required = false) String orgId,
                                         @PathVariable Long id) {
        periodOpeningBalanceApplicationService.submit(orgId, id);
        return CodeDataResponse.ok();
    }

    /**
     * 审批通过期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     * @return 空响应
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> approve(@RequestParam(required = false) String orgId,
                                          @PathVariable Long id) {
        periodOpeningBalanceApplicationService.approve(orgId, id);
        return CodeDataResponse.ok();
    }

    /**
     * 驳回期初库存。
     *
     * @param orgId   机构标识
     * @param id      主键
     * @param request 驳回请求
     * @return 空响应
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> reject(@RequestParam(required = false) String orgId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PeriodOpeningRejectRequest request) {
        periodOpeningBalanceApplicationService.reject(orgId, id, request);
        return CodeDataResponse.ok();
    }
}
