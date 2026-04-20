package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.UnitAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UnitUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
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

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin/units")
/**
 * 单位管理接口，负责单位的查询、创建、修改、状态变更和删除。
 */
public class UnitController {

    private final UnitAdministrationService unitAdministrationService;
    private final OrgScopeService orgScopeService;

    /**
     * 构造单位管理接口。
     *
     * @param unitAdministrationService 单位管理服务
     * @param orgScopeService 组织范围解析服务
     */
    public UnitController(UnitAdministrationService unitAdministrationService,
                          OrgScopeService orgScopeService) {
        this.unitAdministrationService = unitAdministrationService;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping
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

    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建单位。
     *
     * @param orgId 机构标识
     * @param request 单位新增请求
     * @return 新建结果
     */
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
                normalizeUnitType(request.getType()),
                normalizeStatus(request.getStatus()),
                trimNullable(request.getRemark())
        );
        return CodeDataResponse.ok(new IdPayload(entity.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新单位信息。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @param request 单位更新请求
     * @return 空响应
     */
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
                normalizeUnitType(request.getType()),
                normalizeStatus(request.getStatus()),
                trimNullable(request.getRemark())
        );
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新单位状态。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @param request 状态更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateUnitStatus(@PathVariable Long id,
                                                   @RequestParam(required = false) String orgId,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        unitAdministrationService.updateUnitStatus(id, scope.scopeType(), scope.scopeId(), normalizeStatus(request.getStatus()));
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除单位。
     *
     * @param id 单位主键
     * @param orgId 机构标识
     * @return 空响应
     */
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
     * 解析可空状态参数，ALL 代表不过滤。
     *
     * @param value 原始状态值
     * @return 规范化后的状态值
     */
    private String normalizeStatusNullable(String value) {
        String status = trimNullable(value);
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return normalizeStatus(status);
    }

    /**
     * 规范化状态参数。
     *
     * @param value 原始状态值
     * @return 规范化后的状态值
     */
    private String normalizeStatus(String value) {
        String status = trimNullable(value);
        if (status == null || status.isEmpty()) {
            return "ENABLED";
        }
        // 统一只允许系统已定义的状态值，避免脏数据写入。
        if ("ENABLED".equalsIgnoreCase(status)) {
            return "ENABLED";
        }
        if ("DISABLED".equalsIgnoreCase(status)) {
            return "DISABLED";
        }
        throw new BusinessException("状态参数非法");
    }

    /**
     * 解析可空单位类型参数，ALL 代表不过滤。
     *
     * @param value 原始单位类型
     * @return 规范化后的单位类型
     */
    private String normalizeUnitTypeNullable(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || "ALL".equalsIgnoreCase(unitType)) {
            return null;
        }
        return normalizeUnitType(unitType);
    }

    /**
     * 规范化单位类型参数。
     *
     * @param value 原始单位类型
     * @return 规范化后的单位类型
     */
    private String normalizeUnitType(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || unitType.isEmpty()) {
            return "STANDARD";
        }
        // 统一单位类型枚举值，确保接口与持久化一致。
        if ("STANDARD".equalsIgnoreCase(unitType)) {
            return "STANDARD";
        }
        if ("AUXILIARY".equalsIgnoreCase(unitType)) {
            return "AUXILIARY";
        }
        throw new BusinessException("单位类型参数非法");
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
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new UnitScope(scope.scopeType(), scope.scopeId());
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
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
