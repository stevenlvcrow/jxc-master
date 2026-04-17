package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;

import java.util.List;

public interface ItemTagRepository {

    List<ItemTagDO> findPlatformTemplates();

    List<ItemTagDO> findStoreRows(Long storeId);

    void save(ItemTagDO itemTag);
}
