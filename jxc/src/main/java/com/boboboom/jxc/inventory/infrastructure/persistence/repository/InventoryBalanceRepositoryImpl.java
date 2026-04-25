package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryBalanceMapper;

/** 库存仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class InventoryBalanceRepositoryImpl implements InventoryBalanceRepository {

    private final InventoryBalanceMapper inventoryBalanceMapper;

    /** 库存仓储实现，负责通过持久层组件完成数据读写。 */
    public InventoryBalanceRepositoryImpl(InventoryBalanceMapper inventoryBalanceMapperValue) {
        this.inventoryBalanceMapper = inventoryBalanceMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<InventoryBalanceDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return inventoryBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBalanceDO>()
                .eq(InventoryBalanceDO::getScopeType, scopeType)
                .eq(InventoryBalanceDO::getScopeId, scopeId)
                .orderByAsc(InventoryBalanceDO::getWarehouseName)
                .orderByAsc(InventoryBalanceDO::getItemCode));
    }

    /** 库存明细项模型，承载子表或批量操作明细。 */
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

    /** 库存明细项模型，承载子表或批量操作明细。 */
    @Override
    public Optional<InventoryBalanceDO> lockByScopeWarehouseAndItem(String scopeType, Long scopeId, String warehouseName, String itemCode) {
        return Optional.ofNullable(inventoryBalanceMapper.selectForUpdate(scopeType, scopeId, warehouseName, itemCode));
    }

    /** 保存业务数据。 */
    @Override
    public void save(InventoryBalanceDO balance) {
        inventoryBalanceMapper.insert(balance);
    }

    /** 更新业务记录。 */
    @Override
    public void update(InventoryBalanceDO balance) {
        inventoryBalanceMapper.updateById(balance);
    }
}
