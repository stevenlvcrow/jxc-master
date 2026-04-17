package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemTagRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemTagMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ItemTagRepositoryImpl implements ItemTagRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemTagMapper itemTagMapper;

    public ItemTagRepositoryImpl(ItemTagMapper itemTagMapper) {
        this.itemTagMapper = itemTagMapper;
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
    public void save(ItemTagDO itemTag) {
        itemTagMapper.insert(itemTag);
    }
}
