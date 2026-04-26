package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.InventoryBatchTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryBatchTransactionMapper;

/** 库存批次流水仓储实现。 */
@Repository
public class InventoryBatchTransactionRepositoryImpl implements InventoryBatchTransactionRepository {

    private final InventoryBatchTransactionMapper inventoryBatchTransactionMapper;

    /** 构造批次流水仓储。 */
    public InventoryBatchTransactionRepositoryImpl(InventoryBatchTransactionMapper inventoryBatchTransactionMapperValue) {
        this.inventoryBatchTransactionMapper = inventoryBatchTransactionMapperValue;
    }

    /** 保存批次流水。 */
    @Override
    public void save(InventoryBatchTransactionDO transaction) {
        inventoryBatchTransactionMapper.insert(transaction);
    }

    /** 按作用域查询批次流水。 */
    @Override
    public List<InventoryBatchTransactionDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return inventoryBatchTransactionMapper.selectList(new LambdaQueryWrapper<InventoryBatchTransactionDO>()
                .eq(InventoryBatchTransactionDO::getScopeType, scopeType)
                .eq(InventoryBatchTransactionDO::getScopeId, scopeId)
                .orderByAsc(InventoryBatchTransactionDO::getBusinessDate)
                .orderByAsc(InventoryBatchTransactionDO::getId));
    }

    /** 按业务日期查询批次流水。 */
    @Override
    public List<InventoryBatchTransactionDO> findByScopeAndBusinessDateRange(String scopeType,
                                                                             Long scopeId,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate) {
        LambdaQueryWrapper<InventoryBatchTransactionDO> wrapper = new LambdaQueryWrapper<InventoryBatchTransactionDO>()
                .eq(InventoryBatchTransactionDO::getScopeType, scopeType)
                .eq(InventoryBatchTransactionDO::getScopeId, scopeId);
        if (startDate != null) {
            wrapper.ge(InventoryBatchTransactionDO::getBusinessDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(InventoryBatchTransactionDO::getBusinessDate, endDate);
        }
        return inventoryBatchTransactionMapper.selectList(wrapper
                .orderByAsc(InventoryBatchTransactionDO::getBusinessDate)
                .orderByAsc(InventoryBatchTransactionDO::getId));
    }
}
