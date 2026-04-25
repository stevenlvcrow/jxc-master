package com.boboboom.jxc.identity.interfaces.rest;

import java.time.LocalDateTime;
import java.util.List;

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

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.UnitAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UnitUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;

import jakarta.validation.Valid;

/**
 * 单位管理接口，负责单位的查询、创建、修改、状态变更和删除。
 */
@Validated
@RestController
@RequestMapping("/api/identity/admin/units")
public class UnitController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final UnitAdministrationService unitAdministrationService;
    private final OrgScopeService orgScopeService;

    /**
     * 构造单位管理接口。
     *
     * @param unitAdministrationServiceValue 单位管理服务
     * @param orgScopeServiceValue 组织范围解析服务
     */
    public UnitController(UnitAdministrationService unitAdministrationServiceValue,
                          OrgScopeService orgScopeServiceValue) {
        this.unitAdministrationService = unitAdministrationServiceValue;
        this.orgScopeService = orgScopeServiceValue;
    }

    /**
     * 查询单位列表。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param status 状态
     * @param unitType 单位类型
     * @param orgId 机构标识
     * @return 单位列表响应
     */
    @GetMapping
    public CodeDataResponse<PageData<UnitView>> listUnits(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String unitType,
                                                          @RequestParam(required = false) String orgId) {
        UnitScope scope = resolveUnitScope(orgId);
        List<UnitView> data = unitAdministrationService.listUnits(
                        scope.scopeType(),
                        scope.scopeId(),
                        keyword,
                        status,
                        unitType
                )
                .stream()
                .map(this::toView)
                .toList();
        return CodeDataResponse.ok(paginate(data, pageNum, pageSize));
    }

    /**
     * 新建单位。
     *
     * @param orgId 机构标识
     * @param request 单位新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createUnit(@RequestParam(required = false) String orgId,
                                                  @Valid @RequestBody UnitUpsertRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        String unitCode = trimNullable(request.getCode());
        String unitName = trim(request.getName());
        UnitDO entity = unitAdministrationService.createUnit(
                scope.scopeType(),
                scope.scopeId(),
                unitCode,
                unitName,
                request.getType(),
                request.getStatus(),
                trimNullable(request.getRemark())
        );
        return CodeDataResponse.ok(new IdPayload(entity.getId()));
    }

    /**
     * 更新单位信息。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @param request 单位更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateUnit(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody UnitUpsertRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        String unitCode = trim(request.getCode());
        String unitName = trim(request.getName());
        unitAdministrationService.updateUnit(
                id,
                scope.scopeType(),
                scope.scopeId(),
                unitCode,
                unitName,
                request.getType(),
                request.getStatus(),
                trimNullable(request.getRemark())
        );
        return CodeDataResponse.ok();
    }

    /**
     * 更新单位状态。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateUnitStatus(@PathVariable Long id,
                                                   @RequestParam(required = false) String orgId,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        unitAdministrationService.updateUnitStatus(id, scope.scopeType(), scope.scopeId(), request.getStatus());
        return CodeDataResponse.ok();
    }

    /**
     * 删除单位。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteUnit(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId) {
        UnitScope scope = resolveUnitScope(orgId);
        unitAdministrationService.deleteUnit(id, scope.scopeType(), scope.scopeId());
        return CodeDataResponse.ok();
    }

    /**
     * 将单位实体转换为接口返回视图。
     *
     * @param entity 单位实体
     * @return 单位视图
     */
    private UnitView toView(UnitDO entity) {
        return new UnitView(
                entity.getId(),
                entity.getUnitCode(),
                entity.getUnitName(),
                entity.getUnitType(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedAt()
        );
    }

    /**
     * 去除空白并校验必填参数。
     *
     * @param value 原始字符串
     * @return 去空格后的字符串
     */
    private String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException("必填参数不能为空");
        }
        return trimmed;
    }

    /**
     * 去除空白并允许返回空值。
     *
     * @param value 原始字符串
     * @return 去空格后的字符串，或空值
     */
    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 解析当前请求所在的单位范围。
     *
     * @param orgId 机构标识
     * @return 单位范围
     */
    private UnitScope resolveUnitScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolvePlatformOrStoreScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new UnitScope(scope.scopeType(), scope.scopeId());
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }

    /**
     * 单位主键返回结果。
     *
     * @param id 单位主键
     */
    public record IdPayload(Long id) {
    }

    /**
     * 单位列表返回视图。
     *
     * @param id 单位主键
     * @param code 单位编码
     * @param name 单位名称
     * @param type 单位类型
     * @param status 单位状态
     * @param remark 备注
     * @param createdAt 创建时间
     */
    public record UnitView(Long id,
                           String code,
                           String name,
                           String type,
                           String status,
                           String remark,
                           LocalDateTime createdAt) {
    }

    /**
     * 通用分页响应。
     *
     * @param <T> 数据类型
     * @param list 当前页数据
     * @param total 总条数
     * @param pageNum 当前页码
     * @param pageSize 当前页大小
     */
    public record PageData<T>(List<T> list, long total, int pageNum, int pageSize) {
    }

    /**
     * 当前请求可操作的组织范围。
     *
     * @param scopeType 范围类型
     * @param scopeId 范围主键
     */
    private record UnitScope(String scopeType, Long scopeId) {
    }
}
