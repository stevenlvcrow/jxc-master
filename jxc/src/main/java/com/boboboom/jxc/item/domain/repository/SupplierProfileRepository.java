package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;

import java.util.List;
import java.util.Optional;

public interface SupplierProfileRepository {

    List<SupplierProfileDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<SupplierProfileDO> findById(Long id);

    void save(SupplierProfileDO profile);

    void update(SupplierProfileDO profile);
}
