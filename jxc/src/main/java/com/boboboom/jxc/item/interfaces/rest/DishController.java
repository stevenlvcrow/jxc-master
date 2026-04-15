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
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishCategoryMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishProfileMapper;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private static final String ROOT_CATEGORY = "菜品分类";
    private static final String ROOT_CATEGORY_ID = "all";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final DishProfileMapper dishProfileMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public DishController(DishProfileMapper dishProfileMapper,
                          DishCategoryMapper dishCategoryMapper,
                          RoleMapper roleMapper,
                          UserRoleRelMapper userRoleRelMapper,
                          StoreMapper storeMapper) {
        this.dishProfileMapper = dishProfileMapper;
        this.dishCategoryMapper = dishCategoryMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public CodeDataResponse<PageData<DishListRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String deleted,
                                                        @RequestParam(required = false) String categoryId,
                                                        @RequestParam(required = false) String dishType,
                                                        @RequestParam(required = false) String orgId) {
        DishScope scope = resolveDishScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        String keywordValue = trimNullable(keyword);
        String deletedValue = normalizeDeleted(deleted);
        String dishTypeValue = trimNullable(dishType);
        Set<Long> categoryIds = resolveCategoryFilter(scope, categoryId);

        LambdaQueryWrapper<DishProfileDO> query = buildListQuery(scope, keywordValue, deletedValue, categoryIds, dishTypeValue);
        Long total = dishProfileMapper.selectCount(query);

        List<DishProfileDO> rows = dishProfileMapper.selectList(
                buildListQuery(scope, keywordValue, deletedValue, categoryIds, dishTypeValue)
                        .orderByDesc(DishProfileDO::getUpdatedAt)
                        .orderByDesc(DishProfileDO::getId)
                        .last("limit " + safePageSize + " offset " + offset)
        );

        Map<Long, String> categoryNameMap = buildCategoryNameMap(scope);
        List<DishListRow> list = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            DishProfileDO row = rows.get(i);
            list.add(new DishListRow(
                    row.getId(),
                    offset + i + 1,
                    row.getDishId(),
                    row.getSpuCode(),
                    row.getDishName(),
                    row.getSpec(),
                    row.getCategoryId(),
                    categoryNameMap.get(row.getCategoryId()),
                    row.getDishType(),
                    row.getDeleted(),
                    row.getLinkedCostCard(),
                    formatDateTime(row.getCreatedAt()),
                    formatDateTime(row.getUpdatedAt())
            ));
        }

        return CodeDataResponse.ok(new PageData<>(list, total == null ? 0 : total, safePageNo, safePageSize));
    }

    @GetMapping("/categories/tree")
    public CodeDataResponse<List<TreeNode>> categoryTree(@RequestParam(required = false) String orgId) {
        DishScope scope = resolveDishScope(orgId);
        List<DishCategoryDO> categories = dishCategoryMapper.selectList(
                scopeQuery(scope)
                        .orderByAsc(DishCategoryDO::getCreatedAt)
                        .orderByAsc(DishCategoryDO::getId)
        );

        Map<String, TreeNode> nodeMap = new LinkedHashMap<>();
        nodeMap.put(ROOT_CATEGORY, new TreeNode(ROOT_CATEGORY_ID, ROOT_CATEGORY, new ArrayList<>()));
        for (DishCategoryDO category : categories) {
            nodeMap.putIfAbsent(category.getCategoryName(),
                    new TreeNode(String.valueOf(category.getId()), category.getCategoryName(), new ArrayList<>()));
        }

        for (DishCategoryDO category : categories) {
            String parentName = normalizeParentForTree(category.getParentCategory());
            TreeNode parentNode = nodeMap.computeIfAbsent(parentName, key -> new TreeNode(key, key, new ArrayList<>()));
            TreeNode childNode = nodeMap.get(category.getCategoryName());
            if (childNode != null && parentNode.children().stream().noneMatch(item -> Objects.equals(item.id(), childNode.id()))) {
                parentNode.children().add(childNode);
            }
        }
        return CodeDataResponse.ok(List.of(nodeMap.get(ROOT_CATEGORY)));
    }

    private LambdaQueryWrapper<DishProfileDO> buildListQuery(DishScope scope,
                                                             String keyword,
                                                             String deleted,
                                                             Set<Long> categoryIds,
                                                             String dishType) {
        LambdaQueryWrapper<DishProfileDO> query = profileScopeQuery(scope);
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(DishProfileDO::getSpuCode, keyword)
                    .or().like(DishProfileDO::getDishName, keyword)
                    .or().like(DishProfileDO::getSpec, keyword));
        }
        if (StringUtils.hasText(deleted)) {
            query.eq(DishProfileDO::getDeleted, deleted);
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.in(DishProfileDO::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(dishType)) {
            query.eq(DishProfileDO::getDishType, dishType);
        }
        return query;
    }

    private Set<Long> resolveCategoryFilter(DishScope scope, String categoryId) {
        String normalized = trimNullable(categoryId);
        if (normalized == null || Objects.equals(normalized, ROOT_CATEGORY_ID)) {
            return null;
        }

        Long rootId;
        try {
            rootId = Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            throw new BusinessException("菜品分类参数非法");
        }

        List<DishCategoryDO> categories = dishCategoryMapper.selectList(
                scopeQuery(scope)
                        .orderByAsc(DishCategoryDO::getId)
        );
        Map<Long, DishCategoryDO> categoryById = categories.stream()
                .collect(Collectors.toMap(DishCategoryDO::getId, item -> item));
        DishCategoryDO rootCategory = categoryById.get(rootId);
        if (rootCategory == null) {
            throw new BusinessException("菜品分类不存在");
        }

        Map<String, List<DishCategoryDO>> childrenByParent = categories.stream()
                .collect(Collectors.groupingBy(item -> normalizeParentForTree(item.getParentCategory())));
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(rootCategory.getCategoryName());
        java.util.LinkedHashSet<Long> descendants = new java.util.LinkedHashSet<>();
        descendants.add(rootId);

        while (!queue.isEmpty()) {
            String parentName = queue.removeFirst();
            List<DishCategoryDO> children = childrenByParent.get(parentName);
            if (children == null) {
                continue;
            }
            for (DishCategoryDO child : children) {
                if (descendants.add(child.getId())) {
                    queue.addLast(child.getCategoryName());
                }
            }
        }

        return descendants;
    }

    private Map<Long, String> buildCategoryNameMap(DishScope scope) {
        List<DishCategoryDO> categories = dishCategoryMapper.selectList(scopeQuery(scope));
        Map<Long, String> map = new LinkedHashMap<>();
        for (DishCategoryDO category : categories) {
            map.put(category.getId(), category.getCategoryName());
        }
        return map;
    }

    private String normalizeParentForTree(String parentCategory) {
        if (!StringUtils.hasText(parentCategory)) {
            return ROOT_CATEGORY;
        }
        String normalized = parentCategory.trim();
        if (normalized.isEmpty() || Objects.equals(normalized, ROOT_CATEGORY)) {
            return ROOT_CATEGORY;
        }
        return normalized;
    }

    private String normalizeDeleted(String deleted) {
        String value = trimNullable(deleted);
        if (value == null) {
            return null;
        }
        if (!Objects.equals(value, "Y") && !Objects.equals(value, "N")) {
            throw new BusinessException("菜品是否删除参数非法");
        }
        return value;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(dateTime);
    }

    private LambdaQueryWrapper<DishCategoryDO> scopeQuery(DishScope scope) {
        return new LambdaQueryWrapper<DishCategoryDO>()
                .eq(DishCategoryDO::getScopeType, scope.scopeType())
                .eq(DishCategoryDO::getScopeId, scope.scopeId());
    }

    private LambdaQueryWrapper<DishProfileDO> profileScopeQuery(DishScope scope) {
        return new LambdaQueryWrapper<DishProfileDO>()
                .eq(DishProfileDO::getScopeType, scope.scopeType())
                .eq(DishProfileDO::getScopeId, scope.scopeId());
    }

    private DishScope resolveDishScope(String orgId) {
        Scope requested = parseScope(orgId);
        Long userId = currentUserIdNullable();
        if (userId == null) {
            return new DishScope(requested.scopeType(), requested.scopeId());
        }
        if (isPlatformAdmin(userId)) {
            return new DishScope(requested.scopeType(), requested.scopeId());
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new DishScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new DishScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new DishScope(SCOPE_STORE, requested.scopeId());
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

    private Long currentUserIdNullable() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            return null;
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

    public record DishListRow(Long id,
                              int index,
                              String dishId,
                              String spuCode,
                              String dishName,
                              String spec,
                              Long categoryId,
                              String category,
                              String dishType,
                              String deleted,
                              String linkedCostCard,
                              String createdAt,
                              String updatedAt) {
    }

    public record TreeNode(String id,
                           String label,
                           List<TreeNode> children) {
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record DishScope(String scopeType, Long scopeId) {
    }
}
