package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;

import java.util.List;

public interface ItemStatisticsTypeRepository {

    List<ItemStatisticsTypeDO> findPlatformTemplates();

    List<ItemStatisticsTypeDO> findStoreRows(Long storeId);

    void save(ItemStatisticsTypeDO itemStatisticsType);
}
