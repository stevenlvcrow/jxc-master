package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;

import java.util.Optional;

public interface StoreAdminRelRepository {

    Long countByStoreId(Long storeId);

    Optional<StoreAdminRelDO> findByStoreId(Long storeId);

    Optional<StoreAdminRelDO> findByUserId(Long userId);

    void save(StoreAdminRelDO rel);

    void update(StoreAdminRelDO rel);

    void deleteByUserId(Long userId);
}
