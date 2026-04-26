package com.boboboom.jxc.identity.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.identity.domain.repository.UnitRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.item.domain.repository.ItemCategoryRepository;
import com.boboboom.jxc.item.domain.repository.ItemStatisticsTypeRepository;
import com.boboboom.jxc.item.domain.repository.ItemTagRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class StoreSampleDataInitializationService {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final UnitRepository unitRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemStatisticsTypeRepository itemStatisticsTypeRepository;
    private final ItemTagRepository itemTagRepository;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public StoreSampleDataInitializationService(UnitRepository unitRepositoryValue,
                                                ItemCategoryRepository itemCategoryRepositoryValue,
                                                ItemStatisticsTypeRepository itemStatisticsTypeRepositoryValue,
                                                ItemTagRepository itemTagRepositoryValue) {
        this.unitRepository = unitRepositoryValue;
        this.itemCategoryRepository = itemCategoryRepositoryValue;
        this.itemStatisticsTypeRepository = itemStatisticsTypeRepositoryValue;
        this.itemTagRepository = itemTagRepositoryValue;
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public void initializeStoreSampleData(Long storeId) {
        copyUnitSamplesToStore(storeId);
        copyItemCategorySamplesToStore(storeId);
        copyStatisticsTypeSamplesToStore(storeId);
        copyItemTagSamplesToStore(storeId);
    }

    private void copyUnitSamplesToStore(Long storeId) {
        List<UnitDO> templates = unitRepository.findPlatformTemplates();
        if (templates.isEmpty()) {
            return;
        }
        List<UnitDO> existingRows = unitRepository.findStoreRows(storeId);
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (UnitDO row : existingRows) {
            if (row.getUnitCode() != null) {
                existingCodes.add(row.getUnitCode());
            }
            if (row.getUnitName() != null) {
                existingNames.add(row.getUnitName());
            }
        }

        for (UnitDO template : templates) {
            if (existingCodes.contains(template.getUnitCode()) || existingNames.contains(template.getUnitName())) {
                continue;
            }
            UnitDO toInsert = new UnitDO();
            toInsert.setScopeType(STORE_SCOPE);
            toInsert.setScopeId(storeId);
            toInsert.setUnitCode(template.getUnitCode());
            toInsert.setUnitName(template.getUnitName());
            toInsert.setUnitType(template.getUnitType());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            unitRepository.save(toInsert);
            existingCodes.add(template.getUnitCode());
            existingNames.add(template.getUnitName());
        }
    }

    private void copyItemCategorySamplesToStore(Long storeId) {
        List<ItemCategoryDO> templates = itemCategoryRepository.findPlatformTemplates();
        if (templates.isEmpty()) {
            return;
        }
        List<ItemCategoryDO> existingRows = itemCategoryRepository.findStoreRows(storeId);
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (ItemCategoryDO row : existingRows) {
            if (row.getCategoryCode() != null) {
                existingCodes.add(row.getCategoryCode());
            }
            if (row.getCategoryName() != null) {
                existingNames.add(row.getCategoryName());
            }
        }

        for (ItemCategoryDO template : templates) {
            if (existingCodes.contains(template.getCategoryCode()) || existingNames.contains(template.getCategoryName())) {
                continue;
            }
            ItemCategoryDO toInsert = new ItemCategoryDO();
            toInsert.setScopeType(STORE_SCOPE);
            toInsert.setScopeId(storeId);
            toInsert.setCategoryCode(template.getCategoryCode());
            toInsert.setCategoryName(template.getCategoryName());
            toInsert.setParentCategory(template.getParentCategory());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            itemCategoryRepository.save(toInsert);
            existingCodes.add(template.getCategoryCode());
            existingNames.add(template.getCategoryName());
        }
    }

    private void copyStatisticsTypeSamplesToStore(Long storeId) {
        List<ItemStatisticsTypeDO> templates = itemStatisticsTypeRepository.findPlatformTemplates();
        if (templates.isEmpty()) {
            return;
        }
        List<ItemStatisticsTypeDO> existingRows = itemStatisticsTypeRepository.findStoreRows(storeId);
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNameCategoryPairs = new HashSet<>();
        for (ItemStatisticsTypeDO row : existingRows) {
            collectStatisticsTypeExistingKey(row, existingCodes, existingNameCategoryPairs);
        }

        for (ItemStatisticsTypeDO template : templates) {
            String pair = statisticsTypeNameCategoryPair(template.getName(), template.getStatisticsCategory());
            if (statisticsTypeSampleExists(template, existingCodes, existingNameCategoryPairs)) {
                continue;
            }
            ItemStatisticsTypeDO toInsert = new ItemStatisticsTypeDO();
            toInsert.setScopeType(STORE_SCOPE);
            toInsert.setScopeId(storeId);
            toInsert.setCode(template.getCode());
            toInsert.setName(template.getName());
            toInsert.setStatisticsCategory(template.getStatisticsCategory());
            toInsert.setCreateType(template.getCreateType());
            itemStatisticsTypeRepository.save(toInsert);
            existingCodes.add(template.getCode());
            existingNameCategoryPairs.add(pair);
        }
    }

    private void collectStatisticsTypeExistingKey(ItemStatisticsTypeDO row,
                                                  Set<String> existingCodes,
                                                  Set<String> existingNameCategoryPairs) {
        if (row.getCode() != null) {
            existingCodes.add(row.getCode());
        }
        existingNameCategoryPairs.add(statisticsTypeNameCategoryPair(row.getName(), row.getStatisticsCategory()));
    }

    private boolean statisticsTypeSampleExists(ItemStatisticsTypeDO template,
                                               Set<String> existingCodes,
                                               Set<String> existingNameCategoryPairs) {
        return existingCodes.contains(template.getCode())
                || existingNameCategoryPairs.contains(statisticsTypeNameCategoryPair(template.getName(), template.getStatisticsCategory()));
    }

    private String statisticsTypeNameCategoryPair(String name, String statisticsCategory) {
        return (name == null ? "" : name) + "#" + (statisticsCategory == null ? "" : statisticsCategory);
    }

    private void copyItemTagSamplesToStore(Long storeId) {
        List<ItemTagDO> templates = itemTagRepository.findPlatformTemplates();
        if (templates.isEmpty()) {
            return;
        }
        List<ItemTagDO> existingRows = itemTagRepository.findStoreRows(storeId);
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (ItemTagDO row : existingRows) {
            if (row.getTagCode() != null) {
                existingCodes.add(row.getTagCode());
            }
            if (row.getTagName() != null) {
                existingNames.add(row.getTagName());
            }
        }

        for (ItemTagDO template : templates) {
            if (existingCodes.contains(template.getTagCode()) || existingNames.contains(template.getTagName())) {
                continue;
            }
            ItemTagDO toInsert = new ItemTagDO();
            toInsert.setScopeType(STORE_SCOPE);
            toInsert.setScopeId(storeId);
            toInsert.setTagCode(template.getTagCode());
            toInsert.setTagName(template.getTagName());
            toInsert.setStatus(template.getStatus());
            toInsert.setRemark(template.getRemark());
            itemTagRepository.save(toInsert);
            existingCodes.add(template.getTagCode());
            existingNames.add(template.getTagName());
        }
    }
}
