package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;

import java.util.Optional;

public interface WarehouseRepository {

    Optional<WarehouseDO> findById(Long id);
}
