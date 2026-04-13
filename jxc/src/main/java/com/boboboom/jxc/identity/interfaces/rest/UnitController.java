package com.boboboom.jxc.identity.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UnitMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UnitUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String TYPE_STANDARD = "STANDARD";
    private static final String TYPE_AUXILIARY = "AUXILIARY";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final UnitMapper unitMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public UnitController(UnitMapper unitMapper,
                          RoleMapper roleMapper,
                          UserRoleRelMapper userRoleRelMapper,
                          StoreMapper storeMapper) {
        this.unitMapper = unitMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public CodeDataResponse<List<UnitView>> listUnits(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String unitType,
                                                      @RequestParam(required = false) String orgId) {
        UnitScope scope = resolveUnitScope(orgId);
        String normalizedKeyword = trimNullable(keyword);
        String normalizedStatus = normalizeStatusNullable(status);
        String normalizedType = normalizeUnitTypeNullable(unitType);

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scope.scopeType())
                .eq(UnitDO::getScopeId, scope.scopeId())
                .orderByDesc(UnitDO::getCreatedAt)
                .orderByDesc(UnitDO::getId);
        if (normalizedKeyword != null) {
            queryWrapper.and(wrapper -> wrapper.like(UnitDO::getUnitCode, normalizedKeyword)
                    .or()
                    .like(UnitDO::getUnitName, normalizedKeyword));
        }
        if (normalizedStatus != null) {
            queryWrapper.eq(UnitDO::getStatus, normalizedStatus);
        }
        if (normalizedType != null) {
            queryWrapper.eq(UnitDO::getUnitType, normalizedType);
        }

        List<UnitView> data = unitMapper.selectList(queryWrapper).stream()
                .map(this::toView)
                .toList();
        return CodeDataResponse.ok(data);
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> createUnit(@RequestParam(required = false) String orgId,
                                                  @Valid @RequestBody UnitUpsertRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        String unitCode = trim(request.getCode());
        String unitName = trim(request.getName());
        ensureCodeUnique(unitCode, null, scope);
        ensureNameUnique(unitName, null, scope);

        UnitDO entity = new UnitDO();
        entity.setScopeType(scope.scopeType());
        entity.setScopeId(scope.scopeId());
        entity.setUnitCode(unitCode);
        entity.setUnitName(unitName);
        entity.setUnitType(normalizeUnitType(request.getType()));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setRemark(trimNullable(request.getRemark()));
        unitMapper.insert(entity);
        return CodeDataResponse.ok(new IdPayload(entity.getId()));
    }

    @PutMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> updateUnit(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody UnitUpsertRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        UnitDO entity = requireUnit(id, scope);
        String unitCode = trim(request.getCode());
        String unitName = trim(request.getName());
        ensureCodeUnique(unitCode, id, scope);
        ensureNameUnique(unitName, id, scope);

        entity.setUnitCode(unitCode);
        entity.setUnitName(unitName);
        entity.setUnitType(normalizeUnitType(request.getType()));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setRemark(trimNullable(request.getRemark()));
        unitMapper.updateById(entity);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/status")
    @Transactional
    public CodeDataResponse<Void> updateUnitStatus(@PathVariable Long id,
                                                   @RequestParam(required = false) String orgId,
                                                   @Valid @RequestBody StatusUpdateRequest request) {
        UnitScope scope = resolveUnitScope(orgId);
        UnitDO entity = requireUnit(id, scope);
        entity.setStatus(normalizeStatus(request.getStatus()));
        unitMapper.updateById(entity);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> deleteUnit(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId) {
        UnitScope scope = resolveUnitScope(orgId);
        requireUnit(id, scope);
        unitMapper.delete(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getId, id)
                .eq(UnitDO::getScopeType, scope.scopeType())
                .eq(UnitDO::getScopeId, scope.scopeId()));
        return CodeDataResponse.ok();
    }

    private UnitDO requireUnit(Long id, UnitScope scope) {
        UnitDO entity = unitMapper.selectOne(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getId, id)
                .eq(UnitDO::getScopeType, scope.scopeType())
                .eq(UnitDO::getScopeId, scope.scopeId())
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException("单位不存在");
        }
        return entity;
    }

    private void ensureCodeUnique(String unitCode, Long excludedId, UnitScope scope) {
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scope.scopeType())
                .eq(UnitDO::getScopeId, scope.scopeId())
                .eq(UnitDO::getUnitCode, unitCode)
                .last("limit 1");
        if (excludedId != null) {
            queryWrapper.ne(UnitDO::getId, excludedId);
        }
        UnitDO exists = unitMapper.selectOne(queryWrapper);
        if (exists != null) {
            throw new BusinessException("单位编码已存在");
        }
    }

    private void ensureNameUnique(String unitName, Long excludedId, UnitScope scope) {
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scope.scopeType())
                .eq(UnitDO::getScopeId, scope.scopeId())
                .eq(UnitDO::getUnitName, unitName)
                .last("limit 1");
        if (excludedId != null) {
            queryWrapper.ne(UnitDO::getId, excludedId);
        }
        UnitDO exists = unitMapper.selectOne(queryWrapper);
        if (exists != null) {
            throw new BusinessException("单位名称已存在");
        }
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
            return STATUS_ENABLED;
        }
        if (STATUS_ENABLED.equalsIgnoreCase(status)) {
            return STATUS_ENABLED;
        }
        if (STATUS_DISABLED.equalsIgnoreCase(status)) {
            return STATUS_DISABLED;
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
            return TYPE_STANDARD;
        }
        if (TYPE_STANDARD.equalsIgnoreCase(unitType)) {
            return TYPE_STANDARD;
        }
        if (TYPE_AUXILIARY.equalsIgnoreCase(unitType)) {
            return TYPE_AUXILIARY;
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
        Long userId = currentUserId();
        Scope requested = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return new UnitScope(requested.scopeType(), requested.scopeId());
        }

        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new UnitScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new UnitScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new UnitScope(SCOPE_STORE, requested.scopeId());
            }
            throw new BusinessException("当前账号无该门店权限");
        }

        throw new BusinessException("机构参数非法");
    }

    private Scope parseScope(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return new Scope(SCOPE_PLATFORM, 0L);
        }
        if (orgId.startsWith("group-")) {
            return new Scope(SCOPE_GROUP, parseNumericId(orgId.substring("group-".length())));
        }
        if (orgId.startsWith("store-")) {
            return new Scope(SCOPE_STORE, parseNumericId(orgId.substring("store-".length())));
        }
        throw new BusinessException("机构参数非法");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private Long currentUserId() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        return AuthContextHolder.get().getUserId();
    }

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, SCOPE_PLATFORM)
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        return rel != null;
    }

    private boolean hasScope(Long userId, String scopeType, Long scopeId) {
        if (scopeId == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .eq(UserRoleRelDO::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        return rel != null;
    }

    private Long findGroupIdByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreDO store = storeMapper.selectById(storeId);
        if (store == null) {
            return null;
        }
        return store.getGroupId();
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

    private record Scope(String scopeType, Long scopeId) {
    }

    private record UnitScope(String scopeType, Long scopeId) {
    }
}
