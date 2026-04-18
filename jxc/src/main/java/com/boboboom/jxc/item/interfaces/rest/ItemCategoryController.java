package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService.BatchCreateResult;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService.ItemCategoryRow;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService.PageData;
import com.boboboom.jxc.item.application.service.ItemCategoryApplicationService.TreeNode;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryBatchCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryBatchDeleteRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryStatusUpdateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryUpdateRequest;
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
@RequestMapping("/api/items/categories")
public class ItemCategoryController {

    private final ItemCategoryApplicationService itemCategoryApplicationService;

    public ItemCategoryController(ItemCategoryApplicationService itemCategoryApplicationService) {
        this.itemCategoryApplicationService = itemCategoryApplicationService;
    }

    @GetMapping
    public CodeDataResponse<PageData<ItemCategoryRow>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String categoryInfo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String treeNode,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String orgId
    ) {
        return itemCategoryApplicationService.list(pageNo, pageSize, categoryInfo, status, treeNode, sortBy, orgId);
    }

    @GetMapping("/tree")
    public CodeDataResponse<List<TreeNode>> tree(@RequestParam(required = false) String orgId) {
        return itemCategoryApplicationService.tree(orgId);
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCategoryCreateRequest request) {
        return itemCategoryApplicationService.create(orgId, request);
    }

    @PostMapping("/batch")
    public CodeDataResponse<BatchCreateResult> batchCreate(@RequestParam(required = false) String orgId,
                                                           @Valid @RequestBody ItemCategoryBatchCreateRequest request) {
        return itemCategoryApplicationService.batchCreate(orgId, request);
    }

    @PutMapping("/{id}")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemCategoryUpdateRequest request) {
        return itemCategoryApplicationService.update(id, orgId, request);
    }

    @PutMapping("/{id}/status")
    public CodeDataResponse<Void> updateStatus(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody ItemCategoryStatusUpdateRequest request) {
        return itemCategoryApplicationService.updateStatus(id, orgId, request);
    }

    @DeleteMapping("/{id}")
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        return itemCategoryApplicationService.delete(id, orgId);
    }

    @PostMapping("/batch-delete")
    public CodeDataResponse<Void> batchDelete(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCategoryBatchDeleteRequest request) {
        return itemCategoryApplicationService.batchDelete(orgId, request);
    }
}
