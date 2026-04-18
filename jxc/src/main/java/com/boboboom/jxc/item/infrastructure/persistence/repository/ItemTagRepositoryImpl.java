package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemTagRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemTagMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ItemTagRepositoryImpl implements ItemTagRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemTagMapper itemTagMapper;

    public ItemTagRepositoryImpl(ItemTagMapper itemTagMapper) {
        this.itemTagMapper = itemTagMapper;
    }

    @Override
    public List<ItemTagDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .orderByDesc(ItemTagDO::getUpdatedAt)
                .orderByDesc(ItemTagDO::getId));
    }

    @Override
    public List<ItemTagDO> findPlatformTemplates() {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemTagDO::getScopeId, 0L)
                .orderByAsc(ItemTagDO::getCreatedAt)
                .orderByAsc(ItemTagDO::getId));
    }

    @Override
    public List<ItemTagDO> findStoreRows(Long storeId) {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, STORE_SCOPE)
                .eq(ItemTagDO::getScopeId, storeId)
                .select(ItemTagDO::getTagCode, ItemTagDO::getTagName));
    }

    @Override
    public Optional<ItemTagDO> findById(Long id) {
        return Optional.ofNullable(itemTagMapper.selectById(id));
    }

    @Override
    public Long countByScopeAndTagCode(String scopeType, Long scopeId, String tagCode) {
        return itemTagMapper.selectCount(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .eq(ItemTagDO::getTagCode, tagCode));
    }

    @Override
    public Long countByScopeAndTagName(String scopeType, Long scopeId, String tagName) {
        return itemTagMapper.selectCount(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .eq(ItemTagDO::getTagName, tagName));
    }

    @Override
    public void save(ItemTagDO itemTag) {
        itemTagMapper.insert(itemTag);
    }

    @Override
    public void update(ItemTagDO itemTag) {
        itemTagMapper.updateById(itemTag);
    }

    @Override
    public void deleteById(Long id) {
        itemTagMapper.deleteById(id);
    }
}
