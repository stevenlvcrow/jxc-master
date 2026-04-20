package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;

import java.util.List;
import java.util.Optional;

public interface SupplierCategoryRepository {

    List<SupplierCategoryDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<SupplierCategoryDO> findByScopeAndName(String scopeType, Long scopeId, String categoryName);

    Optional<SupplierCategoryDO> findByScopeAndCode(String scopeType, Long scopeId, String categoryCode);

    void save(SupplierCategoryDO category);
}
