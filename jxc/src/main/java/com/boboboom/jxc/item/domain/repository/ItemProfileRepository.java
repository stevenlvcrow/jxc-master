package com.boboboom.jxc.item.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface ItemProfileRepository {

    List<ItemProfileDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<ItemProfileDO> findByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds);

    List<ItemProfileDO> findByScopeAndItemCodes(String scopeType, Long scopeId, List<String> itemCodes);

    Optional<ItemProfileDO> findByItemId(String itemId);

    void save(ItemProfileDO itemProfile);

    void update(ItemProfileDO itemProfile);

    void deleteByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds);
}
