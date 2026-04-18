package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService.BatchExportResult;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService.CreateResult;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService.PageResult;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService.StatisticsTypeDetailItem;
import com.boboboom.jxc.item.application.service.ItemStatisticsTypeApplicationService.StatisticsTypeListItem;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeBatchExportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeCreateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/items/statistics-types")
public class ItemStatisticsTypeController {

    private final ItemStatisticsTypeApplicationService itemStatisticsTypeApplicationService;

    public ItemStatisticsTypeController(ItemStatisticsTypeApplicationService itemStatisticsTypeApplicationService) {
        this.itemStatisticsTypeApplicationService = itemStatisticsTypeApplicationService;
    }

    @GetMapping
    public CodeDataResponse<PageResult<StatisticsTypeListItem>> list(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orgId) {
        return itemStatisticsTypeApplicationService.list(pageNo, pageSize, keyword, orgId);
    }

    @GetMapping("/{id}")
    public CodeDataResponse<StatisticsTypeDetailItem> detail(@PathVariable Long id,
                                                             @RequestParam(required = false) String orgId) {
        return itemStatisticsTypeApplicationService.detail(id, orgId);
    }

    @PostMapping
    public CodeDataResponse<CreateResult> create(@RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody StatisticsTypeCreateRequest request) {
        return itemStatisticsTypeApplicationService.create(orgId, request);
    }

    @PostMapping("/batch-export")
    public CodeDataResponse<BatchExportResult> batchExport(@RequestParam(required = false) String orgId,
                                                           @RequestBody(required = false) StatisticsTypeBatchExportRequest request) {
        return itemStatisticsTypeApplicationService.batchExport(orgId, request);
    }
}
