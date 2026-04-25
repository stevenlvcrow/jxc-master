package com.boboboom.jxc.item.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface SupplierProfileRepository {

    List<SupplierProfileDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<SupplierProfileDO> findById(Long id);

    void save(SupplierProfileDO profile);

    void update(SupplierProfileDO profile);
}
