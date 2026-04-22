package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.DictionaryRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictFieldBindingDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictTypeDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 字典管理应用服务。
 */
@Service
public class DictionaryApplicationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final Pattern DICT_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");
    private static final Pattern ITEM_KEY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final DictionaryRepository dictionaryRepository;
    private final DictionaryLookupService dictionaryLookupService;

    /**
     * 构造字典管理服务。
     *
     * @param dictionaryRepository 字典仓储
     * @param dictionaryLookupService 字典查询服务
     */
    public DictionaryApplicationService(DictionaryRepository dictionaryRepository,
                                        DictionaryLookupService dictionaryLookupService) {
        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryLookupService = dictionaryLookupService;
    }

    /**
     * 查询字典类型列表。
     *
     * @param keyword 关键字
     * @param status 状态
     * @return 字典类型列表
     */
    public List<DictionaryTypeView> listTypes(String keyword, String status) {
        String normalizedKeyword = trimNullable(keyword);
        String normalizedStatus = normalizeStatusNullable(status);
        return dictionaryRepository.findTypes()
                .stream()
                .filter(type -> normalizedKeyword == null
                        || contains(type.getDictCode(), normalizedKeyword)
                        || contains(type.getDictName(), normalizedKeyword))
                .filter(type -> normalizedStatus == null || Objects.equals(type.getStatus(), normalizedStatus))
                .map(type -> new DictionaryTypeView(
                        type.getId(),
                        type.getDictCode(),
                        type.getDictName(),
                        type.getCategory(),
                        type.getStatus(),
                        Boolean.TRUE.equals(type.getBuiltin()),
                        defaultSortNo(type.getSortNo()),
                        type.getRemark()
                ))
                .toList();
    }

    /**
     * 创建字典类型。
     *
     * @param request 创建请求
     * @return 字典类型主键
     */
    @Transactional
    public Long createType(DictionaryTypeRequest request) {
        String dictCode = normalizeDictCode(request.dictCode());
        ensureTypeCodeUnique(dictCode, null);
        DictTypeDO type = new DictTypeDO();
        type.setDictCode(dictCode);
        type.setDictName(trimRequired(request.dictName(), "字典名称不能为空"));
        type.setCategory(defaultIfBlank(request.category(), "BUSINESS"));
        type.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        type.setBuiltin(Boolean.FALSE);
        type.setSortNo(defaultSortNo(request.sortNo()));
        type.setRemark(trimNullable(request.remark()));
        dictionaryRepository.saveType(type);
        return type.getId();
    }

    /**
     * 更新字典类型。
     *
     * @param id 字典类型主键
     * @param request 更新请求
     */
    @Transactional
    public void updateType(Long id, DictionaryTypeRequest request) {
        DictTypeDO type = requireType(id);
        String nextCode = normalizeDictCode(request.dictCode());
        if (Boolean.TRUE.equals(type.getBuiltin()) && !Objects.equals(type.getDictCode(), nextCode)) {
            throw new BusinessException("内置字典编码不可修改");
        }
        ensureTypeCodeUnique(nextCode, id);
        type.setDictCode(nextCode);
        type.setDictName(trimRequired(request.dictName(), "字典名称不能为空"));
        type.setCategory(defaultIfBlank(request.category(), "BUSINESS"));
        type.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        type.setSortNo(defaultSortNo(request.sortNo()));
        type.setRemark(trimNullable(request.remark()));
        dictionaryRepository.updateType(type);
    }

    /**
     * 更新字典类型状态。
     *
     * @param id 字典类型主键
     * @param status 状态
     */
    @Transactional
    public void updateTypeStatus(Long id, String status) {
        DictTypeDO type = requireType(id);
        type.setStatus(normalizeStatus(status, STATUS_ENABLED));
        dictionaryRepository.updateType(type);
    }

    /**
     * 删除字典类型。
     *
     * @param id 字典类型主键
     */
    @Transactional
    public void deleteType(Long id) {
        DictTypeDO type = requireType(id);
        if (Boolean.TRUE.equals(type.getBuiltin())) {
            throw new BusinessException("内置字典不可删除");
        }
        if (!dictionaryRepository.findItemsByTypeId(id, false).isEmpty()) {
            throw new BusinessException("字典下存在字典项，不能删除");
        }
        dictionaryRepository.deleteType(id);
    }

    /**
     * 查询字典项列表。
     *
     * @param dictTypeId 字典类型主键
     * @return 字典项列表
     */
    public List<DictionaryItemView> listItems(Long dictTypeId) {
        requireType(dictTypeId);
        return dictionaryRepository.findItemsByTypeId(dictTypeId, false)
                .stream()
                .map(DictionaryItemView::from)
                .toList();
    }

    /**
     * 创建字典项。
     *
     * @param dictTypeId 字典类型主键
     * @param request 创建请求
     * @return 字典项主键
     */
    @Transactional
    public Long createItem(Long dictTypeId, DictionaryItemRequest request) {
        DictTypeDO type = requireType(dictTypeId);
        String itemKey = normalizeItemKey(request.itemKey());
        String itemCode = trimRequired(request.itemCode(), "字典项存储值不能为空");
        ensureItemKeyUnique(dictTypeId, itemKey, null);
        ensureItemCodeUnique(dictTypeId, itemCode, null);
        ensureParentBelongsToType(dictTypeId, request.parentId());
        DictItemDO item = new DictItemDO();
        item.setDictTypeId(dictTypeId);
        item.setParentId(request.parentId());
        item.setItemKey(itemKey);
        item.setItemCode(itemCode);
        item.setItemLabel(trimRequired(request.itemLabel(), "字典项展示值不能为空"));
        item.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        item.setBuiltin(Boolean.FALSE);
        item.setSortNo(defaultSortNo(request.sortNo()));
        item.setExtraJson(trimNullable(request.extraJson()));
        item.setRemark(trimNullable(request.remark()));
        dictionaryRepository.saveItem(item);
        return item.getId();
    }

    /**
     * 更新字典项。
     *
     * @param id 字典项主键
     * @param request 更新请求
     */
    @Transactional
    public void updateItem(Long id, DictionaryItemRequest request) {
        DictItemDO item = requireItem(id);
        DictTypeDO type = requireType(item.getDictTypeId());
        String nextKey = normalizeItemKey(request.itemKey());
        String nextCode = trimRequired(request.itemCode(), "字典项存储值不能为空");
        if (Boolean.TRUE.equals(item.getBuiltin()) && !Objects.equals(item.getItemKey(), nextKey)) {
            throw new BusinessException("内置字典项键不可修改");
        }
        ensureItemKeyUnique(item.getDictTypeId(), nextKey, id);
        ensureItemCodeUnique(item.getDictTypeId(), nextCode, id);
        ensureParentBelongsToType(item.getDictTypeId(), request.parentId());
        if (!Objects.equals(item.getItemCode(), nextCode)) {
            migrateItemCode(type.getDictCode(), item.getItemCode(), nextCode);
        }
        item.setParentId(request.parentId());
        item.setItemKey(nextKey);
        item.setItemCode(nextCode);
        item.setItemLabel(trimRequired(request.itemLabel(), "字典项展示值不能为空"));
        item.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        item.setSortNo(defaultSortNo(request.sortNo()));
        item.setExtraJson(trimNullable(request.extraJson()));
        item.setRemark(trimNullable(request.remark()));
        dictionaryRepository.updateItem(item);
    }

    /**
     * 更新字典项状态。
     *
     * @param id 字典项主键
     * @param status 状态
     */
    @Transactional
    public void updateItemStatus(Long id, String status) {
        DictItemDO item = requireItem(id);
        DictTypeDO type = requireType(item.getDictTypeId());
        String normalizedStatus = normalizeStatus(status, STATUS_ENABLED);
        if (STATUS_DISABLED.equals(normalizedStatus)) {
            ensureItemNotReferenced(type.getDictCode(), item.getItemCode(), "字典项已被业务数据引用，不能停用");
        }
        item.setStatus(normalizedStatus);
        dictionaryRepository.updateItem(item);
    }

    /**
     * 删除字典项。
     *
     * @param id 字典项主键
     */
    @Transactional
    public void deleteItem(Long id) {
        DictItemDO item = requireItem(id);
        DictTypeDO type = requireType(item.getDictTypeId());
        if (Boolean.TRUE.equals(item.getBuiltin())) {
            throw new BusinessException("内置字典项不可删除");
        }
        ensureItemNotReferenced(type.getDictCode(), item.getItemCode(), "字典项已被业务数据引用，不能删除");
        dictionaryRepository.deleteItem(id);
    }

    /**
     * 批量查询业务可用字典项。
     *
     * @param dictCodes 字典编码列表
     * @return 字典项分组
     */
    public Map<String, List<DictionaryLookupService.DictionaryItemOption>> listEnabledItemsByCodes(List<String> dictCodes) {
        Map<String, List<DictionaryLookupService.DictionaryItemOption>> result = new LinkedHashMap<>();
        for (String dictCode : dictCodes) {
            String normalizedCode = trimNullable(dictCode);
            if (normalizedCode != null) {
                result.put(normalizedCode, dictionaryLookupService.itemsOf(normalizedCode));
            }
        }
        return result;
    }

    private DictTypeDO requireType(Long id) {
        return dictionaryRepository.findTypeById(id)
                .orElseThrow(() -> new BusinessException("字典不存在"));
    }

    private DictItemDO requireItem(Long id) {
        return dictionaryRepository.findItemById(id)
                .orElseThrow(() -> new BusinessException("字典项不存在"));
    }

    private void ensureTypeCodeUnique(String dictCode, Long excludedId) {
        dictionaryRepository.findTypeByCode(dictCode)
                .filter(type -> excludedId == null || !Objects.equals(type.getId(), excludedId))
                .ifPresent(type -> {
                    throw new BusinessException("字典编码已存在");
                });
    }

    private void ensureItemKeyUnique(Long dictTypeId, String itemKey, Long excludedId) {
        dictionaryRepository.findItemByKey(dictTypeId, itemKey)
                .filter(item -> excludedId == null || !Objects.equals(item.getId(), excludedId))
                .ifPresent(item -> {
                    throw new BusinessException("字典项键已存在");
                });
    }

    private void ensureItemCodeUnique(Long dictTypeId, String itemCode, Long excludedId) {
        dictionaryRepository.findItemByCode(dictTypeId, itemCode)
                .filter(item -> excludedId == null || !Objects.equals(item.getId(), excludedId))
                .ifPresent(item -> {
                    throw new BusinessException("字典项存储值已存在");
                });
    }

    private void ensureParentBelongsToType(Long dictTypeId, Long parentId) {
        if (parentId == null) {
            return;
        }
        DictItemDO parent = requireItem(parentId);
        if (!Objects.equals(parent.getDictTypeId(), dictTypeId)) {
            throw new BusinessException("父级字典项不属于当前字典");
        }
    }

    private void migrateItemCode(String dictCode, String oldCode, String newCode) {
        for (DictFieldBindingDO binding : dictionaryRepository.findBindingsByDictCode(dictCode)) {
            dictionaryRepository.migrateBindingReferences(binding, oldCode, newCode);
        }
    }

    private void ensureItemNotReferenced(String dictCode, String itemCode, String message) {
        long referenced = dictionaryRepository.findBindingsByDictCode(dictCode)
                .stream()
                .mapToLong(binding -> dictionaryRepository.countBindingReferences(binding, itemCode))
                .sum();
        if (referenced > 0) {
            throw new BusinessException(message);
        }
    }

    private String normalizeDictCode(String value) {
        String dictCode = trimRequired(value, "字典编码不能为空");
        if (!DICT_CODE_PATTERN.matcher(dictCode).matches()) {
            throw new BusinessException("字典编码格式非法");
        }
        return dictCode;
    }

    private String normalizeItemKey(String value) {
        String itemKey = trimRequired(value, "字典项键不能为空");
        if (!ITEM_KEY_PATTERN.matcher(itemKey).matches()) {
            throw new BusinessException("字典项键格式非法");
        }
        return itemKey;
    }

    private String normalizeStatusNullable(String value) {
        String status = trimNullable(value);
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return normalizeStatus(status, null);
    }

    private String normalizeStatus(String value, String defaultStatus) {
        String status = trimNullable(value);
        if (status == null) {
            return defaultStatus;
        }
        if (STATUS_ENABLED.equalsIgnoreCase(status)) {
            return STATUS_ENABLED;
        }
        if (STATUS_DISABLED.equalsIgnoreCase(status)) {
            return STATUS_DISABLED;
        }
        throw new BusinessException("状态仅支持 ENABLED 或 DISABLED");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = trimNullable(value);
        return normalized == null ? defaultValue : normalized;
    }

    private Integer defaultSortNo(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException(message);
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

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    /**
     * 字典类型请求。
     *
     * @param dictCode 字典编码
     * @param dictName 字典名称
     * @param category 分类
     * @param status 状态
     * @param sortNo 排序号
     * @param remark 备注
     */
    public record DictionaryTypeRequest(String dictCode,
                                        String dictName,
                                        String category,
                                        String status,
                                        Integer sortNo,
                                        String remark) {
    }

    /**
     * 字典项请求。
     *
     * @param parentId 父级字典项 ID
     * @param itemKey 稳定键
     * @param itemCode 存储值
     * @param itemLabel 展示文案
     * @param status 状态
     * @param sortNo 排序号
     * @param extraJson 扩展 JSON
     * @param remark 备注
     */
    public record DictionaryItemRequest(Long parentId,
                                        String itemKey,
                                        String itemCode,
                                        String itemLabel,
                                        String status,
                                        Integer sortNo,
                                        String extraJson,
                                        String remark) {
    }

    /**
     * 字典类型视图。
     *
     * @param id 主键
     * @param dictCode 字典编码
     * @param dictName 字典名称
     * @param category 分类
     * @param status 状态
     * @param builtin 是否内置
     * @param sortNo 排序号
     * @param remark 备注
     */
    public record DictionaryTypeView(Long id,
                                     String dictCode,
                                     String dictName,
                                     String category,
                                     String status,
                                     boolean builtin,
                                     Integer sortNo,
                                     String remark) {
    }

    /**
     * 字典项视图。
     *
     * @param id 主键
     * @param dictTypeId 字典类型 ID
     * @param parentId 父级字典项 ID
     * @param itemKey 稳定键
     * @param itemCode 存储值
     * @param itemLabel 展示文案
     * @param status 状态
     * @param builtin 是否内置
     * @param sortNo 排序号
     * @param extraJson 扩展 JSON
     * @param remark 备注
     */
    public record DictionaryItemView(Long id,
                                     Long dictTypeId,
                                     Long parentId,
                                     String itemKey,
                                     String itemCode,
                                     String itemLabel,
                                     String status,
                                     boolean builtin,
                                     Integer sortNo,
                                     String extraJson,
                                     String remark) {

        static DictionaryItemView from(DictItemDO item) {
            return new DictionaryItemView(
                    item.getId(),
                    item.getDictTypeId(),
                    item.getParentId(),
                    item.getItemKey(),
                    item.getItemCode(),
                    item.getItemLabel(),
                    item.getStatus(),
                    Boolean.TRUE.equals(item.getBuiltin()),
                    item.getSortNo(),
                    item.getExtraJson(),
                    item.getRemark()
            );
        }
    }
}
