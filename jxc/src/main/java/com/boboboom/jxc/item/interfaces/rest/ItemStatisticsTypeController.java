package com.boboboom.jxc.item.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemStatisticsTypeMapper;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeBatchExportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeCreateRequest;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/items/statistics-types")
public class ItemStatisticsTypeController {

    private static final String CREATE_TYPE_SYSTEM_BUILTIN = "SYSTEM_BUILTIN";
    private static final String CREATE_TYPE_CUSTOM = "CUSTOM";
    private static final String CODE_PREFIX = "TJLX";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final ItemStatisticsTypeMapper itemStatisticsTypeMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public ItemStatisticsTypeController(ItemStatisticsTypeMapper itemStatisticsTypeMapper,
                                        RoleMapper roleMapper,
                                        UserRoleRelMapper userRoleRelMapper,
                                        StoreMapper storeMapper) {
        this.itemStatisticsTypeMapper = itemStatisticsTypeMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public CodeDataResponse<PageResult<StatisticsTypeListItem>> list(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        long normalizedPageNo = Math.max(pageNo, 1L);
        long normalizedPageSize = Math.max(1L, Math.min(pageSize, 200L));
        String normalizedKeyword = trimNullable(keyword);

        LambdaQueryWrapper<ItemStatisticsTypeDO> query = scopeQuery(scope)
                .orderByDesc(ItemStatisticsTypeDO::getCode)
                .orderByDesc(ItemStatisticsTypeDO::getId);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like(ItemStatisticsTypeDO::getCode, normalizedKeyword)
                    .or()
                    .like(ItemStatisticsTypeDO::getName, normalizedKeyword)
                    .or()
                    .like(ItemStatisticsTypeDO::getStatisticsCategory, normalizedKeyword));
        }

