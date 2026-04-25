package com.boboboom.jxc.item.domain.repository;

import java.util.List;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemCategoryDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface ItemCategoryRepository {

    List<ItemCategoryDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<ItemCategoryDO> findPlatformTemplates();

    List<ItemCategoryDO> findStoreRows(Long storeId);

    void save(ItemCategoryDO itemCategory);

    void update(ItemCategoryDO itemCategory);

    void deleteById(Long id);
}
