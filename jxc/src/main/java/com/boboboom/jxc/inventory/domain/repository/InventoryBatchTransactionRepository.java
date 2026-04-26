package com.boboboom.jxc.inventory.domain.repository;

import java.time.LocalDate;
import java.util.List;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchTransactionDO;

/** 库存批次流水仓储接口。 */
public interface InventoryBatchTransactionRepository {

    void save(InventoryBatchTransactionDO transaction);

    List<InventoryBatchTransactionDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<InventoryBatchTransactionDO> findByScopeAndBusinessDateRange(String scopeType,
                                                                      Long scopeId,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate);
}
