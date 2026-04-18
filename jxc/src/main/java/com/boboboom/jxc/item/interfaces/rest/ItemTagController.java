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
public class ItemTagController {

    private final ItemTagApplicationService itemTagApplicationService;

    public ItemTagController(ItemTagApplicationService itemTagApplicationService) {
        this.itemTagApplicationService = itemTagApplicationService;
    }

    @GetMapping
    public CodeDataResponse<PageData<ItemTagRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String tagCode,
                                                       @RequestParam(required = false) String tagName,
                                                       @RequestParam(required = false) String itemName,
                                                       @RequestParam(required = false) String orgId) {
        return itemTagApplicationService.list(pageNo, pageSize, tagCode, tagName, itemName, orgId);
    }

    @PostMapping
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemTagCreateRequest request) {
        return itemTagApplicationService.create(orgId, request);
    }

    @PutMapping("/{id}")
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemTagUpdateRequest request) {
        return itemTagApplicationService.update(id, orgId, request);
    }

    @DeleteMapping("/{id}")
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        return itemTagApplicationService.delete(id, orgId);
    }

    @PostMapping("/batch-import")
    public CodeDataResponse<BatchImportResult> batchImport(@RequestParam(required = false) String orgId,
                                                           @Valid @RequestBody ItemTagBatchImportRequest request) {
        return itemTagApplicationService.batchImport(orgId, request);
    }
}
