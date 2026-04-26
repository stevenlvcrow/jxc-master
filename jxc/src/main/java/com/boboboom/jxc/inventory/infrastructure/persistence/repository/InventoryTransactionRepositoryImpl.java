package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryTransactionMapper;

/** 库存仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class InventoryTransactionRepositoryImpl implements InventoryTransactionRepository {

    private final InventoryTransactionMapper inventoryTransactionMapper;

    /** 库存仓储实现，负责通过持久层组件完成数据读写。 */
    public InventoryTransactionRepositoryImpl(InventoryTransactionMapper inventoryTransactionMapperValue) {
        this.inventoryTransactionMapper = inventoryTransactionMapperValue;
    }

    /** 保存业务数据。 */
    @Override
    public void save(InventoryTransactionDO transaction) {
        inventoryTransactionMapper.insert(transaction);
    }

    @Override
    public List<InventoryTransactionDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return inventoryTransactionMapper.selectByScopeOrdered(scopeType, scopeId);
    }

    @Override
    public List<InventoryTransactionDO> findByScopeAndCreatedAtRange(String scopeType,
                                                                     Long scopeId,
                                                                     LocalDateTime startTime,
                                                                     LocalDateTime endTime) {
        return inventoryTransactionMapper.selectByScopeAndCreatedAtRange(scopeType, scopeId, startTime, endTime);
    }

    @Override
    public List<InventoryTransactionDO> findByScopeAndBusinessDateRange(String scopeType,
                                                                        Long scopeId,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate) {
        return inventoryTransactionMapper.selectByScopeAndBusinessDateRange(scopeType, scopeId, startDate, endDate);
    }

    @Override
    public boolean existsByScopeWarehouseAndBusinessDateOnOrAfter(String scopeType,
                                                                  Long scopeId,
                                                                  String warehouseName,
                                                                  LocalDate businessDate) {
        Long count = inventoryTransactionMapper.countByScopeWarehouseAndBusinessDateOnOrAfter(
                scopeType, scopeId, warehouseName, businessDate
        );
        return count != null && count > 0;
    }

    @Override
    public boolean existsByScopeAndBizType(String scopeType, Long scopeId, String bizType) {
        Long count = inventoryTransactionMapper.countByScopeAndBizType(scopeType, scopeId, bizType);
        return count != null && count > 0;
    }

    @Override
    public List<InventoryBalanceDO> findLatestBalancesBefore(String scopeType,
                                                             Long scopeId,
                                                             String warehouseName,
                                                             LocalDateTime cutoff) {
        return inventoryTransactionMapper.selectLatestBalancesBefore(scopeType, scopeId, warehouseName, cutoff);
    }
}
