package com.boboboom.jxc.item.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemTagMapper;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagBatchImportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagUpdateRequest;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/items/tags")
public class ItemTagController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String STATUS_ENABLED = "启用";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final ItemTagMapper itemTagMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public ItemTagController(ItemTagMapper itemTagMapper,
                             RoleMapper roleMapper,
                             UserRoleRelMapper userRoleRelMapper,
                             StoreMapper storeMapper) {
        this.itemTagMapper = itemTagMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public CodeDataResponse<PageData<ItemTagRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String tagCode,
                                                       @RequestParam(required = false) String tagName,
                                                       @RequestParam(required = false) String itemName,
                                                       @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<ItemTagDO> query = buildListQuery(scope, tagCode, tagName, itemName);
        Long total = itemTagMapper.selectCount(query);
        List<ItemTagDO> rows = itemTagMapper.selectList(buildListQuery(scope, tagCode, tagName, itemName)
                .orderByDesc(ItemTagDO::getUpdatedAt)
                .orderByDesc(ItemTagDO::getId)
                .last("limit " + safePageSize + " offset " + offset));

        List<ItemTagRow> list = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ItemTagDO row = rows.get(i);
            list.add(new ItemTagRow(
                    row.getId(),
                    offset + i + 1,
                    row.getTagCode(),
                    row.getTagName(),
                    trimNullable(row.getRemark()),
                    formatDateTime(row.getCreatedAt()),
                    formatDateTime(row.getUpdatedAt())
            ));
        }

        return CodeDataResponse.ok(new PageData<>(list, total == null ? 0 : total, safePageNo, safePageSize));
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemTagCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String tagCode = trim(request.tagCode());
        String tagName = trim(request.tagName());

        ensureTagCodeUnique(scope, tagCode, null);
        ensureTagNameUnique(scope, tagName, null);

        ItemTagDO row = new ItemTagDO();
        row.setScopeType(scope.scopeType());
        row.setScopeId(scope.scopeId());
        row.setTagCode(tagCode);
        row.setTagName(tagName);
        row.setStatus(STATUS_ENABLED);
        row.setRemark(trimNullable(request.itemName()));
        itemTagMapper.insert(row);

        return CodeDataResponse.ok(new IdPayload(row.getId()));
    }

    @PutMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemTagUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemTagDO existing = requireTag(id, scope);
        String tagCode = trim(request.tagCode());
        String tagName = trim(request.tagName());

        ensureTagCodeUnique(scope, tagCode, id);
        ensureTagNameUnique(scope, tagName, id);

        existing.setTagCode(tagCode);
        existing.setTagName(tagName);
        existing.setRemark(trimNullable(request.itemName()));
        itemTagMapper.updateById(existing);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        requireTag(id, scope);
        itemTagMapper.delete(scopeQuery(scope).eq(ItemTagDO::getId, id));
        return CodeDataResponse.ok();
    }

    @PostMapping("/batch-import")
    @Transactional
    public CodeDataResponse<BatchImportResult> batchImport(@RequestParam(required = false) String orgId,
                                                           @Valid @RequestBody ItemTagBatchImportRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        Set<String> seenCodes = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        int inserted = 0;
        int skipped = 0;

        for (ItemTagBatchImportRequest.Item item : request.items()) {
            String tagCode = trim(item.tagCode());
            String tagName = trim(item.tagName());
            if (!seenCodes.add(tagCode) || !seenNames.add(tagName)) {
                skipped++;
                continue;
            }
            if (existsTagCode(scope, tagCode) || existsTagName(scope, tagName)) {
                skipped++;
                continue;
            }

            ItemTagDO row = new ItemTagDO();
            row.setScopeType(scope.scopeType());
            row.setScopeId(scope.scopeId());
            row.setTagCode(tagCode);
            row.setTagName(tagName);
            row.setStatus(STATUS_ENABLED);
            row.setRemark(trimNullable(item.itemName()));
            itemTagMapper.insert(row);
            inserted++;
        }

        return CodeDataResponse.ok(new BatchImportResult(request.items().size(), inserted, skipped));
    }

    private LambdaQueryWrapper<ItemTagDO> buildListQuery(ItemScope scope,
                                                         String tagCode,
                                                         String tagName,
                                                         String itemName) {
        LambdaQueryWrapper<ItemTagDO> query = scopeQuery(scope);
        String tagCodeKeyword = trimNullable(tagCode);
        if (tagCodeKeyword != null) {
            query.like(ItemTagDO::getTagCode, tagCodeKeyword);
        }
        String tagNameKeyword = trimNullable(tagName);
        if (tagNameKeyword != null) {
            query.like(ItemTagDO::getTagName, tagNameKeyword);
        }
        String itemKeyword = trimNullable(itemName);
        if (itemKeyword != null) {
            query.like(ItemTagDO::getRemark, itemKeyword);
        }
        return query;
    }

    private ItemTagDO requireTag(Long id, ItemScope scope) {
        ItemTagDO row = itemTagMapper.selectOne(scopeQuery(scope)
                .eq(ItemTagDO::getId, id)
                .last("limit 1"));
        if (row == null) {
            throw new BusinessException("标签不存在");
        }
        return row;
    }

    private void ensureTagCodeUnique(ItemScope scope, String tagCode, Long excludeId) {
        LambdaQueryWrapper<ItemTagDO> query = scopeQuery(scope)
                .eq(ItemTagDO::getTagCode, tagCode)
                .last("limit 1");
        if (excludeId != null) {
            query.ne(ItemTagDO::getId, excludeId);
        }
        ItemTagDO row = itemTagMapper.selectOne(query);
        if (row != null) {
            throw new BusinessException("标签编码已存在");
        }
    }

    private void ensureTagNameUnique(ItemScope scope, String tagName, Long excludeId) {
        LambdaQueryWrapper<ItemTagDO> query = scopeQuery(scope)
                .eq(ItemTagDO::getTagName, tagName)
                .last("limit 1");
        if (excludeId != null) {
            query.ne(ItemTagDO::getId, excludeId);
        }
        ItemTagDO row = itemTagMapper.selectOne(query);
        if (row != null) {
            throw new BusinessException("标签名称已存在");
        }
    }

    private boolean existsTagCode(ItemScope scope, String tagCode) {
        Long count = itemTagMapper.selectCount(scopeQuery(scope).eq(ItemTagDO::getTagCode, tagCode));
        return count != null && count > 0;
    }

    private boolean existsTagName(ItemScope scope, String tagName) {
        Long count = itemTagMapper.selectCount(scopeQuery(scope).eq(ItemTagDO::getTagName, tagName));
        return count != null && count > 0;
    }

    private LambdaQueryWrapper<ItemTagDO> scopeQuery(ItemScope scope) {
        return new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scope.scopeType())
                .eq(ItemTagDO::getScopeId, scope.scopeId());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(dateTime);
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

    public record PageData<T>(List<T> list,
                              long total,
                              int pageNo,
                              int pageSize) {
    }

    public record ItemTagRow(Long id,
                             int index,
                             String tagCode,
                             String tagName,
                             String itemName,
                             String createdAt,
                             String updatedAt) {
    }

    public record IdPayload(Long id) {
    }

    public record BatchImportResult(int totalCount,
                                    int insertedCount,
                                    int skippedCount) {
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}

