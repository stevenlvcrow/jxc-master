package com.boboboom.jxc.item.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface SupplierCategoryRepository {

    List<SupplierCategoryDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<SupplierCategoryDO> findByScopeAndName(String scopeType, Long scopeId, String categoryName);

    Optional<SupplierCategoryDO> findByScopeAndCode(String scopeType, Long scopeId, String categoryCode);

    void save(SupplierCategoryDO category);
}
