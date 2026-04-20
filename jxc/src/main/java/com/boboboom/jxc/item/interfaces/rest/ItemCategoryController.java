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
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/items/categories")
/**
 * 商品分类接口，负责分类分页、树形查询、新建、批量和状态维护。
 */
public class ItemCategoryController {

    private final ItemCategoryApplicationService itemCategoryApplicationService;

    /**
     * 构造商品分类接口。
     *
     * @param itemCategoryApplicationService 商品分类服务
     */
    public ItemCategoryController(ItemCategoryApplicationService itemCategoryApplicationService) {
        this.itemCategoryApplicationService = itemCategoryApplicationService;
    }

    /**
     * 分页查询商品分类。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param categoryInfo 分类信息
     * @param status 状态
     * @param treeNode 树节点
     * @param sortBy 排序字段
     * @param orgId 机构标识
     * @return 分页结果
     */
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
        return CodeDataResponse.ok(itemCategoryApplicationService.list(pageNo, pageSize, categoryInfo, status, treeNode, sortBy, orgId));
    }

    @GetMapping("/tree")
    /**
     * 查询商品分类树。
     *
     * @param orgId 机构标识
     * @return 分类树
     */
    public CodeDataResponse<List<TreeNode>> tree(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemCategoryApplicationService.tree(orgId));
    }

    /**
     * 新建商品分类。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCategoryCreateRequest request) {
        return CodeDataResponse.ok(itemCategoryApplicationService.create(orgId, request));
    }

    /**
     * 批量新建商品分类。
     *
     * @param orgId 机构标识
     * @param request 批量新增请求
     * @return 批量新建结果
     */
    @PostMapping("/batch")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<BatchCreateResult> batchCreate(@RequestParam(required = false) String orgId,
                                                           @Valid @RequestBody ItemCategoryBatchCreateRequest request) {
        return CodeDataResponse.ok(itemCategoryApplicationService.batchCreate(orgId, request));
    }

    /**
     * 更新商品分类。
     *
     * @param id 分类主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemCategoryUpdateRequest request) {
        itemCategoryApplicationService.update(id, orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 更新商品分类状态。
     *
     * @param id 分类主键
     * @param orgId 机构标识
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateStatus(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody ItemCategoryStatusUpdateRequest request) {
        itemCategoryApplicationService.updateStatus(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除商品分类。
     *
     * @param id 分类主键
     * @param orgId 机构标识
     * @return 空响应
     */
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        itemCategoryApplicationService.delete(id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除商品分类。
     *
     * @param orgId 机构标识
     * @param request 批量删除请求
     * @return 空响应
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDelete(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCategoryBatchDeleteRequest request) {
        itemCategoryApplicationService.batchDelete(orgId, request);
        return CodeDataResponse.ok();
    }
}
