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
public class UnitController {

    private final UnitAdministrationService unitAdministrationService;
    private final OrgScopeService orgScopeService;

    public UnitController(UnitAdministrationService unitAdministrationService,
                          OrgScopeService orgScopeService) {
        this.unitAdministrationService = unitAdministrationService;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping
    public CodeDataResponse<List<UnitView>> listUnits(@RequestParam(required = false) String keyword,
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
        return CodeDataResponse.ok(data);
    }

    @PostMapping
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
    public CodeDataResponse<Void> updateUnitStatus(@PathVariable Long id,
                                                   @RequestParam(required = false) String orgId,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        unitAdministrationService.updateUnitStatus(id, scope.scopeType(), scope.scopeId(), normalizeStatus(request.getStatus()));
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    public CodeDataResponse<Void> deleteUnit(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId) {
        UnitScope scope = resolveUnitScope(orgId);
        unitAdministrationService.deleteUnit(id, scope.scopeType(), scope.scopeId());
        return CodeDataResponse.ok();
    }

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

    private String normalizeStatusNullable(String value) {
        String status = trimNullable(value);
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String value) {
        String status = trimNullable(value);
        if (status == null || status.isEmpty()) {
            return "ENABLED";
        }
        if ("ENABLED".equalsIgnoreCase(status)) {
            return "ENABLED";
        }
        if ("DISABLED".equalsIgnoreCase(status)) {
            return "DISABLED";
        }
        throw new BusinessException("状态参数非法");
    }

    private String normalizeUnitTypeNullable(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || "ALL".equalsIgnoreCase(unitType)) {
            return null;
        }
        return normalizeUnitType(unitType);
    }

    private String normalizeUnitType(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || unitType.isEmpty()) {
            return "STANDARD";
        }
        if ("STANDARD".equalsIgnoreCase(unitType)) {
            return "STANDARD";
        }
        if ("AUXILIARY".equalsIgnoreCase(unitType)) {
            return "AUXILIARY";
        }
        throw new BusinessException("单位类型参数非法");
    }

    private String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException("必填参数不能为空");
        }
        return trimmed;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UnitScope resolveUnitScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new UnitScope(scope.scopeType(), scope.scopeId());
    }

    public record IdPayload(Long id) {
    }

    public record UnitView(Long id,
                           String code,
                           String name,
                           String type,
                           String status,
                           String remark,
                           LocalDateTime createdAt) {
    }

    private record UnitScope(String scopeType, Long scopeId) {
    }
}
