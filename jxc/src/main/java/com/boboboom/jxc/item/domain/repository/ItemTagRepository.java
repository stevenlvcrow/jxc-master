package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;

import java.util.List;
import java.util.Optional;

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
