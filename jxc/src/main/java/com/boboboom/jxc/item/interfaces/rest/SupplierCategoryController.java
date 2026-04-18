package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.SupplierCategoryApplicationService.TreeNode;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCategoryCreateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/items/supplier-categories")
public class SupplierCategoryController {

    private final SupplierCategoryApplicationService supplierCategoryApplicationService;

    public SupplierCategoryController(SupplierCategoryApplicationService supplierCategoryApplicationService) {
        this.supplierCategoryApplicationService = supplierCategoryApplicationService;
    }

    @GetMapping("/tree")
    public CodeDataResponse<List<TreeNode>> tree(@RequestParam(required = false) String orgId) {
        return supplierCategoryApplicationService.tree(orgId);
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCategoryCreateRequest request) {
        return supplierCategoryApplicationService.create(orgId, request);
    }
}
