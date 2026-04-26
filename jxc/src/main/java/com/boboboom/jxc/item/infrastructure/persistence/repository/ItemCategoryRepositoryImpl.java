package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemCategoryMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class ItemCategoryRepositoryImpl implements ItemCategoryRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemCategoryMapper itemCategoryMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public ItemCategoryRepositoryImpl(ItemCategoryMapper itemCategoryMapperValue) {
        this.itemCategoryMapper = itemCategoryMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<ItemCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, scopeType)
                .eq(ItemCategoryDO::getScopeId, scopeId)
                .orderByAsc(ItemCategoryDO::getCreatedAt)
                .orderByAsc(ItemCategoryDO::getId));
    }

    /** 查询PlatformTemplates。 */
    @Override
    public List<ItemCategoryDO> findPlatformTemplates() {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemCategoryDO::getScopeId, 0L)
                .orderByAsc(ItemCategoryDO::getCreatedAt)
                .orderByAsc(ItemCategoryDO::getId));
    }

    /** 查询门店Rows。 */
    @Override
    public List<ItemCategoryDO> findStoreRows(Long storeId) {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategoryDO>()
                .eq(ItemCategoryDO::getScopeType, STORE_SCOPE)
                .eq(ItemCategoryDO::getScopeId, storeId)
                .select(ItemCategoryDO::getCategoryCode, ItemCategoryDO::getCategoryName));
    }

    /** 保存业务数据。 */
    @Override
    public void save(ItemCategoryDO itemCategory) {
        itemCategoryMapper.insert(itemCategory);
    }

    /** 更新业务记录。 */
    @Override
    public void update(ItemCategoryDO itemCategory) {
        itemCategoryMapper.updateById(itemCategory);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        itemCategoryMapper.deleteById(id);
    }
}
