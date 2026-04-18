package com.boboboom.jxc.identity.domain.repository;

public interface StoreAdminRelRepository {

    Long countByStoreId(Long storeId);

    void deleteByUserId(Long userId);
}
