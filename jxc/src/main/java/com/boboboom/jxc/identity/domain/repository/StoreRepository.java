package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;

import java.util.List;
import java.util.Optional;

public interface StoreRepository {

    Optional<StoreDO> findById(Long id);

    Optional<StoreDO> findByStoreCode(String storeCode);

    List<StoreDO> findByGroupId(Long groupId);

    List<StoreDO> findByGroupIds(List<Long> groupIds);

    Long countByGroupId(Long groupId);

    void save(StoreDO store);

    void update(StoreDO store);

    void deleteById(Long id);
}
