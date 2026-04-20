package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryBalanceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InventoryBalanceRepositoryImpl implements InventoryBalanceRepository {

    private final InventoryBalanceMapper inventoryBalanceMapper;

    public InventoryBalanceRepositoryImpl(InventoryBalanceMapper inventoryBalanceMapper) {
        this.inventoryBalanceMapper = inventoryBalanceMapper;
    }

    @Override
    public List<InventoryBalanceDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return inventoryBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBalanceDO>()
                .eq(InventoryBalanceDO::getScopeType, scopeType)
                .eq(InventoryBalanceDO::getScopeId, scopeId)
                .orderByAsc(InventoryBalanceDO::getWarehouseName)
                .orderByAsc(InventoryBalanceDO::getItemCode));
    }

    @Override
    public Optional<InventoryBalanceDO> findByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode) {
        return inventoryBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBalanceDO>()
                .eq(InventoryBalanceDO::getScopeType, scopeType)
                .eq(InventoryBalanceDO::getScopeId, scopeId)
                .eq(InventoryBalanceDO::getWarehouseName, warehouseName)
                .eq(InventoryBalanceDO::getItemCode, itemCode)
                .orderByDesc(InventoryBalanceDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<InventoryBalanceDO> lockByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode) {
        return Optional.ofNullable(inventoryBalanceMapper.selectForUpdate(scopeType, scopeId, warehouseName, itemCode));
    }

    @Override
    public void save(InventoryBalanceDO balance) {
        inventoryBalanceMapper.insert(balance);
    }

    @Override
    public void update(InventoryBalanceDO balance) {
        inventoryBalanceMapper.updateById(balance);
    }
}
