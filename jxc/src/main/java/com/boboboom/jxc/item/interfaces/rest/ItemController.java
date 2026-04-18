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

@Validated
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemApplicationService itemApplicationService;

    public ItemController(ItemApplicationService itemApplicationService) {
        this.itemApplicationService = itemApplicationService;
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCreateRequest request) {
        return itemApplicationService.create(orgId, request);
    }

    @PostMapping("/drafts")
    public CodeDataResponse<IdPayload> saveDraft(@RequestParam(required = false) String orgId,
                                                 @RequestBody ItemCreateRequest request) {
        return itemApplicationService.saveDraft(orgId, request);
    }

    @GetMapping("/{id}")
    public CodeDataResponse<ItemCreateRequest> detail(@PathVariable String id,
                                                      @RequestParam(required = false) String orgId) {
        return itemApplicationService.detail(id, orgId);
    }

    @PutMapping("/{id}")
    public CodeDataResponse<Void> update(@PathVariable String id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemCreateRequest request) {
        return itemApplicationService.update(id, orgId, request);
    }

    @GetMapping
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
        return itemApplicationService.list(pageNo, pageSize, keyword, category, status, itemType, statType, storageMode, tag, orgId);
    }

    @PostMapping("/batch-status")
    public CodeDataResponse<Void> batchUpdateStatus(@RequestParam(required = false) String orgId,
                                                    @Valid @RequestBody ItemBatchStatusUpdateRequest request) {
        return itemApplicationService.batchUpdateStatus(orgId, request);
    }

    @PostMapping("/batch-delete")
    public CodeDataResponse<Void> batchDelete(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemBatchDeleteRequest request) {
        return itemApplicationService.batchDelete(orgId, request);
    }
}
