package com.boboboom.jxc.inventory.domain.repository;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;

public interface InventoryTransactionRepository {

    void save(InventoryTransactionDO transaction);
}
