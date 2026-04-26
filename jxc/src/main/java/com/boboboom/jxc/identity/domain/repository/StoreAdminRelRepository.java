package com.boboboom.jxc.identity.domain.repository;

import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public interface StoreAdminRelRepository {

    Long countByStoreId(Long storeId);

    Optional<StoreAdminRelDO> findByStoreId(Long storeId);

    Optional<StoreAdminRelDO> findByUserId(Long userId);

    void save(StoreAdminRelDO rel);

    void update(StoreAdminRelDO rel);

    void deleteByUserId(Long userId);
}
