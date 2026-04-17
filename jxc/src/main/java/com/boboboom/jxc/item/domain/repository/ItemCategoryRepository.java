package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;

import java.util.List;

public interface ItemCategoryRepository {

    List<ItemCategoryDO> findPlatformTemplates();

    List<ItemCategoryDO> findStoreRows(Long storeId);

    void save(ItemCategoryDO itemCategory);
}
