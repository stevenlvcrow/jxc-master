package com.boboboom.jxc.item.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemProfileMapper;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchDeleteRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchStatusUpdateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "停用";
    private static final String STATUS_ALL = "全部";
    private static final String ITEM_TYPE_DEFAULT = "普通物品";
    private static final String SOURCE_SELF_BUILT = "自建";
    private static final String PLACEHOLDER = "-";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final ItemProfileMapper itemProfileMapper;
    private final ObjectMapper objectMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public ItemController(ItemProfileMapper itemProfileMapper,
                          ObjectMapper objectMapper,
                          RoleMapper roleMapper,
                          UserRoleRelMapper userRoleRelMapper,
                          StoreMapper storeMapper) {
        this.itemProfileMapper = itemProfileMapper;
        this.objectMapper = objectMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String itemCode = requiredTrim(request.code(), "物品编码不能为空");
        validateRequest(request);
        ensureCodeUnique(scope, itemCode, null, false);

        ItemProfileDO entity = new ItemProfileDO();
        entity.setScopeType(scope.scopeType());
        entity.setScopeId(scope.scopeId());
        entity.setItemId(generateId("itm"));
        entity.setItemCode(itemCode);
        entity.setDetailJson(writeRequestJson(request));
        entity.setDraft(Boolean.FALSE);
        itemProfileMapper.insert(entity);
        return CodeDataResponse.ok(new IdPayload(entity.getItemId()));
    }

    @PostMapping("/drafts")
    @Transactional
    public CodeDataResponse<IdPayload> saveDraft(@RequestParam(required = false) String orgId,
                                                 @RequestBody ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String itemCode = trimNullable(request.code());
        if (itemCode != null) {
            ensureCodeUnique(scope, itemCode, null, true);
        }

        ItemProfileDO entity = new ItemProfileDO();
        entity.setScopeType(scope.scopeType());
        entity.setScopeId(scope.scopeId());
        entity.setItemId(generateId("draft"));
        entity.setItemCode(itemCode == null ? entity.getItemId() : itemCode);
        entity.setDetailJson(writeRequestJson(request));
        entity.setDraft(Boolean.TRUE);
        itemProfileMapper.insert(entity);
        return CodeDataResponse.ok(new IdPayload(entity.getItemId()));
    }

    @GetMapping("/{id}")
    public CodeDataResponse<ItemCreateRequest> detail(@PathVariable String id,
                                                      @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        ItemProfileDO entity = requireItem(id, scope, true);
        return CodeDataResponse.ok(parseRequestJson(entity.getDetailJson()));
    }

    @PutMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> update(@PathVariable String id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemProfileDO entity = requireItem(id, scope, true);
        String itemCode = requiredTrim(request.code(), "物品编码不能为空");
        validateRequest(request);
        ensureCodeUnique(scope, itemCode, id, Boolean.TRUE.equals(entity.getDraft()));

        entity.setItemCode(itemCode);
        entity.setDetailJson(writeRequestJson(request));
        itemProfileMapper.updateById(entity);
        return CodeDataResponse.ok();
    }

    @GetMapping
    public CodeDataResponse<PageData<ItemListRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String itemType,
                                                        @RequestParam(required = false) String statType,
                                                        @RequestParam(required = false) String storageMode,
                                                        @RequestParam(required = false) String tag,
                                                        @RequestParam(required = false) String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);

        String keywordValue = trimNullable(keyword);
        Set<String> categorySet = parseCategorySet(trimNullable(category));
        String statusValue = trimNullable(status);
        String itemTypeValue = trimNullable(itemType);
        String statTypeValue = trimNullable(statType);
        String storageModeValue = trimNullable(storageMode);
        String tagValue = trimNullable(tag);

        // 使用 MyBatis-Plus IPage 数据库分页，仅查询当前页数据
        IPage<ItemProfileDO> pageResult = itemProfileMapper.selectPage(
                new Page<>(safePageNo, safePageSize),
                scopeQuery(scope)
                        .eq(ItemProfileDO::getDraft, Boolean.FALSE)
                        .orderByDesc(ItemProfileDO::getCreatedAt)
                        .orderByDesc(ItemProfileDO::getId)
        );

        // 仅对当前页数据进行投影和内存过滤（detailJson字段无法下推SQL）
        List<ItemListRow> filtered = pageResult.getRecords().stream()
                .map(this::toListProjection)
                .filter(row -> matchKeyword(row, keywordValue))
                .filter(row -> matchCategory(row.category(), categorySet))
                .filter(row -> matchCondition(row.status(), statusValue))
                .filter(row -> matchCondition(row.type(), itemTypeValue))
                .filter(row -> matchCondition(row.statType(), statTypeValue))
                .filter(row -> matchStorageMode(row.storageMode(), storageModeValue))
                .filter(row -> matchTag(row.tag(), tagValue))
                .toList();

        return CodeDataResponse.ok(new PageData<>(filtered, pageResult.getTotal(), safePageNo, safePageSize));
    }

    @PostMapping("/batch-status")
    @Transactional
    public CodeDataResponse<Void> batchUpdateStatus(@RequestParam(required = false) String orgId,
                                                    @Valid @RequestBody ItemBatchStatusUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String nextStatus = normalizeStatus(request.status());
        Set<String> idSet = sanitizeAndValidateIds(request.ids());
        List<ItemProfileDO> items = requireItems(scope, idSet);
        for (ItemProfileDO item : items) {
            ItemCreateRequest detail = parseRequestJson(item.getDetailJson());
            item.setDetailJson(writeRequestJson(copyWithStatus(detail, nextStatus)));
            itemProfileMapper.updateById(item);
        }
        return CodeDataResponse.ok();
    }

    @PostMapping("/batch-delete")
    @Transactional
    public CodeDataResponse<Void> batchDelete(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody ItemBatchDeleteRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        Set<String> idSet = sanitizeAndValidateIds(request.ids());
        requireItems(scope, idSet);
        itemProfileMapper.delete(scopeQuery(scope)
                .eq(ItemProfileDO::getDraft, Boolean.FALSE)
                .in(ItemProfileDO::getItemId, idSet));
        return CodeDataResponse.ok();
    }

    private ItemProfileDO requireItem(String itemId, ItemScope scope, boolean includeDraft) {
        LambdaQueryWrapper<ItemProfileDO> query = scopeQuery(scope)
                .eq(ItemProfileDO::getItemId, requiredTrim(itemId, "物品ID不能为空"))
                .last("limit 1");
        if (!includeDraft) {
            query.eq(ItemProfileDO::getDraft, Boolean.FALSE);
        }
        ItemProfileDO entity = itemProfileMapper.selectOne(query);
        if (entity == null) {
            throw new BusinessException("物品不存在");
        }
        return entity;
    }

    private List<ItemProfileDO> requireItems(ItemScope scope, Set<String> itemIds) {
        List<ItemProfileDO> items = itemProfileMapper.selectList(
                scopeQuery(scope)
                        .eq(ItemProfileDO::getDraft, Boolean.FALSE)
                        .in(ItemProfileDO::getItemId, itemIds)
        );
        if (items.size() != itemIds.size()) {
            throw new BusinessException("存在无效的物品ID");
        }
        return items;
    }

    private void ensureCodeUnique(ItemScope scope, String itemCode, String excludeItemId, boolean includeDraft) {
        LambdaQueryWrapper<ItemProfileDO> query = scopeQuery(scope)
                .eq(ItemProfileDO::getItemCode, itemCode)
                .last("limit 1");
        if (!includeDraft) {
            query.eq(ItemProfileDO::getDraft, Boolean.FALSE);
        }
        if (excludeItemId != null) {
            query.ne(ItemProfileDO::getItemId, excludeItemId);
        }
        ItemProfileDO existing = itemProfileMapper.selectOne(query);
        if (existing != null) {
            throw new BusinessException("物品编码已存在");
        }
    }

    private void validateRequest(ItemCreateRequest request) {
        requiredTrim(request.name(), "物品名称不能为空");
        requiredTrim(request.code(), "物品编码不能为空");
        requiredTrim(request.category(), "物品类别不能为空");
        normalizeStatus(request.status());
        normalizeStorageMode(request.storageMode());
        normalizeNumericString(request.stockMin());
        normalizeNumericString(request.stockMax());
        normalizeMoneyString(request.productionRefCost());
        normalizeMoneyString(request.suggestPurchasePrice());
        validateStockRange(
                normalizeNumericString(request.stockMin()),
                normalizeNumericString(request.stockMax())
        );
        resolveVolume(request);
        resolveWeight(request);
    }

    private ItemListRow toListProjection(ItemProfileDO profile) {
        ItemCreateRequest request = parseRequestJson(profile.getDetailJson());
        String baseUnit = resolveBaseUnit(request);
        String purchaseUnit = resolveUnit(request.defaultPurchaseUnit(), baseUnit);
        String orderUnit = resolveUnit(request.defaultOrderUnit(), baseUnit);
        String stockUnit = resolveUnit(request.defaultStockUnit(), baseUnit);
        String costUnit = resolveUnit(request.defaultCostUnit(), baseUnit);
        return new ItemListRow(
                profile.getItemId(),
                0,
                requiredTrim(request.code(), "物品编码不能为空"),
                requiredTrim(request.name(), "物品名称不能为空"),
                defaultIfBlank(trimNullable(request.spec()), PLACEHOLDER),
                ITEM_TYPE_DEFAULT,
                requiredTrim(request.category(), "物品类别不能为空"),
                defaultIfBlank(trimNullable(request.brand()), PLACEHOLDER),
                defaultIfBlank(normalizeMoneyString(request.productionRefCost()), "0.00"),
                baseUnit,
                purchaseUnit,
                orderUnit,
                stockUnit,
                costUnit,
                defaultIfBlank(normalizeMoneyString(request.suggestPurchasePrice()), "0.00"),
                resolveVolume(request),
                resolveWeight(request),
                defaultIfBlank(trimNullable(request.statType()), PLACEHOLDER),
                defaultIfBlank(trimNullable(request.thirdPartyCode()), PLACEHOLDER),
                defaultIfBlank(trimNullable(request.abcClass()), PLACEHOLDER),
                defaultIfBlank(normalizeNumericString(request.stockMin()), "0"),
                defaultIfBlank(normalizeNumericString(request.stockMax()), "0"),
                SOURCE_SELF_BUILT,
                normalizeStatus(request.status()),
                normalizeStorageMode(request.storageMode()),
                defaultIfBlank(trimNullable(request.tag()), PLACEHOLDER),
                hasImages(request.introImages()) ? "已上传" : "未上传",
                formatDateTime(profile.getCreatedAt()),
                formatDateTime(profile.getUpdatedAt())
        );
    }

    private ItemCreateRequest copyWithStatus(ItemCreateRequest request, String status) {
        try {
            JsonNode root = objectMapper.valueToTree(request);
            if (root instanceof ObjectNode objectNode) {
                objectNode.put("status", status);
            }
            return objectMapper.treeToValue(root, ItemCreateRequest.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("物品详情序列化失败");
        }
    }

    private boolean matchKeyword(ItemListRow row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String text = keyword.toLowerCase(Locale.ROOT);
        String joined = (row.code() + " " + row.name() + " " + row.spec() + " " + row.thirdPartyCode()).toLowerCase(Locale.ROOT);
        return joined.contains(text);
    }

    private boolean matchCondition(String rowValue, String queryValue) {
        if (!StringUtils.hasText(queryValue) || Objects.equals(queryValue, STATUS_ALL)) {
            return true;
        }
        return Objects.equals(rowValue, queryValue);
    }

    private boolean matchCategory(String rowValue, Set<String> categorySet) {
        if (categorySet == null || categorySet.isEmpty()) {
            return true;
        }
        return categorySet.contains(rowValue);
    }

    private Set<String> parseCategorySet(String category) {
        if (!StringUtils.hasText(category)) {
            return Set.of();
        }
        return List.of(category.split(","))
                .stream()
                .map(this::trimNullable)
                .filter(StringUtils::hasText)
                .filter(value -> !Objects.equals(value, "物品类别"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean matchStorageMode(String rowValue, String queryValue) {
        if (!StringUtils.hasText(queryValue)) {
            return true;
        }
        return Objects.equals(rowValue, queryValue);
    }

    private boolean matchTag(String rowValue, String queryValue) {
        if (!StringUtils.hasText(queryValue)) {
            return true;
        }
        return rowValue.contains(queryValue);
    }

    private Set<String> sanitizeAndValidateIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("物品ID不能为空");
        }
        Set<String> idSet = new LinkedHashSet<>();
        for (String id : ids) {
            idSet.add(requiredTrim(id, "物品ID不能为空"));
        }
        return idSet;
    }

    private ItemCreateRequest parseRequestJson(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            throw new BusinessException("物品详情数据为空");
        }
        try {
            return objectMapper.readValue(detailJson, ItemCreateRequest.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("物品详情数据已损坏");
        }
    }

    private String writeRequestJson(ItemCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("物品详情序列化失败");
        }
    }

    private String resolveBaseUnit(ItemCreateRequest request) {
        List<ItemCreateRequest.UnitSettingRow> rows = request.unitSettingRows();
        if (rows == null || rows.isEmpty()) {
            return "斤";
        }
        String unit = trimNullable(rows.get(0).unit());
        return unit == null ? "斤" : unit;
    }

    private String resolveUnit(String unit, String fallback) {
        String normalized = trimNullable(unit);
        return normalized == null ? fallback : normalized;
    }

    private String resolveVolume(ItemCreateRequest request) {
        List<ItemCreateRequest.UnitSettingRow> rows = request.unitSettingRows();
        if (rows == null || rows.isEmpty()) {
            return "0.000";
        }
        return defaultIfBlank(normalizeDecimal(rows.get(0).volume(), 3), "0.000");
    }

    private String resolveWeight(ItemCreateRequest request) {
        List<ItemCreateRequest.UnitSettingRow> rows = request.unitSettingRows();
        if (rows == null || rows.isEmpty()) {
            return "0.000";
        }
        return defaultIfBlank(normalizeDecimal(rows.get(0).weight(), 3), "0.000");
    }

    private String normalizeStatus(String status) {
        String normalized = requiredTrim(status, "状态不能为空");
        if (Objects.equals(normalized, STATUS_ENABLED) || Objects.equals(normalized, "ENABLED")) {
            return STATUS_ENABLED;
        }
        if (Objects.equals(normalized, STATUS_DISABLED) || Objects.equals(normalized, "DISABLED")) {
            return STATUS_DISABLED;
        }
        throw new BusinessException("状态仅支持 启用/停用");
    }

    private String normalizeStorageMode(String storageMode) {
        String normalized = requiredTrim(storageMode, "储存方式不能为空");
        if (Objects.equals(normalized, "冷冻") || Objects.equals(normalized, "冷藏") || Objects.equals(normalized, "常温")) {
            return normalized;
        }
        throw new BusinessException("储存方式仅支持 冷藏/冷冻/常温");
    }

    private void validateStockRange(String stockMin, String stockMax) {
        if (stockMin == null || stockMax == null) {
            return;
        }
        try {
            BigDecimal min = new BigDecimal(stockMin);
            BigDecimal max = new BigDecimal(stockMax);
            if (min.compareTo(max) > 0) {
                throw new BusinessException("库存下限不能大于库存上限");
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException("库存上下限格式不正确");
        }
    }

    private String normalizeMoneyString(String value) {
        return normalizeDecimal(value, 2);
    }

    private String normalizeNumericString(String value) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw new BusinessException("数值格式不正确");
        }
    }

    private String normalizeDecimal(String value, int scale) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized).setScale(scale, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            throw new BusinessException("金额格式不正确");
        }
    }

    private boolean hasImages(List<ItemCreateRequest.IntroImageRow> images) {
        return images != null && images.stream().anyMatch(image -> image != null && StringUtils.hasText(image.url()));
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(value);
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private LambdaQueryWrapper<ItemProfileDO> scopeQuery(ItemScope scope) {
        return new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getScopeType, scope.scopeType())
                .eq(ItemProfileDO::getScopeId, scope.scopeId());
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

    public record ItemListRow(String id,
                              Integer index,
                              String code,
                              String name,
                              String spec,
                              String type,
                              String category,
                              String brand,
                              String productionCost,
                              String baseUnit,
                              String purchaseUnit,
                              String orderUnit,
                              String stockUnit,
                              String costUnit,
                              String suggestPrice,
                              String volume,
                              String weight,
                              String statType,
                              String thirdPartyCode,
                              String abcClass,
                              String stockMin,
                              String stockMax,
                              String source,
                              String status,
                              String storageMode,
                              String tag,
                              String image,
                              String createdAt,
                              String updatedAt) {
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    public record IdPayload(String id) {
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}
