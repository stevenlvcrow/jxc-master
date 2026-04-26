package com.boboboom.jxc.item.interfaces.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService.TreeNode;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCategoryCreateRequest;

import jakarta.validation.Valid;

/**
 * 供应商分类接口，负责树形分类查询与新增。
 */
@Validated
@RestController
@RequestMapping("/api/items/supplier-categories")
public class SupplierCategoryController {

    private final SupplierCategoryApplicationService supplierCategoryApplicationService;

    /**
     * 构造供应商分类接口。
     *
     * @param supplierCategoryApplicationServiceValue 供应商分类服务
     */
    public SupplierCategoryController(SupplierCategoryApplicationService supplierCategoryApplicationServiceValue) {
        this.supplierCategoryApplicationService = supplierCategoryApplicationServiceValue;
    }

    /**
     * 查询供应商分类树。
     *
     * @param orgId 机构标识
     * @return 分类树
     */
    @GetMapping("/tree")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<List<TreeNode>> tree(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(supplierCategoryApplicationService.tree(orgId));
    }

    /**
     * 新建供应商分类。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCategoryCreateRequest request) {
        return CodeDataResponse.ok(supplierCategoryApplicationService.create(orgId, request));
    }
}
