package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.SupplierApplicationService;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.PageData;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.SupplierDetailResponse;
import com.boboboom.jxc.item.application.service.SupplierApplicationService.SupplierListRow;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCreateRequest;
import jakarta.validation.Valid;
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
public class SupplierController {

    private final SupplierApplicationService supplierApplicationService;

    public SupplierController(SupplierApplicationService supplierApplicationService) {
        this.supplierApplicationService = supplierApplicationService;
    }

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
        return supplierApplicationService.list(pageNo, pageSize, supplierInfo, status, bindStatus, source, supplyRelation, treeNode, orgId);
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        return supplierApplicationService.create(orgId, request);
    }

    @GetMapping("/{id}")
    public CodeDataResponse<SupplierDetailResponse> detail(@PathVariable Long id,
                                                           @RequestParam(required = false) String orgId) {
        return supplierApplicationService.detail(id, orgId);
    }

    @PutMapping("/{id}")
    public CodeDataResponse<IdPayload> update(@PathVariable Long id,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        return supplierApplicationService.update(id, orgId, request);
    }
}
