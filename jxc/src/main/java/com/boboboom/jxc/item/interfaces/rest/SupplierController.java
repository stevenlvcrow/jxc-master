package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.SupplierApplicationService;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.PageData;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.SupplierDetailResponse;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.SupplierListRow;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCreateRequest;
import jakarta.validation.Valid;
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

@Validated
@RestController
@RequestMapping("/api/items/suppliers")
/**
 * 供应商管理接口，负责供应商列表、详情、新建和修改。
 */
public class SupplierController {

    private final SupplierApplicationService supplierApplicationService;

    /**
     * 构造供应商管理接口。
     *
     * @param supplierApplicationService 供应商服务
     */
    public SupplierController(SupplierApplicationService supplierApplicationService) {
        this.supplierApplicationService = supplierApplicationService;
    }

    /**
     * 分页查询供应商列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param supplierInfo 供应商信息
     * @param status 状态
     * @param bindStatus 绑定状态
     * @param source 来源
     * @param supplyRelation 供货关系
     * @param treeNode 树节点
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping
    public CodeDataResponse<PageData<SupplierListRow>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String supplierInfo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bindStatus,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String supplyRelation,
            @RequestParam(required = false) String treeNode,
            @RequestParam(required = false) String orgId
    ) {
        return CodeDataResponse.ok(supplierApplicationService.list(pageNo, pageSize, supplierInfo, status, bindStatus, source, supplyRelation, treeNode, orgId));
    }

    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建供应商。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        return CodeDataResponse.ok(supplierApplicationService.create(orgId, request));
    }

    /**
     * 查询供应商详情。
     *
     * @param id 供应商主键
     * @param orgId 机构标识
     * @return 详情结果
     */
    @GetMapping("/{id}")
    public CodeDataResponse<SupplierDetailResponse> detail(@PathVariable Long id,
                                                           @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(supplierApplicationService.detail(id, orgId));
    }

    /**
     * 更新供应商。
     *
     * @param id 供应商主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> update(@PathVariable Long id,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        return CodeDataResponse.ok(supplierApplicationService.update(id, orgId, request));
    }
}
