package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.DictionaryRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictTypeDO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 字典查询与校验服务，供业务服务统一校验可配置状态和下拉值。
 */
@Service
public class DictionaryLookupService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final DictionaryRepository dictionaryRepository;

    /**
     * 构造字典查询服务。
     *
     * @param dictionaryRepository 字典仓储
     */
    public DictionaryLookupService(DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    /**
     * 校验字典存储值必须存在且启用。
     *
     * @param dictCode 字典编码
     * @param itemCode 字典存储值
     * @return 规范化后的字典存储值
     */
    public String requireEnabledCode(String dictCode, String itemCode) {
        String normalizedCode = trimRequired(itemCode, "字典值不能为空");
        DictTypeDO type = requireEnabledType(dictCode);
        DictItemDO item = dictionaryRepository.findItemByCode(type.getId(), normalizedCode)
                .orElseThrow(() -> new BusinessException("字典值不存在或已停用"));
        if (!STATUS_ENABLED.equals(item.getStatus())) {
            throw new BusinessException("字典值不存在或已停用");
        }
        return item.getItemCode();
    }

    /**
     * 根据稳定项键获取当前可配置存储值。
     *
     * @param dictCode 字典编码
     * @param itemKey 稳定项键
     * @return 当前存储值
     */
    public String codeOf(String dictCode, String itemKey) {
        String normalizedKey = trimRequired(itemKey, "字典项键不能为空");
        DictTypeDO type = requireType(dictCode);
        DictItemDO item = dictionaryRepository.findItemByKey(type.getId(), normalizedKey)
                .orElseThrow(() -> new BusinessException("字典项不存在：" + dictCode + "/" + itemKey));
        return item.getItemCode();
    }

    /**
     * 判断实际存储值是否等于稳定项键对应的当前存储值。
     *
     * @param dictCode 字典编码
     * @param itemKey 稳定项键
     * @param actualCode 实际存储值
     * @return 是否匹配
     */
    public boolean isCode(String dictCode, String itemKey, String actualCode) {
        return Objects.equals(codeOf(dictCode, itemKey), actualCode);
    }

    /**
     * 查询指定字典的启用项。
     *
     * @param dictCode 字典编码
     * @return 字典项选项
     */
    public List<DictionaryItemOption> itemsOf(String dictCode) {
        DictTypeDO type = requireEnabledType(dictCode);
        return dictionaryRepository.findItemsByTypeId(type.getId(), true)
                .stream()
                .map(DictionaryItemOption::from)
                .toList();
    }

    private DictTypeDO requireEnabledType(String dictCode) {
        DictTypeDO type = requireType(dictCode);
        if (!STATUS_ENABLED.equals(type.getStatus())) {
            throw new BusinessException("字典未启用：" + dictCode);
        }
        return type;
    }

    private DictTypeDO requireType(String dictCode) {
        String normalizedCode = trimRequired(dictCode, "字典编码不能为空");
        return dictionaryRepository.findTypeByCode(normalizedCode)
                .orElseThrow(() -> new BusinessException("字典不存在：" + normalizedCode));
    }

    private String trimRequired(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    /**
     * 字典项业务选项。
     *
     * @param id 字典项主键
     * @param parentId 父级字典项主键
     * @param itemKey 稳定项键
     * @param itemCode 存储值
     * @param itemLabel 展示文本
     * @param sortNo 排序号
     * @param extraJson 扩展 JSON
     */
    public record DictionaryItemOption(Long id,
                                       Long parentId,
                                       String itemKey,
                                       String itemCode,
                                       String itemLabel,
                                       Integer sortNo,
                                       String extraJson) {

        static DictionaryItemOption from(DictItemDO item) {
            return new DictionaryItemOption(
                    item.getId(),
                    item.getParentId(),
                    item.getItemKey(),
                    item.getItemCode(),
                    item.getItemLabel(),
                    item.getSortNo(),
                    item.getExtraJson()
            );
        }
    }
}
