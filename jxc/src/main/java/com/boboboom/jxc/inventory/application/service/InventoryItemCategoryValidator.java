package com.boboboom.jxc.inventory.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 库存单据物品类别校验组件，防止单据行类别与档案类别不一致。 */
@Component
public class InventoryItemCategoryValidator {

    private final ItemProfileRepository itemProfileRepository;
    private final ObjectMapper objectMapper;

    /** 库存单据物品类别校验组件，依赖物品档案仓储和 JSON 解析器。 */
    public InventoryItemCategoryValidator(ItemProfileRepository itemProfileRepositoryValue,
                                          ObjectMapper objectMapperValue) {
        this.itemProfileRepository = itemProfileRepositoryValue;
        this.objectMapper = objectMapperValue;
    }

    /** 校验当前作用域内所有行的物品编码和类别必须与物品档案一致。 */
    public void validateLines(String scopeType, Long scopeId, List<LineCategory> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("单据明细不能为空");
        }
        List<String> itemCodes = lines.stream()
                .map(LineCategory::itemCode)
                .map(this::trimNullable)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, String> categoryByItemCode = loadCategoryMap(scopeType, scopeId, itemCodes);
        for (LineCategory line : lines) {
            validateLine(line, categoryByItemCode);
        }
    }

    private Map<String, String> loadCategoryMap(String scopeType, Long scopeId, List<String> itemCodes) {
        List<ItemProfileDO> profiles = itemProfileRepository.findByScopeAndItemCodes(scopeType, scopeId, itemCodes);
        Map<String, String> categoryByItemCode = new LinkedHashMap<>();
        for (ItemProfileDO profile : profiles) {
            String itemCode = trimNullable(profile.getItemCode());
            if (!StringUtils.hasText(itemCode)) {
                continue;
            }
            if (categoryByItemCode.containsKey(itemCode)) {
                throw new BusinessException("物品编码重复，请清理脏数据: " + itemCode);
            }
            categoryByItemCode.put(itemCode, parseProfileCategory(profile));
        }
        return categoryByItemCode;
    }

    private void validateLine(LineCategory line, Map<String, String> categoryByItemCode) {
        String itemCode = requiredTrim(line.itemCode(), "物品编码不能为空");
        String lineCategory = requiredTrim(line.category(), "物品类别不能为空");
        String profileCategory = categoryByItemCode.get(itemCode);
        if (!StringUtils.hasText(profileCategory)) {
            throw new BusinessException("物品不存在或档案类别为空: " + itemCode);
        }
        if (!Objects.equals(lineCategory, profileCategory)) {
            throw new BusinessException("物品类别与档案不一致: " + itemCode);
        }
    }

    private String parseProfileCategory(ItemProfileDO profile) {
        if (!StringUtils.hasText(profile.getDetailJson())) {
            throw new BusinessException("物品详情数据为空: " + defaultIfBlank(profile.getItemCode(), ""));
        }
        try {
            ItemCreateRequest request = objectMapper.readValue(profile.getDetailJson(), ItemCreateRequest.class);
            return requiredTrim(request.category(), "物品档案类别为空: " + defaultIfBlank(profile.getItemCode(), ""));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("物品详情数据已损坏: " + defaultIfBlank(profile.getItemCode(), ""));
        }
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    /** 待校验单据行的物品编码和类别。 */
    public record LineCategory(String itemCode, String category) {
    }
}
