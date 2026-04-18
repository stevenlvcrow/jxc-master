package com.boboboom.jxc.item.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.item.domain.repository.DishCategoryRepository;
import com.boboboom.jxc.item.domain.repository.DishProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishProfileDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

@Service
public class DishApplicationService {

    private static final String ROOT_CATEGORY = "菜品分类";
    private static final String ROOT_CATEGORY_ID = "all";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final DishProfileRepository dishProfileRepository;
    private final DishCategoryRepository dishCategoryRepository;
    private final OrgScopeService orgScopeService;

    public DishApplicationService(DishProfileRepository dishProfileRepository,
                                  DishCategoryRepository dishCategoryRepository,
                                  OrgScopeService orgScopeService) {
        this.dishProfileRepository = dishProfileRepository;
        this.dishCategoryRepository = dishCategoryRepository;
        this.orgScopeService = orgScopeService;
    }

    public PageData<DishListRow> list(Integer pageNo,
                                      Integer pageSize,
                                      String keyword,
                                      String deleted,
                                      String categoryId,
                                      String dishType,
                                      String orgId) {
        DishScope scope = resolveDishScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        String keywordValue = trimNullable(keyword);
        String deletedValue = normalizeDeleted(deleted);
        String dishTypeValue = trimNullable(dishType);
        Set<Long> categoryIds = resolveCategoryFilter(scope, categoryId);

        List<DishProfileDO> rows = dishProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> matchDeleted(row.getDeleted(), deletedValue))
                .filter(row -> categoryIds == null || categoryIds.isEmpty() || categoryIds.contains(row.getCategoryId()))
                .filter(row -> StringUtils.hasText(keywordValue)
                        ? containsKeyword(row, keywordValue)
                        : true)
                .filter(row -> !StringUtils.hasText(dishTypeValue) || Objects.equals(row.getDishType(), dishTypeValue))
                .skip(offset)
                .limit(safePageSize)
                .toList();
        long total = dishProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> matchDeleted(row.getDeleted(), deletedValue))
                .filter(row -> categoryIds == null || categoryIds.isEmpty() || categoryIds.contains(row.getCategoryId()))
                .filter(row -> StringUtils.hasText(keywordValue)
                        ? containsKeyword(row, keywordValue)
                        : true)
                .filter(row -> !StringUtils.hasText(dishTypeValue) || Objects.equals(row.getDishType(), dishTypeValue))
                .count();

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

        return new PageData<>(list, total, safePageNo, safePageSize);
    }

    public List<TreeNode> categoryTree(String orgId) {
        DishScope scope = resolveDishScope(orgId);
        List<DishCategoryDO> categories = dishCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());

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
        return List.of(nodeMap.get(ROOT_CATEGORY));
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

        List<DishCategoryDO> categories = dishCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
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
        List<DishCategoryDO> categories = dishCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
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

    private DishScope resolveDishScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScopeAllowAnonymous(AuthContextHolder.userIdOr(null), orgId);
        return new DishScope(scope.scopeType(), scope.scopeId());
    }

    private boolean matchDeleted(String rowValue, String queryValue) {
        if (!StringUtils.hasText(queryValue)) {
            return true;
        }
        return Objects.equals(rowValue, queryValue);
    }

    private boolean containsKeyword(DishProfileDO row, String keyword) {
        String joined = String.join(" ",
                nullToEmpty(row.getSpuCode()),
                nullToEmpty(row.getDishName()),
                nullToEmpty(row.getSpec()));
        return joined.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private record DishScope(String scopeType, Long scopeId) {
    }
}
