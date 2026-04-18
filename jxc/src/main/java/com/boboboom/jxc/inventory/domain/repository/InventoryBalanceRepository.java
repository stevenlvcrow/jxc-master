package com.boboboom.jxc.inventory.domain.repository;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository {

    List<InventoryBalanceDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<InventoryBalanceDO> findByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode);

    void save(InventoryBalanceDO balance);

    void update(InventoryBalanceDO balance);
}
