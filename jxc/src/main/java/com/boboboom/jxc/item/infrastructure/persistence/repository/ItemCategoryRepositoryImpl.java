package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ItemCategoryRepositoryImpl implements ItemCategoryRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemCategoryMapper itemCategoryMapper;

    public ItemCategoryRepositoryImpl(ItemCategoryMapper itemCategoryMapper) {
        this.itemCategoryMapper = itemCategoryMapper;
    }

    @Override
    public List<ItemCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, scopeType)
                .eq(ItemCategoryDO::getScopeId, scopeId)
                .orderByAsc(ItemCategoryDO::getCreatedAt)
                .orderByAsc(ItemCategoryDO::getId));
    }

    @Override
    public List<ItemCategoryDO> findPlatformTemplates() {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemCategoryDO::getScopeId, 0L)
                .orderByAsc(ItemCategoryDO::getCreatedAt)
                .orderByAsc(ItemCategoryDO::getId));
    }

    @Override
    public List<ItemCategoryDO> findStoreRows(Long storeId) {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, STORE_SCOPE)
                .eq(ItemCategoryDO::getScopeId, storeId)
                .select(ItemCategoryDO::getCategoryCode, ItemCategoryDO::getCategoryName));
    }

    @Override
    public void save(ItemCategoryDO itemCategory) {
        itemCategoryMapper.insert(itemCategory);
    }

    @Override
    public void update(ItemCategoryDO itemCategory) {
        itemCategoryMapper.updateById(itemCategory);
    }

    @Override
    public void deleteById(Long id) {
        itemCategoryMapper.deleteById(id);
    }
}
