package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;

/**
 * 库存余额与流水变更服务。
 */
@Service
public class InventoryStockMutationService {

    private static final int QUANTITY_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /** 库存服务，负责相关业务规则和流程协作。 */
    public InventoryStockMutationService(InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                         InventoryTransactionRepository inventoryTransactionRepositoryValue) {
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.inventoryTransactionRepository = inventoryTransactionRepositoryValue;
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
        applyDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName,
                delta, null, null, bizType, operatorId);
    }

    /**
     * 按指定业务变更库存余额、移动加权金额并记录流水。
     *
     * @param scopeType     作用域类型
     * @param scopeId       作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId         业务单据 ID
     * @param bizLineId     业务明细 ID
     * @param itemCode      物品编码
     * @param itemName      物品名称
     * @param delta         库存变更数量，正数为增加，负数为减少
     * @param amount        入库金额，出库和反向扣减按当前移动均价计算
     * @param businessDate  业务日期
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
                           BigDecimal amount,
                           LocalDate businessDate,
                           String bizType,
                           Long operatorId) {
        BigDecimal normalizedDelta = defaultQuantity(delta);
        InventoryBalanceDO balance = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElse(null);
        if (balance == null) {
            createInitialBalance(scopeType, scopeId, warehouseName, itemCode, itemName, normalizedDelta,
                    amount, businessDate, bizType, bizId, bizLineId, operatorId);
            return;
        }
        applyDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode,
                itemName, normalizedDelta, amount, businessDate, bizType, operatorId, balance);
    }

    /**
     * 按指定业务将库存余额直接设置到目标值，并记录流水。
     *
     * @param scopeType       作用域类型
     * @param scopeId         作用域 ID
     * @param warehouseName   仓库名称
     * @param bizId           业务单据 ID
     * @param bizLineId       业务明细 ID
     * @param itemCode        物品编码
     * @param itemName        物品名称
     * @param targetQuantity  目标库存数量
     * @param bizType         业务类型
     * @param operatorId      操作人 ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyAbsolute(String scopeType,
                              Long scopeId,
                              String warehouseName,
                              Long bizId,
                              Long bizLineId,
                              String itemCode,
                              String itemName,
                              BigDecimal targetQuantity,
                              String bizType,
                              Long operatorId) {
        applyAbsolute(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName,
                targetQuantity, null, null, bizType, operatorId);
    }

    /**
     * 按指定业务将库存余额和成本金额直接设置到目标值，并记录流水。
     *
     * @param scopeType       作用域类型
     * @param scopeId         作用域 ID
     * @param warehouseName   仓库名称
     * @param bizId           业务单据 ID
     * @param bizLineId       业务明细 ID
     * @param itemCode        物品编码
     * @param itemName        物品名称
     * @param targetQuantity  目标库存数量
     * @param targetAmount    目标库存金额
     * @param businessDate    业务日期
     * @param bizType         业务类型
     * @param operatorId      操作人 ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyAbsolute(String scopeType,
                              Long scopeId,
                              String warehouseName,
                              Long bizId,
                              Long bizLineId,
                              String itemCode,
                              String itemName,
                              BigDecimal targetQuantity,
                              BigDecimal targetAmount,
                              LocalDate businessDate,
                              String bizType,
                              Long operatorId) {
        BigDecimal normalizedTarget = normalizeNonNegative(targetQuantity);
        BigDecimal normalizedAmount = normalizeTargetAmount(normalizedTarget, targetAmount);
        InventoryBalanceDO balance = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElse(null);
        if (balance == null) {
            createInitialAbsoluteBalance(
                    scopeType,
                    scopeId,
                    warehouseName,
                    itemCode,
                    itemName,
                    normalizedTarget,
                    normalizedAmount,
                    businessDate,
                    bizType,
                    bizId,
                    bizLineId,
                    operatorId
            );
            return;
        }
        BigDecimal before = defaultQuantity(balance.getQuantity());
        BigDecimal beforeAmount = defaultMoney(balance.getCostAmount());
        BigDecimal avgCost = averageCost(normalizedAmount, normalizedTarget);
        upsertBalance(balance, scopeType, scopeId, warehouseName, itemCode, itemName, normalizedTarget, normalizedAmount, avgCost);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType,
                scopeId,
                bizType,
                bizId,
                bizLineId,
                warehouseName,
                itemCode,
                itemName,
                normalizedTarget.subtract(before),
                before,
                normalizedTarget,
                normalizedAmount.subtract(beforeAmount),
                beforeAmount,
                normalizedAmount,
                avgCost,
                businessDate,
                operatorId
        ));
    }

    private void createInitialBalance(String scopeType,
                                      Long scopeId,
                                      String warehouseName,
                                      String itemCode,
                                      String itemName,
                                      BigDecimal delta,
                                      BigDecimal amount,
                                      LocalDate businessDate,
                                      String bizType,
                                      Long bizId,
                                      Long bizLineId,
                                      Long operatorId) {
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存不足，无法完成库存扣减");
        }
        BigDecimal normalizedAmount = normalizeInboundAmount(delta, amount);
        BigDecimal avgCost = averageCost(normalizedAmount, delta);
        InventoryBalanceDO created = new InventoryBalanceDO();
        created.setScopeType(scopeType);
        created.setScopeId(scopeId);
        created.setWarehouseName(warehouseName);
        created.setItemCode(itemCode);
        created.setItemName(itemName);
        created.setQuantity(delta);
        created.setCostAmount(normalizedAmount);
        created.setAvgCost(avgCost);
        try {
            inventoryBalanceRepository.save(created);
            inventoryTransactionRepository.save(buildTransaction(
                    scopeType, scopeId, bizType, bizId, bizLineId, warehouseName, itemCode, itemName,
                    delta, BigDecimal.ZERO, delta, normalizedAmount, BigDecimal.ZERO, normalizedAmount,
                    avgCost, businessDate, operatorId
            ));
            return;
        } catch (DataIntegrityViolationException ex) {
            // Another transaction inserted the same balance row first; fall through to the locked update path.
        }
        InventoryBalanceDO locked = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElseThrow(() -> new BusinessException("库存变更失败，请重试"));
        applyDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode,
                itemName, delta, amount, businessDate, bizType, operatorId, locked);
    }

    private void createInitialAbsoluteBalance(String scopeType,
                                              Long scopeId,
                                              String warehouseName,
                                              String itemCode,
                                              String itemName,
                                              BigDecimal targetQuantity,
                                              BigDecimal targetAmount,
                                              LocalDate businessDate,
                                              String bizType,
                                              Long bizId,
                                              Long bizLineId,
                                              Long operatorId) {
        BigDecimal normalizedAmount = normalizeTargetAmount(targetQuantity, targetAmount);
        BigDecimal avgCost = averageCost(normalizedAmount, targetQuantity);
        InventoryBalanceDO created = new InventoryBalanceDO();
        created.setScopeType(scopeType);
        created.setScopeId(scopeId);
        created.setWarehouseName(warehouseName);
        created.setItemCode(itemCode);
        created.setItemName(itemName);
        created.setQuantity(targetQuantity);
        created.setCostAmount(normalizedAmount);
        created.setAvgCost(avgCost);
        try {
            inventoryBalanceRepository.save(created);
            inventoryTransactionRepository.save(buildTransaction(
                    scopeType, scopeId, bizType, bizId, bizLineId, warehouseName, itemCode, itemName,
                    targetQuantity, BigDecimal.ZERO, targetQuantity, normalizedAmount, BigDecimal.ZERO,
                    normalizedAmount, avgCost, businessDate, operatorId
            ));
            return;
        } catch (DataIntegrityViolationException ex) {
            // Another transaction inserted the same balance row first; fall through to the locked update path.
        }
        InventoryBalanceDO locked = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElseThrow(() -> new BusinessException("库存变更失败，请重试"));
        BigDecimal before = defaultQuantity(locked.getQuantity());
        BigDecimal beforeAmount = defaultMoney(locked.getCostAmount());
        upsertBalance(locked, scopeType, scopeId, warehouseName, itemCode, itemName, targetQuantity, normalizedAmount, avgCost);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType,
                scopeId,
                bizType,
                bizId,
                bizLineId,
                warehouseName,
                itemCode,
                itemName,
                targetQuantity.subtract(before),
                before,
                targetQuantity,
                normalizedAmount.subtract(beforeAmount),
                beforeAmount,
                normalizedAmount,
                avgCost,
                businessDate,
                operatorId
        ));
    }

    private void applyDeltaToLockedBalance(String scopeType,
                                           Long scopeId,
                                           String warehouseName,
                                           Long bizId,
                                           Long bizLineId,
                                           String itemCode,
                                           String itemName,
                                           BigDecimal delta,
                                           BigDecimal amount,
                                           LocalDate businessDate,
                                           String bizType,
                                           Long operatorId,
                                           InventoryBalanceDO balance) {
        BigDecimal before = defaultQuantity(balance.getQuantity());
        BigDecimal beforeAmount = defaultMoney(balance.getCostAmount());
        BigDecimal after = before.add(delta).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存不足，无法完成库存扣减");
        }
        BigDecimal amountDelta = resolveAmountDelta(delta, amount, balance);
        BigDecimal afterAmount = beforeAmount.add(amountDelta).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (afterAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存金额不足，无法完成库存扣减");
        }
        if (after.compareTo(BigDecimal.ZERO) == 0 && afterAmount.compareTo(BigDecimal.ZERO) != 0) {
            afterAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal avgCost = averageCost(afterAmount, after);
        upsertBalance(balance, scopeType, scopeId, warehouseName, itemCode, itemName, after, afterAmount, avgCost);
        inventoryTransactionRepository.save(buildTransaction(scopeType, scopeId, bizType, bizId,
                bizLineId, warehouseName, itemCode, itemName, delta, before, after, amountDelta, beforeAmount,
                afterAmount, avgCost, businessDate, operatorId));
    }

    private void upsertBalance(InventoryBalanceDO balance,
                               String scopeType,
                               Long scopeId,
                               String warehouseName,
                               String itemCode,
                               String itemName,
                               BigDecimal quantity,
                               BigDecimal costAmount,
                               BigDecimal avgCost) {
        if (balance == null) {
            InventoryBalanceDO created = new InventoryBalanceDO();
            created.setScopeType(scopeType);
            created.setScopeId(scopeId);
            created.setWarehouseName(warehouseName);
            created.setItemCode(itemCode);
            created.setItemName(itemName);
            created.setQuantity(quantity);
            created.setCostAmount(costAmount);
            created.setAvgCost(avgCost);
            inventoryBalanceRepository.save(created);
            return;
        }
        balance.setItemName(itemName);
        balance.setQuantity(quantity);
        balance.setCostAmount(costAmount);
        balance.setAvgCost(avgCost);
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
                                                    BigDecimal amountDelta,
                                                    BigDecimal beforeAmount,
                                                    BigDecimal afterAmount,
                                                    BigDecimal costPrice,
                                                    LocalDate businessDate,
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
        transaction.setBusinessDate(businessDate == null ? LocalDate.now() : businessDate);
        transaction.setQuantityDelta(delta);
        transaction.setBeforeQty(before);
        transaction.setAfterQty(after);
        transaction.setAmountDelta(amountDelta);
        transaction.setBeforeAmount(beforeAmount);
        transaction.setAfterAmount(afterAmount);
        transaction.setCostPrice(costPrice);
        transaction.setOperatorId(operatorId);
        return transaction;
    }

    private BigDecimal resolveAmountDelta(BigDecimal delta, BigDecimal amount, InventoryBalanceDO balance) {
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (amount != null) {
                return normalizeInboundAmount(delta, amount);
            }
            return delta.multiply(resolveAverageCost(balance)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (amount != null) {
            return normalizeInboundAmount(delta.abs(), amount).negate();
        }
        return delta.abs().multiply(resolveAverageCost(balance)).negate().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeInboundAmount(BigDecimal quantity, BigDecimal amount) {
        BigDecimal normalizedAmount = defaultMoney(amount);
        if (quantity.compareTo(BigDecimal.ZERO) > 0 && normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("入库金额必须大于0");
        }
        return normalizedAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeTargetAmount(BigDecimal quantity, BigDecimal amount) {
        BigDecimal normalizedAmount = defaultMoney(amount);
        if (quantity.compareTo(BigDecimal.ZERO) == 0 && normalizedAmount.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("期初数量为0时金额必须为0");
        }
        if (quantity.compareTo(BigDecimal.ZERO) > 0 && normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("期初金额必须大于0");
        }
        return normalizedAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal averageCost(BigDecimal amount, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return amount.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveAverageCost(InventoryBalanceDO balance) {
        BigDecimal avgCost = defaultMoney(balance.getAvgCost());
        if (avgCost.compareTo(BigDecimal.ZERO) != 0 || defaultQuantity(balance.getQuantity()).compareTo(BigDecimal.ZERO) == 0) {
            return avgCost;
        }
        return defaultMoney(balance.getCostAmount()).divide(defaultQuantity(balance.getQuantity()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegative(BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存数量不能小于 0");
        }
        return quantity;
    }

    private BigDecimal defaultQuantity(BigDecimal quantity) {
        return quantity == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP) : quantity;
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : amount;
    }
}
