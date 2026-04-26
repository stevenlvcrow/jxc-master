package com.boboboom.jxc.inventory.domain.repository;

import java.util.List;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryStockLockDO;

/** 库存锁库仓储接口，定义盘点锁库需要的数据访问能力。 */
public interface InventoryStockLockRepository {

    List<InventoryStockLockDO> findActiveByScopeWarehouseAndItems(String scopeType,
                                                                  Long scopeId,
                                                                  String warehouseName,
                                                                  List<String> itemCodes);

    List<InventoryStockLockDO> findActiveBySource(String sourceType, Long sourceId);

    void save(InventoryStockLockDO stockLock);

    void update(InventoryStockLockDO stockLock);
}
