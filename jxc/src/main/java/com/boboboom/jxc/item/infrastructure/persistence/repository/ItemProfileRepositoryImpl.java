package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemProfileMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class ItemProfileRepositoryImpl implements ItemProfileRepository {

    private final ItemProfileMapper itemProfileMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public ItemProfileRepositoryImpl(ItemProfileMapper itemProfileMapperValue) {
        this.itemProfileMapper = itemProfileMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<ItemProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemProfileMapper.selectList(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getScopeType, scopeType)
                .eq(ItemProfileDO::getScopeId, scopeId)
                .orderByDesc(ItemProfileDO::getCreatedAt)
                .orderByDesc(ItemProfileDO::getId));
    }

    /** 查询By作用域And物品标识。 */
    @Override
    public List<ItemProfileDO> findByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return itemProfileMapper.selectList(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getScopeType, scopeType)
                .eq(ItemProfileDO::getScopeId, scopeId)
                .eq(ItemProfileDO::getDraft, Boolean.FALSE)
                .in(ItemProfileDO::getItemId, itemIds));
    }

    /** 查询By物品标识。 */
    @Override
    public Optional<ItemProfileDO> findByItemId(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return itemProfileMapper.selectList(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getItemId, itemId)
                .orderByDesc(ItemProfileDO::getId))
                .stream()
                .findFirst();
    }

    /** 保存业务数据。 */
    @Override
    public void save(ItemProfileDO itemProfile) {
        itemProfileMapper.insert(itemProfile);
    }

    /** 更新业务记录。 */
    @Override
    public void update(ItemProfileDO itemProfile) {
        itemProfileMapper.updateById(itemProfile);
    }

    /** 删除By作用域And物品标识。 */
    @Override
    public void deleteByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        itemProfileMapper.delete(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getScopeType, scopeType)
                .eq(ItemProfileDO::getScopeId, scopeId)
                .eq(ItemProfileDO::getDraft, Boolean.FALSE)
                .in(ItemProfileDO::getItemId, itemIds));
    }
}
