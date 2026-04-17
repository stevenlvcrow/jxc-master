package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemStatisticsTypeRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemStatisticsTypeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ItemStatisticsTypeRepositoryImpl implements ItemStatisticsTypeRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemStatisticsTypeMapper itemStatisticsTypeMapper;

    public ItemStatisticsTypeRepositoryImpl(ItemStatisticsTypeMapper itemStatisticsTypeMapper) {
        this.itemStatisticsTypeMapper = itemStatisticsTypeMapper;
    }

    @Override
    public List<ItemStatisticsTypeDO> findPlatformTemplates() {
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemStatisticsTypeDO::getScopeId, 0L)
                .orderByAsc(ItemStatisticsTypeDO::getCreatedAt)
                .orderByAsc(ItemStatisticsTypeDO::getId));
    }

    @Override
    public List<ItemStatisticsTypeDO> findStoreRows(Long storeId) {
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, STORE_SCOPE)
                .eq(ItemStatisticsTypeDO::getScopeId, storeId)
                .select(ItemStatisticsTypeDO::getCode, ItemStatisticsTypeDO::getName, ItemStatisticsTypeDO::getStatisticsCategory));
    }

    @Override
    public void save(ItemStatisticsTypeDO itemStatisticsType) {
        itemStatisticsTypeMapper.insert(itemStatisticsType);
    }
}
