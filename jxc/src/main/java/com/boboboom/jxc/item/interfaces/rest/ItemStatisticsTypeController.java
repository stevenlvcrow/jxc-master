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
import org.springframework.security.access.prepost.PreAuthorize;
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
/**
 * 商品统计类别接口，负责统计类别的分页查询、详情、新建和批量导出。
 */
public class ItemStatisticsTypeController {

    private final ItemStatisticsTypeApplicationService itemStatisticsTypeApplicationService;

    /**
     * 构造商品统计类别接口。
     *
     * @param itemStatisticsTypeApplicationService 商品统计类别服务
     */
    public ItemStatisticsTypeController(ItemStatisticsTypeApplicationService itemStatisticsTypeApplicationService) {
        this.itemStatisticsTypeApplicationService = itemStatisticsTypeApplicationService;
    }

    /**
     * 分页查询商品统计类别。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping
    public CodeDataResponse<PageResult<StatisticsTypeListItem>> list(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemStatisticsTypeApplicationService.list(pageNo, pageSize, keyword, orgId));
    }

    @GetMapping("/{id}")
    /**
     * 查询商品统计类别详情。
     *
     * @param id 主键
     * @param orgId 机构标识
     * @return 详情结果
     */
    public CodeDataResponse<StatisticsTypeDetailItem> detail(@PathVariable Long id,
                                                             @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(itemStatisticsTypeApplicationService.detail(id, orgId));
    }

    /**
     * 新建商品统计类别。
     *
     * @param orgId 机构标识
     * @param request 新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<CreateResult> create(@RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody StatisticsTypeCreateRequest request) {
        return CodeDataResponse.ok(itemStatisticsTypeApplicationService.create(orgId, request));
    }

    /**
     * 批量导出商品统计类别。
     *
     * @param orgId 机构标识
     * @param request 批量导出请求
     * @return 导出结果
     */
    @PostMapping("/batch-export")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<BatchExportResult> batchExport(@RequestParam(required = false) String orgId,
                                                           @RequestBody(required = false) StatisticsTypeBatchExportRequest request) {
        return CodeDataResponse.ok(itemStatisticsTypeApplicationService.batchExport(orgId, request));
    }
}
