package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.InventoryBatchBalanceRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryBatchBalanceMapper;

/** 库存批次余额仓储实现。 */
@Repository
public class InventoryBatchBalanceRepositoryImpl implements InventoryBatchBalanceRepository {

    private final InventoryBatchBalanceMapper inventoryBatchBalanceMapper;

    /** 构造批次余额仓储。 */
    public InventoryBatchBalanceRepositoryImpl(InventoryBatchBalanceMapper inventoryBatchBalanceMapperValue) {
        this.inventoryBatchBalanceMapper = inventoryBatchBalanceMapperValue;
    }

    /** 按作用域查询批次余额。 */
    @Override
    public List<InventoryBatchBalanceDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return inventoryBatchBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBatchBalanceDO>()
                .eq(InventoryBatchBalanceDO::getScopeType, scopeType)
                .eq(InventoryBatchBalanceDO::getScopeId, scopeId)
                .orderByAsc(InventoryBatchBalanceDO::getWarehouseName)
                .orderByAsc(InventoryBatchBalanceDO::getItemCode)
                .orderByAsc(InventoryBatchBalanceDO::getExpiryDate)
                .orderByAsc(InventoryBatchBalanceDO::getBatchNo));
    }

    /** 按仓库和物品查询批次余额。 */
    @Override
    public List<InventoryBatchBalanceDO> findByScopeWarehouseAndItemOrdered(String scopeType,
                                                                            Long scopeId,
                                                                            String warehouseName,
                                                                            String itemCode) {
        return inventoryBatchBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBatchBalanceDO>()
                .eq(InventoryBatchBalanceDO::getScopeType, scopeType)
                .eq(InventoryBatchBalanceDO::getScopeId, scopeId)
                .eq(InventoryBatchBalanceDO::getWarehouseName, warehouseName)
                .eq(InventoryBatchBalanceDO::getItemCode, itemCode)
                .orderByAsc(InventoryBatchBalanceDO::getExpiryDate)
                .orderByAsc(InventoryBatchBalanceDO::getBatchNo));
    }

    /** 查询指定到期日之前的批次余额。 */
    @Override
    public List<InventoryBatchBalanceDO> findByScopeAndExpiryDateOnOrBefore(String scopeType, Long scopeId, LocalDate expiryDate) {
        return inventoryBatchBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBatchBalanceDO>()
                .eq(InventoryBatchBalanceDO::getScopeType, scopeType)
                .eq(InventoryBatchBalanceDO::getScopeId, scopeId)
                .le(InventoryBatchBalanceDO::getExpiryDate, expiryDate)
                .gt(InventoryBatchBalanceDO::getQuantity, java.math.BigDecimal.ZERO)
                .orderByAsc(InventoryBatchBalanceDO::getExpiryDate)
                .orderByAsc(InventoryBatchBalanceDO::getWarehouseName)
                .orderByAsc(InventoryBatchBalanceDO::getItemCode));
    }

    /** 锁定批次余额。 */
    @Override
    public Optional<InventoryBatchBalanceDO> lockByScopeWarehouseItemAndBatch(String scopeType,
                                                                              Long scopeId,
                                                                              String warehouseName,
                                                                              String itemCode,
                                                                              String batchNo) {
        return Optional.ofNullable(inventoryBatchBalanceMapper.selectForUpdate(scopeType, scopeId, warehouseName, itemCode, batchNo));
    }

    /** 保存批次余额。 */
    @Override
    public void save(InventoryBatchBalanceDO balance) {
        inventoryBatchBalanceMapper.insert(balance);
    }

    /** 更新批次余额。 */
    @Override
    public void update(InventoryBatchBalanceDO balance) {
        inventoryBatchBalanceMapper.updateById(balance);
    }
}
