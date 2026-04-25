package com.boboboom.jxc.item.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface ItemTagRepository {

    List<ItemTagDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<ItemTagDO> findPlatformTemplates();

    List<ItemTagDO> findStoreRows(Long storeId);

    Optional<ItemTagDO> findById(Long id);

    Long countByScopeAndTagCode(String scopeType, Long scopeId, String tagCode);

    Long countByScopeAndTagName(String scopeType, Long scopeId, String tagName);

    void save(ItemTagDO itemTag);

    void update(ItemTagDO itemTag);

    void deleteById(Long id);
}
