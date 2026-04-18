package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.ItemApplicationService;
import com.boboboom.jxc.item.application.service.ItemApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.ItemApplicationService.ItemListRow;
import com.boboboom.jxc.item.application.service.ItemApplicationService.PageData;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchDeleteRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchStatusUpdateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;

@Validated
@RestController
@RequestMapping("/api/items")
/**
 * 商品接口，负责商品创建、草稿保存、详情、分页、批量状态和批量删除。
 */
public class ItemController {

    private final ItemApplicationService itemApplicationService;

    /**
     * 构造商品接口。
     *
     * @param itemApplicationService 商品服务
     */
    public ItemController(ItemApplicationService itemApplicationService) {
        this.itemApplicationService = itemApplicationService;
    }

    /**
     * 新建商品。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCreateRequest request) {
        return CodeDataResponse.ok(itemApplicationService.create(orgId, request));
    }

    /**
     * 保存商品草稿。
     *
     * @param orgId 机构标识
     * @param request 草稿请求
     * @return 草稿结果
     */
    @PostMapping("/drafts")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> saveDraft(@RequestParam(required = false) String orgId,
                                                 @RequestBody ItemCreateRequest request) {
        return CodeDataResponse.ok(itemApplicationService.saveDraft(orgId, request));
    }

    /**
     * 查询商品详情。
     *
     * @param id 商品主键
     * @param orgId 机构标识
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public CodeDataResponse<ItemCreateRequest> detail(@PathVariable String id,
                                                      @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemApplicationService.detail(id, orgId));
    }

    /**
     * 更新商品。
     *
     * @param id 商品主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable String id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemCreateRequest request) {
        itemApplicationService.update(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @GetMapping
    /**
     * 分页查询商品。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param category 分类
     * @param status 状态
     * @param itemType 商品类型
     * @param statType 统计类型
     * @param storageMode 存储方式
     * @param tag 标签
     * @param orgId 机构标识
     * @return 分页结果
     */
    public CodeDataResponse<PageData<ItemListRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String itemType,
                                                        @RequestParam(required = false) String statType,
                                                        @RequestParam(required = false) String storageMode,
                                                        @RequestParam(required = false) String tag,
                                                        @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemApplicationService.list(pageNo, pageSize, keyword, category, status, itemType, statType, storageMode, tag, orgId));
    }

    @PostMapping("/batch-status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 批量更新商品状态。
     *
     * @param orgId 机构标识
     * @param request 批量状态请求
     * @return 空响应
     */
    public CodeDataResponse<Void> batchUpdateStatus(@RequestParam(required = false) String orgId,
                                                    @Valid @RequestBody ItemBatchStatusUpdateRequest request) {
        itemApplicationService.batchUpdateStatus(orgId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 批量删除商品。
     *
     * @param orgId 机构标识
     * @param request 批量删除请求
     * @return 空响应
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> batchDelete(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemBatchDeleteRequest request) {
        itemApplicationService.batchDelete(orgId, request);
        return CodeDataResponse.ok();
    }
}
