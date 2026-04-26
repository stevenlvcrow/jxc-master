package com.boboboom.jxc.inventory.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchBalanceDO;

/** 库存批次余额仓储接口。 */
public interface InventoryBatchBalanceRepository {

    List<InventoryBatchBalanceDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<InventoryBatchBalanceDO> findByScopeWarehouseAndItemOrdered(String scopeType,
                                                                     Long scopeId,
                                                                     String warehouseName,
                                                                     String itemCode);

    List<InventoryBatchBalanceDO> findByScopeAndExpiryDateOnOrBefore(String scopeType, Long scopeId, LocalDate expiryDate);

    Optional<InventoryBatchBalanceDO> lockByScopeWarehouseItemAndBatch(String scopeType,
                                                                       Long scopeId,
                                                                       String warehouseName,
                                                                       String itemCode,
                                                                       String batchNo);

    void save(InventoryBatchBalanceDO balance);

    void update(InventoryBatchBalanceDO balance);
}
