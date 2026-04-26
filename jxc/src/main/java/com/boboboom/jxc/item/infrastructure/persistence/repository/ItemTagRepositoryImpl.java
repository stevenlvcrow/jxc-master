package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.ItemTagRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.ItemTagMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class ItemTagRepositoryImpl implements ItemTagRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final ItemTagMapper itemTagMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public ItemTagRepositoryImpl(ItemTagMapper itemTagMapperValue) {
        this.itemTagMapper = itemTagMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<ItemTagDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .orderByDesc(ItemTagDO::getUpdatedAt)
                .orderByDesc(ItemTagDO::getId));
    }

    /** 查询PlatformTemplates。 */
    @Override
    public List<ItemTagDO> findPlatformTemplates() {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, PLATFORM_SCOPE)
                .eq(ItemTagDO::getScopeId, 0L)
                .orderByAsc(ItemTagDO::getCreatedAt)
                .orderByAsc(ItemTagDO::getId));
    }

    /** 查询门店Rows。 */
    @Override
    public List<ItemTagDO> findStoreRows(Long storeId) {
        return itemTagMapper.selectList(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, STORE_SCOPE)
                .eq(ItemTagDO::getScopeId, storeId)
                .select(ItemTagDO::getTagCode, ItemTagDO::getTagName));
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<ItemTagDO> findById(Long id) {
        return Optional.ofNullable(itemTagMapper.selectById(id));
    }

    /** 统计By作用域And标签编码数量。 */
    @Override
    public Long countByScopeAndTagCode(String scopeType, Long scopeId, String tagCode) {
        return itemTagMapper.selectCount(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .eq(ItemTagDO::getTagCode, tagCode));
    }

    /** 统计By作用域And标签名称数量。 */
    @Override
    public Long countByScopeAndTagName(String scopeType, Long scopeId, String tagName) {
        return itemTagMapper.selectCount(new LambdaQueryWrapper<ItemTagDO>()
                .eq(ItemTagDO::getScopeType, scopeType)
                .eq(ItemTagDO::getScopeId, scopeId)
                .eq(ItemTagDO::getTagName, tagName));
    }

    /** 保存业务数据。 */
    @Override
    public void save(ItemTagDO itemTag) {
        itemTagMapper.insert(itemTag);
    }

    /** 更新业务记录。 */
    @Override
    public void update(ItemTagDO itemTag) {
        itemTagMapper.updateById(itemTag);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        itemTagMapper.deleteById(id);
    }
}
