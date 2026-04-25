package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemStatisticsTypeRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemStatisticsTypeMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class ItemStatisticsTypeRepositoryImpl implements ItemStatisticsTypeRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemStatisticsTypeMapper itemStatisticsTypeMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public ItemStatisticsTypeRepositoryImpl(ItemStatisticsTypeMapper itemStatisticsTypeMapperValue) {
        this.itemStatisticsTypeMapper = itemStatisticsTypeMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<ItemStatisticsTypeDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, scopeType)
                .eq(ItemStatisticsTypeDO::getScopeId, scopeId)
                .orderByDesc(ItemStatisticsTypeDO::getCode)
                .orderByDesc(ItemStatisticsTypeDO::getId));
    }

    /** 查询By作用域And标识。 */
    @Override
    public List<ItemStatisticsTypeDO> findByScopeAndIds(String scopeType, Long scopeId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, scopeType)
                .eq(ItemStatisticsTypeDO::getScopeId, scopeId)
                .in(ItemStatisticsTypeDO::getId, ids));
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<ItemStatisticsTypeDO> findById(Long id) {
        return Optional.ofNullable(itemStatisticsTypeMapper.selectById(id));
    }

    /** 查询PlatformTemplates。 */
    @Override
    public List<ItemStatisticsTypeDO> findPlatformTemplates() {
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemStatisticsTypeDO::getScopeId, 0L)
                .orderByAsc(ItemStatisticsTypeDO::getCreatedAt)
                .orderByAsc(ItemStatisticsTypeDO::getId));
    }

    /** 查询门店Rows。 */
    @Override
    public List<ItemStatisticsTypeDO> findStoreRows(Long storeId) {
        return itemStatisticsTypeMapper.selectList(new LambdaQueryWrapper<ItemStatisticsTypeDO>()
                .eq(ItemStatisticsTypeDO::getScopeType, STORE_SCOPE)
                .eq(ItemStatisticsTypeDO::getScopeId, storeId)
                .select(ItemStatisticsTypeDO::getCode, ItemStatisticsTypeDO::getName, ItemStatisticsTypeDO::getStatisticsCategory));
    }

    /** 保存业务数据。 */
    @Override
    public void save(ItemStatisticsTypeDO itemStatisticsType) {
        itemStatisticsTypeMapper.insert(itemStatisticsType);
    }

    /** 更新业务记录。 */
    @Override
    public void update(ItemStatisticsTypeDO itemStatisticsType) {
        itemStatisticsTypeMapper.updateById(itemStatisticsType);
    }
}
