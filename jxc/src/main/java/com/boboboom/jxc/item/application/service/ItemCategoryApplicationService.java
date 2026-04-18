package com.boboboom.jxc.item.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.item.domain.repository.ItemCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryBatchCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryBatchDeleteRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryStatusUpdateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCategoryUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ItemCategoryApplicationService {

    private static final String ROOT_CATEGORY = "物品类别";
    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "停用";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CATEGORY_CODE_PREFIX = "WPLB";

    private final ItemCategoryRepository itemCategoryRepository;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final OrgScopeService orgScopeService;

    public ItemCategoryApplicationService(ItemCategoryRepository itemCategoryRepository,
                                          BusinessCodeGenerator businessCodeGenerator,
                                          OrgScopeService orgScopeService) {
        this.itemCategoryRepository = itemCategoryRepository;
        this.businessCodeGenerator = businessCodeGenerator;
        this.orgScopeService = orgScopeService;
    }

    public PageData<ItemCategoryRow> list(Integer pageNo,
                                          Integer pageSize,
                                          String categoryInfo,
                                          String status,
                                          String treeNode,
                                          String sortBy,
                                          String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        List<ItemCategoryDO> scopedRows = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        List<ItemCategoryDO> filteredRows = filterList(scopedRows, categoryInfo, status, treeNode, sortBy);
        Long total = (long) filteredRows.size();
        List<ItemCategoryDO> rows = filteredRows.stream()
                .skip(offset)
                .limit(safePageSize)
                .toList();

        List<ItemCategoryRow> list = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ItemCategoryDO row = rows.get(i);
            list.add(new ItemCategoryRow(
                    row.getId(),
                    offset + i + 1,
                    row.getCategoryCode(),
                    row.getCategoryName(),
                    row.getParentCategory(),
                    row.getStatus(),
                    formatDateTime(row.getCreatedAt()),
                    trimNullable(row.getRemark())
            ));
        }

        return new PageData<>(list, total == null ? 0 : total, safePageNo, safePageSize);
    }

    public List<TreeNode> tree(String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        List<ItemCategoryDO> categories = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());

        Map<String, TreeNode> nodeMap = new LinkedHashMap<>();
        nodeMap.put(ROOT_CATEGORY, new TreeNode(ROOT_CATEGORY, new ArrayList<>()));

        for (ItemCategoryDO category : categories) {
            nodeMap.putIfAbsent(category.getCategoryName(), new TreeNode(category.getCategoryName(), new ArrayList<>()));
        }

        for (ItemCategoryDO category : categories) {
            String parentName = normalizeParentCategoryForTree(category.getParentCategory());
            TreeNode parentNode = nodeMap.computeIfAbsent(parentName, key -> new TreeNode(key, new ArrayList<>()));
            TreeNode childNode = nodeMap.get(category.getCategoryName());
            if (childNode != null && parentNode.children().stream().noneMatch(node -> Objects.equals(node.label(), childNode.label()))) {
                parentNode.children().add(childNode);
            }
        }

        return List.of(nodeMap.get(ROOT_CATEGORY));
    }

    @Transactional
    public IdPayload create(String orgId, ItemCategoryCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String categoryCode = generateCategoryCode(scope);
        String categoryName = trim(request.categoryName());
        String parentCategory = normalizeParentCategory(trim(request.parentCategory()));
        String normalizedStatus = normalizeStatus(request.status());
        String remark = trimNullable(request.remark());

        ensureCategoryNameUnique(categoryName, null, scope);
        ensureParentCategoryExists(parentCategory, scope);

        ItemCategoryDO toInsert = new ItemCategoryDO();
        toInsert.setScopeType(scope.scopeType());
        toInsert.setScopeId(scope.scopeId());
        toInsert.setCategoryCode(categoryCode);
        toInsert.setCategoryName(categoryName);
        toInsert.setParentCategory(parentCategory);
        toInsert.setStatus(normalizedStatus);
        toInsert.setRemark(remark);
        itemCategoryRepository.save(toInsert);

        return new IdPayload(toInsert.getId());
    }

    @Transactional
    public BatchCreateResult batchCreate(String orgId, ItemCategoryBatchCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String parentCategory = normalizeParentCategory(trim(request.parentCategory()));
        String normalizedStatus = normalizeStatus(request.status());
        ensureParentCategoryExists(parentCategory, scope);

        List<ItemCategoryBatchCreateRequest.BatchItem> items = request.items();
        Set<String> nameSet = new HashSet<>();
        List<String> categoryNames = new ArrayList<>(items.size());
        BusinessCodeGenerator.CodeAllocator allocator = categoryCodeAllocator(scope);

        for (ItemCategoryBatchCreateRequest.BatchItem item : items) {
            String categoryName = trim(item.categoryName());
            if (!nameSet.add(categoryName)) {
                throw new BusinessException("批量新增中类别名称重复: " + categoryName);
            }
            categoryNames.add(categoryName);
        }

        List<ItemCategoryDO> nameExists = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> categoryNames.contains(row.getCategoryName()))
                .toList();
        if (!nameExists.isEmpty()) {
            throw new BusinessException("类别名称已存在: " + nameExists.get(0).getCategoryName());
        }

        int createdCount = 0;
        for (ItemCategoryBatchCreateRequest.BatchItem item : items) {
            ItemCategoryDO toInsert = new ItemCategoryDO();
            toInsert.setScopeType(scope.scopeType());
            toInsert.setScopeId(scope.scopeId());
            toInsert.setCategoryCode(allocator.nextCode());
            toInsert.setCategoryName(trim(item.categoryName()));
            toInsert.setParentCategory(parentCategory);
            toInsert.setStatus(normalizedStatus);
            toInsert.setRemark(trimNullable(item.remark()));
            itemCategoryRepository.save(toInsert);
            createdCount++;
        }

        return new BatchCreateResult(createdCount);
    }

    @Transactional
    public void update(Long id, String orgId, ItemCategoryUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemCategoryDO existing = requireCategory(id, scope);

        String categoryName = trim(request.categoryName());
        String parentCategory = normalizeParentCategory(trim(request.parentCategory()));
        String normalizedStatus = normalizeStatus(request.status());
        String remark = trimNullable(request.remark());

        ensureCategoryNameUnique(categoryName, id, scope);
        ensureParentCategoryExists(parentCategory, scope);

        if (Objects.equals(parentCategory, existing.getCategoryName())) {
            throw new BusinessException("上级类别不能选择自身");
        }
        if (wouldCreateCycle(scope, existing.getCategoryName(), parentCategory)) {
            throw new BusinessException("上级类别设置会形成循环层级");
        }

        existing.setCategoryName(categoryName);
        existing.setParentCategory(parentCategory);
        existing.setStatus(normalizedStatus);
        existing.setRemark(remark);
        itemCategoryRepository.update(existing);
    }

    @Transactional
    public void updateStatus(Long id, String orgId, ItemCategoryStatusUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemCategoryDO existing = requireCategory(id, scope);
        existing.setStatus(normalizeStatus(request.status()));
        itemCategoryRepository.update(existing);
    }

    @Transactional
    public void delete(Long id, String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        ItemCategoryDO existing = requireCategory(id, scope);
        Long childCount = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> Objects.equals(row.getParentCategory(), existing.getCategoryName()))
                .count();
        if (childCount != null && childCount > 0) {
            throw new BusinessException("当前类别存在下级类别，无法删除");
        }
        itemCategoryRepository.deleteById(id);
    }

    @Transactional
    public void batchDelete(String orgId, ItemCategoryBatchDeleteRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        Set<Long> deleteIdSet = new HashSet<>(request.ids());
        if (deleteIdSet.isEmpty()) {
            throw new BusinessException("删除ID不能为空");
        }

        List<ItemCategoryDO> deleteRows = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> deleteIdSet.contains(row.getId()))
                .toList();
        if (deleteRows.size() != deleteIdSet.size()) {
            throw new BusinessException("存在无效的类别ID");
        }

        Map<String, Long> nameIdMap = new HashMap<>();
        for (ItemCategoryDO row : deleteRows) {
            nameIdMap.put(row.getCategoryName(), row.getId());
        }

        List<ItemCategoryDO> childRows = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> nameIdMap.keySet().contains(row.getParentCategory()))
                .toList();

        for (ItemCategoryDO child : childRows) {
            Long parentId = nameIdMap.get(child.getParentCategory());
            if (parentId != null && !deleteIdSet.contains(child.getId())) {
                throw new BusinessException("类别“" + child.getParentCategory() + "”存在未删除的下级类别，无法批量删除");
            }
        }

        for (Long deleteId : deleteIdSet) {
            itemCategoryRepository.deleteById(deleteId);
        }
    }

    private Set<String> collectTreeScope(ItemScope scope, String treeNodeName) {
        List<ItemCategoryDO> allCategories = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        Map<String, List<String>> parentChildrenMap = new HashMap<>();
        boolean nodeExists = false;
        for (ItemCategoryDO category : allCategories) {
            String categoryName = category.getCategoryName();
            String parentName = normalizeParentCategoryForTree(category.getParentCategory());
            parentChildrenMap.computeIfAbsent(parentName, key -> new ArrayList<>()).add(categoryName);
            if (Objects.equals(categoryName, treeNodeName)) {
                nodeExists = true;
            }
        }
        if (!nodeExists) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(treeNodeName);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            List<String> children = parentChildrenMap.get(current);
            if (children != null && !children.isEmpty()) {
                queue.addAll(children);
            }
        }
        return result;
    }

    private ItemCategoryDO requireCategory(Long id, ItemScope scope) {
        ItemCategoryDO category = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> Objects.equals(row.getId(), id))
                .findFirst()
                .orElse(null);
        if (category == null) {
            throw new BusinessException("类别不存在");
        }
        return category;
    }

    private String generateCategoryCode(ItemScope scope) {
        return categoryCodeAllocator(scope).nextCode();
    }

    private BusinessCodeGenerator.CodeAllocator categoryCodeAllocator(ItemScope scope) {
        List<ItemCategoryDO> exists = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        List<String> codes = exists.stream()
                .map(ItemCategoryDO::getCategoryCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.allocator(CATEGORY_CODE_PREFIX, codes);
    }

    private void ensureCategoryNameUnique(String categoryName, Long excludeId, ItemScope scope) {
        boolean existing = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getCategoryName(), categoryName)
                        && (excludeId == null || !Objects.equals(row.getId(), excludeId)));
        if (existing) {
            throw new BusinessException("类别名称已存在");
        }
    }

    private void ensureParentCategoryExists(String parentCategory, ItemScope scope) {
        if (Objects.equals(parentCategory, ROOT_CATEGORY)) {
            return;
        }
        boolean exists = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getCategoryName(), parentCategory));
        if (!exists) {
            throw new BusinessException("上级类别不存在");
        }
    }

    private boolean wouldCreateCycle(ItemScope scope, String categoryName, String candidateParentName) {
        if (Objects.equals(candidateParentName, ROOT_CATEGORY)) {
            return false;
        }

        String currentParent = candidateParentName;
        while (StringUtils.hasText(currentParent) && !Objects.equals(currentParent, ROOT_CATEGORY)) {
            if (Objects.equals(currentParent, categoryName)) {
                return true;
            }
            String lookupName = currentParent;
            ItemCategoryDO parent = itemCategoryRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                    .filter(row -> Objects.equals(row.getCategoryName(), lookupName))
                    .findFirst()
                    .orElse(null);
            if (parent == null) {
                return false;
            }
            currentParent = parent.getParentCategory();
        }

        return false;
    }

    private String normalizeParentCategoryForTree(String parentCategory) {
        if (!StringUtils.hasText(parentCategory) || Objects.equals(trim(parentCategory), ROOT_CATEGORY)) {
            return ROOT_CATEGORY;
        }
        return trim(parentCategory);
    }

    private String normalizeParentCategory(String parentCategory) {
        if (!StringUtils.hasText(parentCategory)) {
            return ROOT_CATEGORY;
        }
        return trim(parentCategory);
    }

    private String normalizeStatus(String status) {
        String normalized = trim(status);
        if (Objects.equals(normalized, STATUS_ENABLED) || Objects.equals(normalized, "ENABLED")) {
            return STATUS_ENABLED;
        }
        if (Objects.equals(normalized, STATUS_DISABLED) || Objects.equals(normalized, "DISABLED")) {
            return STATUS_DISABLED;
        }
        throw new BusinessException("状态仅支持 启用/停用");
    }

    private String normalizeQueryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = trim(status);
        if (Objects.equals(normalized, "全部") || Objects.equals(normalized, "ALL")) {
            return null;
        }
        return normalizeStatus(normalized);
    }

    private String trim(String value) {
        if (value == null) {
            throw new BusinessException("参数不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(dateTime);
    }

    private List<ItemCategoryDO> filterList(List<ItemCategoryDO> rows,
                                           String categoryInfo,
                                           String status,
                                           String treeNode,
                                           String sortBy) {
        String keyword = StringUtils.hasText(categoryInfo) ? trim(categoryInfo) : null;
        String normalizedQueryStatus = normalizeQueryStatus(status);
        String normalizedTreeNode = trimNullable(treeNode);
        Set<String> scopedNames = null;
        if (StringUtils.hasText(normalizedTreeNode) && !Objects.equals(normalizedTreeNode, ROOT_CATEGORY)) {
            scopedNames = collectTreeScopeFromRows(rows, normalizedTreeNode);
        }
        final Set<String> finalScopedNames = scopedNames;
        final String finalSortBy = trimNullable(sortBy);
        List<ItemCategoryDO> filtered = rows.stream()
                .filter(row -> keyword == null
                        || row.getCategoryCode().contains(keyword)
                        || row.getCategoryName().contains(keyword))
                .filter(row -> normalizedQueryStatus == null || Objects.equals(row.getStatus(), normalizedQueryStatus))
                .filter(row -> finalScopedNames == null || finalScopedNames.contains(row.getCategoryName()))
                .toList();
        if (Objects.equals(finalSortBy, "parentCategory")) {
            return filtered.stream()
                    .sorted((a, b) -> {
                        int c1 = compareNullableString(a.getParentCategory(), b.getParentCategory());
                        if (c1 != 0) {
                            return c1;
                        }
                        int c2 = compareNullableString(a.getCategoryName(), b.getCategoryName());
                        if (c2 != 0) {
                            return c2;
                        }
                        return compareNullableLongDesc(a.getId(), b.getId());
                    })
                    .toList();
        }
        return filtered.stream()
                .sorted((a, b) -> {
                    int c1 = compareNullableDateTimeDesc(a.getCreatedAt(), b.getCreatedAt());
                    if (c1 != 0) {
                        return c1;
                    }
                    return compareNullableLongDesc(a.getId(), b.getId());
                })
                .toList();
    }

    private Set<String> collectTreeScopeFromRows(List<ItemCategoryDO> rows, String treeNodeName) {
        Map<String, List<String>> parentChildrenMap = new HashMap<>();
        boolean nodeExists = false;
        for (ItemCategoryDO category : rows) {
            String categoryName = category.getCategoryName();
            String parentName = normalizeParentCategoryForTree(category.getParentCategory());
            parentChildrenMap.computeIfAbsent(parentName, key -> new ArrayList<>()).add(categoryName);
            if (Objects.equals(categoryName, treeNodeName)) {
                nodeExists = true;
            }
        }
        if (!nodeExists) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(treeNodeName);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            List<String> children = parentChildrenMap.get(current);
            if (children != null && !children.isEmpty()) {
                queue.addAll(children);
            }
        }
        return result;
    }

    private int compareNullableString(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private int compareNullableLongDesc(Long left, Long right) {
        return -compareNullableLong(left, right);
    }

    private int compareNullableDateTimeDesc(LocalDateTime left, LocalDateTime right) {
        return -compareNullableDateTime(left, right);
    }

    private int compareNullableLong(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private int compareNullableDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private ItemScope resolveItemScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new ItemScope(scope.scopeType(), scope.scopeId());
    }

    public record ItemCategoryRow(Long id,
                                  Integer index,
                                  String categoryCode,
                                  String categoryName,
                                  String parentCategory,
                                  String status,
                                  String createdAt,
                                  String remark) {
    }

    public record PageData<T>(List<T> list,
                              long total,
                              int pageNo,
                              int pageSize) {
    }

    public record IdPayload(Long id) {
    }

    public record BatchCreateResult(int createdCount) {
    }

    public record TreeNode(String label, List<TreeNode> children) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}
