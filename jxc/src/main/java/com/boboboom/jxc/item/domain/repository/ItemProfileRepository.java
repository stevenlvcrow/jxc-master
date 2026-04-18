package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;

import java.util.List;
import java.util.Optional;

public interface ItemProfileRepository {

    List<ItemProfileDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<ItemProfileDO> findByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds);

    Optional<ItemProfileDO> findByItemId(String itemId);

    void save(ItemProfileDO itemProfile);

    void update(ItemProfileDO itemProfile);

    void deleteByScopeAndItemIds(String scopeType, Long scopeId, List<String> itemIds);
}
