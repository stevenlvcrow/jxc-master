package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.InventoryStockLockRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryStockLockDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryStockLockMapper;

/** 库存锁库仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class InventoryStockLockRepositoryImpl implements InventoryStockLockRepository {

    private final InventoryStockLockMapper inventoryStockLockMapper;

    /** 库存锁库仓储实现，负责通过持久层组件完成数据读写。 */
    public InventoryStockLockRepositoryImpl(InventoryStockLockMapper inventoryStockLockMapperValue) {
        this.inventoryStockLockMapper = inventoryStockLockMapperValue;
    }

    /** 查询指定仓库物品的有效锁。 */
    @Override
    public List<InventoryStockLockDO> findActiveByScopeWarehouseAndItems(String scopeType,
                                                                         Long scopeId,
                                                                         String warehouseName,
                                                                         List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryStockLockMapper.selectList(new LambdaQueryWrapper<InventoryStockLockDO>()
                .eq(InventoryStockLockDO::getScopeType, scopeType)
                .eq(InventoryStockLockDO::getScopeId, scopeId)
                .eq(InventoryStockLockDO::getWarehouseName, warehouseName)
                .in(InventoryStockLockDO::getItemCode, itemCodes)
                .eq(InventoryStockLockDO::getActive, Boolean.TRUE)
                .orderByAsc(InventoryStockLockDO::getItemCode)
                .orderByAsc(InventoryStockLockDO::getId));
    }

    /** 查询指定来源的有效锁。 */
    @Override
    public List<InventoryStockLockDO> findActiveBySource(String sourceType, Long sourceId) {
        return inventoryStockLockMapper.selectList(new LambdaQueryWrapper<InventoryStockLockDO>()
                .eq(InventoryStockLockDO::getSourceType, sourceType)
                .eq(InventoryStockLockDO::getSourceId, sourceId)
                .eq(InventoryStockLockDO::getActive, Boolean.TRUE)
                .orderByAsc(InventoryStockLockDO::getId));
    }

    /** 保存锁库记录。 */
    @Override
    public void save(InventoryStockLockDO stockLock) {
        inventoryStockLockMapper.insert(stockLock);
    }

    /** 更新锁库记录。 */
    @Override
    public void update(InventoryStockLockDO stockLock) {
        inventoryStockLockMapper.updateById(stockLock);
    }
}
