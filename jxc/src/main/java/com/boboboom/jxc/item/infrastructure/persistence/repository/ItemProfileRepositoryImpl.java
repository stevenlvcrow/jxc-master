package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemProfileMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class ItemProfileRepositoryImpl implements ItemProfileRepository {

    private final ItemProfileMapper itemProfileMapper;

    public ItemProfileRepositoryImpl(ItemProfileMapper itemProfileMapper) {
        this.itemProfileMapper = itemProfileMapper;
    }

    @Override
    public List<ItemProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemProfileMapper.selectList(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getScopeType, scopeType)
                .eq(ItemProfileDO::getScopeId, scopeId)
                .orderByDesc(ItemProfileDO::getCreatedAt)
                .orderByDesc(ItemProfileDO::getId));
    }

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

    @Override
    public Optional<ItemProfileDO> findByItemId(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemProfileMapper.selectOne(new LambdaQueryWrapper<ItemProfileDO>()
                .eq(ItemProfileDO::getItemId, itemId)
                .last("limit 1")));
    }

    @Override
    public void save(ItemProfileDO itemProfile) {
        itemProfileMapper.insert(itemProfile);
    }

    @Override
    public void update(ItemProfileDO itemProfile) {
        itemProfileMapper.updateById(itemProfile);
    }

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
