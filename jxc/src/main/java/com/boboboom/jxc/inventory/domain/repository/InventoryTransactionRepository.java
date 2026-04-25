package com.boboboom.jxc.inventory.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;

/** 库存仓储接口，定义领域需要的数据访问能力。 */
public interface InventoryTransactionRepository {

    void save(InventoryTransactionDO transaction);

    List<InventoryTransactionDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<InventoryTransactionDO> findByScopeAndCreatedAtRange(String scopeType,
                                                              Long scopeId,
                                                              LocalDateTime startTime,
                                                              LocalDateTime endTime);

    List<InventoryBalanceDO> findLatestBalancesBefore(String scopeType,
                                                      Long scopeId,
                                                      String warehouseName,
                                                      LocalDateTime cutoff);
}
