package com.boboboom.jxc.inventory.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;

/** 库存仓储接口，定义领域需要的数据访问能力。 */
public interface InventoryBalanceRepository {

    List<InventoryBalanceDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<InventoryBalanceDO> findByScopeAndWarehouseOrdered(String scopeType, Long scopeId, String warehouseName);

    Optional<InventoryBalanceDO> findByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode);

    Optional<InventoryBalanceDO> lockByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode);

    void save(InventoryBalanceDO balance);

    void update(InventoryBalanceDO balance);
}
