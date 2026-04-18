package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;

import java.util.List;
import java.util.Optional;

public interface ItemStatisticsTypeRepository {

    List<ItemStatisticsTypeDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<ItemStatisticsTypeDO> findByScopeAndIds(String scopeType, Long scopeId, List<Long> ids);

    Optional<ItemStatisticsTypeDO> findById(Long id);

    List<ItemStatisticsTypeDO> findPlatformTemplates();

    List<ItemStatisticsTypeDO> findStoreRows(Long storeId);

    void save(ItemStatisticsTypeDO itemStatisticsType);

    void update(ItemStatisticsTypeDO itemStatisticsType);
}
