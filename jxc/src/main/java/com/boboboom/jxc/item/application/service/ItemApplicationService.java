package com.boboboom.jxc.item.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchDeleteRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemBatchStatusUpdateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 物品业务服务，负责物品资料、单位、供应关系和扩展字段维护。 */
@Service
public class ItemApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int GENERATED_ID_RANDOM_LENGTH = 12;
    private static final int UNIT_ATTRIBUTE_SCALE = 3;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String STATUS_ALL = "全部";
    private static final String ITEM_TYPE_DEFAULT = "普通物品";
    private static final String SOURCE_SELF_BUILT = "自建";
    private static final String PLACEHOLDER = "-";
    private static final String ITEM_CODE_PREFIX = "WPBM";

    private final ItemProfileRepository itemProfileRepository;
    private final ObjectMapper objectMapper;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;

    /** 物品业务服务，负责物品资料、单位、供应关系和扩展字段维护。 */
    public ItemApplicationService(ItemProfileRepository itemProfileRepositoryValue,
                                 ObjectMapper objectMapperValue,
                                 BusinessCodeGenerator businessCodeGeneratorValue,
                                 OrgScopeService orgScopeServiceValue,
                                 DictionaryLookupService dictionaryLookupServiceValue) {
        this.itemProfileRepository = itemProfileRepositoryValue;
        this.objectMapper = objectMapperValue;
        this.businessCodeGenerator = businessCodeGeneratorValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 创建业务记录。 */
    @Transactional
    public IdPayload create(String orgId, ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String itemCode = generateItemCode(scope);
        validateRequest(request);

        ItemProfileDO entity = new ItemProfileDO();
        entity.setScopeType(scope.scopeType());
        entity.setScopeId(scope.scopeId());
        entity.setItemId(generateId("itm"));
        entity.setItemCode(itemCode);
        entity.setDetailJson(writeRequestJson(copyWithCode(request, itemCode)));
        entity.setDraft(Boolean.FALSE);
        itemProfileRepository.save(entity);
        return new IdPayload(entity.getItemId());
    }

    /** 保存业务草稿。 */
    @Transactional
    public IdPayload saveDraft(String orgId, ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String itemCode = generateItemCode(scope);

        ItemProfileDO entity = new ItemProfileDO();
        entity.setScopeType(scope.scopeType());
        entity.setScopeId(scope.scopeId());
        entity.setItemId(generateId("draft"));
        entity.setItemCode(itemCode);
        entity.setDetailJson(writeRequestJson(copyWithCode(request, itemCode)));
        entity.setDraft(Boolean.TRUE);
        itemProfileRepository.save(entity);
        return new IdPayload(entity.getItemId());
    }

    /** 查询业务详情。 */
    public ItemCreateRequest detail(String id, String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        ItemProfileDO entity = requireItem(id, scope, true);
        return parseRequestJson(entity.getDetailJson());
    }

    /** 更新业务记录。 */
    @Transactional
    public void update(String id, String orgId, ItemCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemProfileDO entity = requireItem(id, scope, true);
        validateRequest(request);
        String itemCode = trimNullable(entity.getItemCode());
        if (itemCode == null) {
            itemCode = generateItemCode(scope);
            entity.setItemCode(itemCode);
        }
        entity.setDetailJson(writeRequestJson(copyWithCode(request, itemCode)));
        itemProfileRepository.update(entity);
    }

    /** 分页查询业务列表。 */
    public PageData<ItemListRow> list(Integer pageNo,
                                      Integer pageSize,
                                      String keyword,
                                      String category,
                                      String status,
                                      String itemType,
                                      String statType,
                                      String storageMode,
                                      String tag,
                                      String stocktakeFrequency,
                                      String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        String keywordValue = trimNullable(keyword);
        Set<String> categorySet = parseCategorySet(trimNullable(category));
        String statusValue = trimNullable(status);
        String itemTypeValue = trimNullable(itemType);
        String statTypeValue = trimNullable(statType);
        String storageModeValue = trimNullable(storageMode);
        String tagValue = trimNullable(tag);
        String stocktakeFrequencyValue = trimNullable(stocktakeFrequency);

        List<ItemListRow> filtered = itemProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(item -> Boolean.FALSE.equals(item.getDraft()))
                .map(this::toListProjection)
                .filter(row -> matchKeyword(row, keywordValue))
                .filter(row -> matchCategory(row.category(), categorySet))
                .filter(row -> matchCondition(row.status(), statusValue))
                .filter(row -> matchCondition(row.type(), itemTypeValue))
                .filter(row -> matchCondition(row.statType(), statTypeValue))
                .filter(row -> matchStorageMode(row.storageMode(), storageModeValue))
                .filter(row -> matchTag(row.tag(), tagValue))
                .filter(row -> matchCondition(row.stocktakeFrequency(), stocktakeFrequencyValue))
                .toList();

        long total = filtered.size();
        long baseIndex = (long) (safePageNo - 1) * safePageSize;
        List<ItemListRow> pageRows = filtered.stream()
                .skip(baseIndex)
                .limit(safePageSize)
                .toList();

        return new PageData<>(pageRows, total, safePageNo, safePageSize);
    }

    /** 批量更新业务状态。 */
    @Transactional
    public void batchUpdateStatus(String orgId, ItemBatchStatusUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String nextStatus = normalizeStatus(request.status());
        Set<String> idSet = sanitizeAndValidateIds(request.ids());
        List<ItemProfileDO> items = requireItems(scope, idSet);
        for (ItemProfileDO item : items) {
            ItemCreateRequest detail = parseRequestJson(item.getDetailJson());
            item.setDetailJson(writeRequestJson(copyWithStatus(detail, nextStatus)));
            itemProfileRepository.update(item);
        }
    }

    /** 批量删除业务记录。 */
    @Transactional
    public void batchDelete(String orgId, ItemBatchDeleteRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        Set<String> idSet = sanitizeAndValidateIds(request.ids());
        requireItems(scope, idSet);
        itemProfileRepository.deleteByScopeAndItemIds(scope.scopeType(), scope.scopeId(), new java.util.ArrayList<>(idSet));
    }

    private ItemProfileDO requireItem(String itemId, ItemScope scope, boolean includeDraft) {
        ItemProfileDO entity = itemProfileRepository.findByItemId(requiredTrim(itemId, "物品ID不能为空")).orElse(null);
        if (entity == null) {
            throw new BusinessException("物品不存在");
        }
        if (!Objects.equals(entity.getScopeType(), scope.scopeType()) || !Objects.equals(entity.getScopeId(), scope.scopeId())) {
            throw new BusinessException("物品不存在");
        }
        if (!includeDraft && !Boolean.FALSE.equals(entity.getDraft())) {
            throw new BusinessException("物品不存在");
        }
        return entity;
    }

    private List<ItemProfileDO> requireItems(ItemScope scope, Set<String> itemIds) {
        List<ItemProfileDO> items = itemProfileRepository.findByScopeAndItemIds(scope.scopeType(), scope.scopeId(), new java.util.ArrayList<>(itemIds));
        if (items.size() != itemIds.size()) {
            throw new BusinessException("存在无效的物品ID");
        }
        return items;
    }

    private void ensureCodeUnique(ItemScope scope, String itemCode, String excludeItemId, boolean includeDraft) {
        boolean existing = itemProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(item -> Objects.equals(item.getItemCode(), itemCode)
                        && (includeDraft || Boolean.FALSE.equals(item.getDraft()))
                        && (excludeItemId == null || !Objects.equals(item.getItemId(), excludeItemId)));
        if (existing) {
            throw new BusinessException("物品编码已存在");
        }
    }

    private void validateRequest(ItemCreateRequest request) {
        requiredTrim(request.name(), "物品名称不能为空");
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
        normalizeStocktakeFrequency(request.stocktakeFrequency());
        resolveVolume(request);
        resolveWeight(request);
    }

    private ItemListRow toListProjection(ItemProfileDO profile) {
        ItemCreateRequest request = parseRequestJson(profile.getDetailJson());
        String itemCode = trimNullable(profile.getItemCode());
        if (itemCode == null) {
            itemCode = trimNullable(request.code());
        }
        String baseUnit = resolveBaseUnit(request);
        String purchaseUnit = resolveUnit(request.defaultPurchaseUnit(), baseUnit);
        String orderUnit = resolveUnit(request.defaultOrderUnit(), baseUnit);
        String stockUnit = resolveUnit(request.defaultStockUnit(), baseUnit);
        String costUnit = resolveUnit(request.defaultCostUnit(), baseUnit);
        return new ItemListRow(
                profile.getItemId(),
                0,
                defaultIfBlank(itemCode, PLACEHOLDER),
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
                formatDateTime(profile.getUpdatedAt()),
                normalizeStocktakeFrequency(request.stocktakeFrequency())
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

    private ItemCreateRequest copyWithCode(ItemCreateRequest request, String code) {
        try {
            JsonNode root = objectMapper.valueToTree(request);
            if (root instanceof ObjectNode objectNode) {
                objectNode.put("code", code);
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
        return defaultIfBlank(normalizeDecimal(rows.get(0).volume(), UNIT_ATTRIBUTE_SCALE), "0.000");
    }

    private String resolveWeight(ItemCreateRequest request) {
        List<ItemCreateRequest.UnitSettingRow> rows = request.unitSettingRows();
        if (rows == null || rows.isEmpty()) {
            return "0.000";
        }
        return defaultIfBlank(normalizeDecimal(rows.get(0).weight(), UNIT_ATTRIBUTE_SCALE), "0.000");
    }

    private String normalizeStatus(String status) {
        String normalized = requiredTrim(status, "状态不能为空");
        if (DictionaryCodes.ENABLED.equals(normalized) || DictionaryCodes.DISABLED.equals(normalized)) {
            return dictionaryLookupService.codeOf(DictionaryCodes.ITEM_STATUS, normalized);
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.ITEM_STATUS, normalized);
    }

    private String normalizeStorageMode(String storageMode) {
        String normalized = requiredTrim(storageMode, "储存方式不能为空");
        if (Objects.equals(normalized, "冷冻") || Objects.equals(normalized, "冷藏") || Objects.equals(normalized, "常温")) {
            return normalized;
        }
        throw new BusinessException("储存方式仅支持 冷藏/冷冻/常温");
    }

    private String normalizeStocktakeFrequency(String stocktakeFrequency) {
        String normalized = requiredTrim(stocktakeFrequency, "盘点频次不能为空");
        if ("月盘点".equals(normalized) || "MONTHLY".equals(normalized)) {
            throw new BusinessException("盘点频次不支持月盘点，请清理脏数据");
        }
        if (DictionaryCodes.DAILY.equals(normalized) || DictionaryCodes.WEEKLY.equals(normalized)) {
            return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_STOCKTAKE_FREQUENCY, normalized);
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.INVENTORY_STOCKTAKE_FREQUENCY, normalized);
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
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, GENERATED_ID_RANDOM_LENGTH);
    }

    private String generateItemCode(ItemScope scope) {
        List<ItemProfileDO> exists = itemProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        List<String> codes = exists.stream()
                .map(ItemProfileDO::getItemCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(ITEM_CODE_PREFIX, codes);
    }

    private ItemScope resolveItemScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolvePlatformOrStoreScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new ItemScope(scope.scopeType(), scope.scopeId());
    }

    /** 物品与供应商行数据模型，承载列表或报表明细。 */
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
                              String updatedAt,
                              String stocktakeFrequency) {
    }

    /** 物品与供应商分页数据模型，承载列表数据和分页信息。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    /** 物品与供应商载荷模型，承载接口返回的关键标识。 */
    public record IdPayload(String id) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}
