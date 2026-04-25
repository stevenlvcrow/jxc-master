package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.DictionaryApplicationService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 字典管理与业务字典查询接口。
 */
@Validated
@RestController
public class DictionaryController {

    private final DictionaryApplicationService dictionaryApplicationService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造字典接口。
     *
     * @param dictionaryApplicationService 字典应用服务
     * @param identityAdminSupport 管理员辅助服务
     */
    public DictionaryController(DictionaryApplicationService dictionaryApplicationService,
                                IdentityAdminSupport identityAdminSupport) {
        this.dictionaryApplicationService = dictionaryApplicationService;
        this.identityAdminSupport = identityAdminSupport;
    }

    /**
     * 查询平台字典类型。
     *
     * @param keyword 关键字
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 字典类型分页
     */
    @GetMapping("/api/identity/admin/dictionaries")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<PageData<DictionaryApplicationService.DictionaryTypeView>> listTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        identityAdminSupport.requirePlatformAdmin();
        List<DictionaryApplicationService.DictionaryTypeView> rows =
                dictionaryApplicationService.listTypes(keyword, status);
        return CodeDataResponse.ok(paginate(rows, pageNum, pageSize));
    }

    /**
     * 创建字典类型。
     *
     * @param request 字典类型请求
     * @return 主键响应
     */
    @PostMapping("/api/identity/admin/dictionaries")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createType(
            @Valid @RequestBody DictionaryApplicationService.DictionaryTypeRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        return CodeDataResponse.ok(new IdPayload(dictionaryApplicationService.createType(request)));
    }

    /**
     * 更新字典类型。
     *
     * @param id 字典类型主键
     * @param request 字典类型请求
     * @return 空响应
     */
    @PutMapping("/api/identity/admin/dictionaries/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateType(
            @PathVariable Long id,
            @Valid @RequestBody DictionaryApplicationService.DictionaryTypeRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.updateType(id, request);
        return CodeDataResponse.ok();
    }

    /**
     * 更新字典类型状态。
     *
     * @param id 字典类型主键
     * @param request 状态请求
     * @return 空响应
     */
    @PutMapping("/api/identity/admin/dictionaries/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateTypeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusPayload request) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.updateTypeStatus(id, request.status());
        return CodeDataResponse.ok();
    }

    /**
     * 删除字典类型。
     *
     * @param id 字典类型主键
     * @return 空响应
     */
    @DeleteMapping("/api/identity/admin/dictionaries/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteType(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.deleteType(id);
        return CodeDataResponse.ok();
    }

    /**
     * 查询字典项。
     *
     * @param id 字典类型主键
     * @return 字典项列表
     */
    @GetMapping("/api/identity/admin/dictionaries/{id}/items")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<List<DictionaryApplicationService.DictionaryItemView>> listItems(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        return CodeDataResponse.ok(dictionaryApplicationService.listItems(id));
    }

    /**
     * 创建字典项。
     *
     * @param id 字典类型主键
     * @param request 字典项请求
     * @return 主键响应
     */
    @PostMapping("/api/identity/admin/dictionaries/{id}/items")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createItem(
            @PathVariable Long id,
            @Valid @RequestBody DictionaryApplicationService.DictionaryItemRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        return CodeDataResponse.ok(new IdPayload(dictionaryApplicationService.createItem(id, request)));
    }

    /**
     * 更新字典项。
     *
     * @param itemId 字典项主键
     * @param request 字典项请求
     * @return 空响应
     */
    @PutMapping("/api/identity/admin/dictionary-items/{itemId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody DictionaryApplicationService.DictionaryItemRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.updateItem(itemId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 更新字典项状态。
     *
     * @param itemId 字典项主键
     * @param request 状态请求
     * @return 空响应
     */
    @PutMapping("/api/identity/admin/dictionary-items/{itemId}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateItemStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody StatusPayload request) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.updateItemStatus(itemId, request.status());
        return CodeDataResponse.ok();
    }

    /**
     * 删除字典项。
     *
     * @param itemId 字典项主键
     * @return 空响应
     */
    @DeleteMapping("/api/identity/admin/dictionary-items/{itemId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteItem(@PathVariable Long itemId) {
        identityAdminSupport.requirePlatformAdmin();
        dictionaryApplicationService.deleteItem(itemId);
        return CodeDataResponse.ok();
    }

    /**
     * 批量查询启用字典项。
     *
     * @param codes 逗号分隔的字典编码
     * @return 字典项分组
     */
    @GetMapping("/api/dictionaries/items")
    public CodeDataResponse<Map<String, List<DictionaryLookupService.DictionaryItemOption>>> listEnabledItems(
            @RequestParam String codes) {
        List<String> dictCodes = Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return CodeDataResponse.ok(dictionaryApplicationService.listEnabledItemsByCodes(dictCodes));
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }

    /**
     * 状态请求体。
     *
     * @param status 状态
     */
    public record StatusPayload(String status) {
    }
}
