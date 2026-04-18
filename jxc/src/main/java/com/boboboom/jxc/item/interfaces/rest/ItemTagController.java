package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.ItemTagApplicationService;
import com.boboboom.jxc.item.application.service.ItemTagApplicationService.BatchImportResult;
import com.boboboom.jxc.item.application.service.ItemTagApplicationService.IdPayload;
import com.boboboom.jxc.item.application.service.ItemTagApplicationService.ItemTagRow;
import com.boboboom.jxc.item.application.service.ItemTagApplicationService.PageData;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagBatchImportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagUpdateRequest;
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

@Validated
@RestController
@RequestMapping("/api/items/tags")
/**
 * 商品标签接口，负责标签分页查询、创建、修改、删除和批量导入。
 */
public class ItemTagController {

    private final ItemTagApplicationService itemTagApplicationService;

    /**
     * 构造商品标签接口。
     *
     * @param itemTagApplicationService 商品标签服务
     */
    public ItemTagController(ItemTagApplicationService itemTagApplicationService) {
        this.itemTagApplicationService = itemTagApplicationService;
    }

    /**
     * 分页查询商品标签。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param tagCode 标签编码
     * @param tagName 标签名称
     * @param itemName 商品名称
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping
    public CodeDataResponse<PageData<ItemTagRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String tagCode,
                                                       @RequestParam(required = false) String tagName,
                                                       @RequestParam(required = false) String itemName,
                                                       @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemTagApplicationService.list(pageNo, pageSize, tagCode, tagName, itemName, orgId));
    }

    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建商品标签。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemTagCreateRequest request) {
        return CodeDataResponse.ok(itemTagApplicationService.create(orgId, request));
    }

    /**
     * 更新商品标签。
     *
     * @param id 标签主键
     * @param orgId 机构标识
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemTagUpdateRequest request) {
        itemTagApplicationService.update(id, orgId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除商品标签。
     *
     * @param id 标签主键
     * @param orgId 机构标识
     * @return 空响应
     */
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        itemTagApplicationService.delete(id, orgId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量导入商品标签。
     *
     * @param orgId 机构标识
     * @param request 批量导入请求
     * @return 导入结果
     */
    @PostMapping("/batch-import")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<BatchImportResult> batchImport(@RequestParam(required = false) String orgId,
                                                           @Valid @RequestBody ItemTagBatchImportRequest request) {
        return CodeDataResponse.ok(itemTagApplicationService.batchImport(orgId, request));
    }
}
