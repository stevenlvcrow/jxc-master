package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryTransactionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryTransactionRepositoryImpl implements InventoryTransactionRepository {

    private final InventoryTransactionMapper inventoryTransactionMapper;

    public InventoryTransactionRepositoryImpl(InventoryTransactionMapper inventoryTransactionMapper) {
        this.inventoryTransactionMapper = inventoryTransactionMapper;
    }

    @Override
    public void save(InventoryTransactionDO transaction) {
        inventoryTransactionMapper.insert(transaction);
    }
}
