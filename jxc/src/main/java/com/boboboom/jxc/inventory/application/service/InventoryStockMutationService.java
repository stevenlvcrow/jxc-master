package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 库存余额与流水变更服务。
 */
@Service
public class InventoryStockMutationService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryStockMutationService(InventoryBalanceRepository inventoryBalanceRepository,
                                         InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    /**
     * 按指定业务变更库存余额并记录流水。
     *
     * @param scopeType     作用域类型
     * @param scopeId       作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId         业务单据 ID
     * @param bizLineId     业务明细 ID
     * @param itemCode      物品编码
     * @param itemName      物品名称
     * @param delta         库存变更数量，正数为增加，负数为减少
     * @param bizType       业务类型
     * @param operatorId    操作人 ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyDelta(String scopeType,
                           Long scopeId,
                           String warehouseName,
                           Long bizId,
                           Long bizLineId,
                           String itemCode,
                           String itemName,
                           BigDecimal delta,
                           String bizType,
                           Long operatorId) {
        InventoryBalanceDO balance = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElse(null);
        if (balance == null) {
            createInitialBalance(scopeType, scopeId, warehouseName, itemCode, itemName, delta, bizType, bizId, bizLineId, operatorId);
            return;
        }
        BigDecimal before = balance == null || balance.getQuantity() == null ? BigDecimal.ZERO : balance.getQuantity();
        BigDecimal after = before.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存不足，无法反审核");
        }
        upsertBalance(balance, scopeType, scopeId, warehouseName, itemCode, itemName, after);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType, scopeId, bizType, bizId, bizLineId, warehouseName, itemCode, itemName, delta, before, after, operatorId
        ));
    }

    private void createInitialBalance(String scopeType,
                                      Long scopeId,
                                      String warehouseName,
                                      String itemCode,
                                      String itemName,
                                      BigDecimal delta,
                                      String bizType,
                                      Long bizId,
                                      Long bizLineId,
                                      Long operatorId) {
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存不足，无法反审核");
        }
        InventoryBalanceDO created = new InventoryBalanceDO();
        created.setScopeType(scopeType);
        created.setScopeId(scopeId);
        created.setWarehouseName(warehouseName);
        created.setItemCode(itemCode);
        created.setItemName(itemName);
        created.setQuantity(delta);
        try {
            inventoryBalanceRepository.save(created);
            inventoryTransactionRepository.save(buildTransaction(
                    scopeType, scopeId, bizType, bizId, bizLineId, warehouseName, itemCode, itemName, delta, BigDecimal.ZERO, delta, operatorId
            ));
            return;
        } catch (DataIntegrityViolationException ex) {
            // Another transaction inserted the same balance row first; fall through to the locked update path.
        }
        InventoryBalanceDO locked = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElseThrow(() -> new BusinessException("库存变更失败，请重试"));
        BigDecimal before = locked.getQuantity() == null ? BigDecimal.ZERO : locked.getQuantity();
        BigDecimal after = before.add(delta);
        locked.setItemName(itemName);
        locked.setQuantity(after);
        inventoryBalanceRepository.update(locked);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType, scopeId, bizType, bizId, bizLineId, warehouseName, itemCode, itemName, delta, before, after, operatorId
        ));
    }

    private void upsertBalance(InventoryBalanceDO balance,
                               String scopeType,
                               Long scopeId,
                               String warehouseName,
                               String itemCode,
                               String itemName,
                               BigDecimal quantity) {
        if (balance == null) {
            InventoryBalanceDO created = new InventoryBalanceDO();
            created.setScopeType(scopeType);
            created.setScopeId(scopeId);
            created.setWarehouseName(warehouseName);
            created.setItemCode(itemCode);
            created.setItemName(itemName);
            created.setQuantity(quantity);
            inventoryBalanceRepository.save(created);
            return;
        }
        balance.setItemName(itemName);
        balance.setQuantity(quantity);
        inventoryBalanceRepository.update(balance);
    }

    private InventoryTransactionDO buildTransaction(String scopeType,
                                                    Long scopeId,
                                                    String bizType,
                                                    Long bizId,
                                                    Long bizLineId,
                                                    String warehouseName,
                                                    String itemCode,
                                                    String itemName,
                                                    BigDecimal delta,
                                                    BigDecimal before,
                                                    BigDecimal after,
                                                    Long operatorId) {
        InventoryTransactionDO transaction = new InventoryTransactionDO();
        transaction.setScopeType(scopeType);
        transaction.setScopeId(scopeId);
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setBizLineId(bizLineId);
        transaction.setWarehouseName(warehouseName);
        transaction.setItemCode(itemCode);
        transaction.setItemName(itemName);
        transaction.setQuantityDelta(delta);
        transaction.setBeforeQty(before);
        transaction.setAfterQty(after);
        transaction.setOperatorId(operatorId);
        return transaction;
    }
}