        Page<ItemStatisticsTypeDO> page = itemStatisticsTypeMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                query
        );

        long baseIndex = (normalizedPageNo - 1) * normalizedPageSize;
        List<ItemStatisticsTypeDO> records = page.getRecords();
        List<StatisticsTypeListItem> list = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            list.add(toListItem(records.get(i), baseIndex + i + 1));
        }
        return CodeDataResponse.ok(new PageResult<>(
                list,
                page.getTotal(),
                normalizedPageNo,
                normalizedPageSize
        ));
    }

    @GetMapping("/{id}")
    public CodeDataResponse<StatisticsTypeDetailItem> detail(@PathVariable Long id,
                                                             @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        ItemStatisticsTypeDO row = requireById(id, scope);
        return CodeDataResponse.ok(new StatisticsTypeDetailItem(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getStatisticsCategory(),
                toCreateTypeLabel(row.getCreateType()),
                toModifiedTimeLabel(row),
                row.getCreatedAt(),
                row.getUpdatedAt()
        ));
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<CreateResult> create(@RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody StatisticsTypeCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String name = trim(request.getName());
        String statisticsCategory = trim(request.getStatisticsCategory());

        ItemStatisticsTypeDO duplicated = itemStatisticsTypeMapper.selectOne(
                scopeQuery(scope)
                        .eq(ItemStatisticsTypeDO::getName, name)
                        .eq(ItemStatisticsTypeDO::getStatisticsCategory, statisticsCategory)
                        .last("limit 1")
        );
        if (duplicated != null) {
            throw new BusinessException("统计类型名称已存在");
        }

        ItemStatisticsTypeDO row = new ItemStatisticsTypeDO();
        row.setScopeType(scope.scopeType());
        row.setScopeId(scope.scopeId());
        row.setCode(nextCode(scope));
        row.setName(name);
        row.setStatisticsCategory(statisticsCategory);
        row.setCreateType(CREATE_TYPE_CUSTOM);
        itemStatisticsTypeMapper.insert(row);

        return CodeDataResponse.ok(new CreateResult(row.getId(), row.getCode()));
    }

    @PostMapping("/batch-export")
    public CodeDataResponse<BatchExportResult> batchExport(@RequestParam(required = false) String orgId,
                                                           @RequestBody(required = false) StatisticsTypeBatchExportRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        List<Long> ids = request == null ? Collections.emptyList() : request.getIds();
        List<ItemStatisticsTypeDO> rows;
        if (ids == null || ids.isEmpty()) {
            rows = itemStatisticsTypeMapper.selectList(scopeQuery(scope)
                    .orderByDesc(ItemStatisticsTypeDO::getCode)
                    .orderByDesc(ItemStatisticsTypeDO::getId));
        } else {
            rows = itemStatisticsTypeMapper.selectList(scopeQuery(scope)
                            .in(ItemStatisticsTypeDO::getId, ids))
                    .stream()
                    .sorted((left, right) -> {
                        int codeCompare = nullSafeString(right.getCode()).compareTo(nullSafeString(left.getCode()));
                        if (codeCompare != 0) {
                            return codeCompare;
                        }
                        return Long.compare(right.getId(), left.getId());
                    })
                    .toList();
        }

        List<StatisticsTypeExportItem> exportedRows = rows.stream()
                .map(row -> new StatisticsTypeExportItem(
                        row.getId(),
                        row.getCode(),
                        row.getName(),
                        row.getStatisticsCategory(),
                        toCreateTypeLabel(row.getCreateType()),
                        toModifiedTimeLabel(row)
                ))
                .toList();

        String fileName = "statistics-types-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return CodeDataResponse.ok(new BatchExportResult(fileName, exportedRows));
    }

    private ItemStatisticsTypeDO requireById(Long id, ItemScope scope) {
        ItemStatisticsTypeDO row = itemStatisticsTypeMapper.selectOne(scopeQuery(scope)
                .eq(ItemStatisticsTypeDO::getId, id)
                .last("limit 1"));
        if (row == null) {
            throw new BusinessException("统计类型不存在");
        }
        return row;
    }

    private StatisticsTypeListItem toListItem(ItemStatisticsTypeDO row, long index) {
        return new StatisticsTypeListItem(
                row.getId(),
                index,
                row.getCode(),
                row.getName(),
                row.getStatisticsCategory(),
                toCreateTypeLabel(row.getCreateType()),
                toModifiedTimeLabel(row)
        );
    }

    private String toCreateTypeLabel(String createType) {
        if (CREATE_TYPE_SYSTEM_BUILTIN.equals(createType)) {
            return "系统内置";
        }
        return "自定义";
    }

    private String toModifiedTimeLabel(ItemStatisticsTypeDO row) {
        if (CREATE_TYPE_SYSTEM_BUILTIN.equals(row.getCreateType())) {
            return "- -";
        }
        if (row.getUpdatedAt() == null) {
            return "- -";
        }
        return row.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT));
    }

    private String nextCode(ItemScope scope) {
        ItemStatisticsTypeDO latest = itemStatisticsTypeMapper.selectOne(
                scopeQuery(scope)
                        .likeRight(ItemStatisticsTypeDO::getCode, CODE_PREFIX)
                        .orderByDesc(ItemStatisticsTypeDO::getCode)
                        .last("limit 1")
        );
        if (latest == null || trimNullable(latest.getCode()) == null) {
            return CODE_PREFIX + "0001";
        }
        String rawCode = latest.getCode().trim();
        String suffix = rawCode.length() > CODE_PREFIX.length() ? rawCode.substring(CODE_PREFIX.length()) : "";
        try {
            int number = Integer.parseInt(suffix);
            return CODE_PREFIX + String.format(Locale.ROOT, "%04d", number + 1);
        } catch (NumberFormatException ex) {
            return CODE_PREFIX + "0001";
        }
    }

    private String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException("参数不能为空");
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

    private String nullSafeString(String value) {
        return value == null ? "" : value;
    }

    private LambdaQueryWrapper<ItemStatisticsTypeDO> scopeQuery(ItemScope scope) {
        return new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, scope.scopeType())
                .eq(ItemStatisticsTypeDO::getScopeId, scope.scopeId());
    }

    private ItemScope resolveItemScope(String orgId) {
        Long userId = currentUserId();
        Scope requested = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return new ItemScope(requested.scopeType(), requested.scopeId());
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new ItemScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new ItemScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new ItemScope(SCOPE_STORE, requested.scopeId());
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
                .eq(UserRoleRelDO::getStatus, "ENABLED")
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
                .eq(UserRoleRelDO::getStatus, "ENABLED")
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

    public record PageResult<T>(List<T> list, long total, long pageNo, long pageSize) {
    }

    public record StatisticsTypeListItem(Long id,
                                         long index,
                                         String code,
                                         String name,
                                         String statisticsCategory,
                                         String createType,
                                         String modifiedTime) {
    }

    public record StatisticsTypeDetailItem(Long id,
                                           String code,
                                           String name,
                                           String statisticsCategory,
                                           String createType,
                                           String modifiedTime,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
    }

    public record CreateResult(Long id, String code) {
    }

    public record StatisticsTypeExportItem(Long id,
                                           String code,
                                           String name,
                                           String statisticsCategory,
                                           String createType,
                                           String modifiedTime) {
    }

    public record BatchExportResult(String fileName, List<StatisticsTypeExportItem> rows) {
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}
