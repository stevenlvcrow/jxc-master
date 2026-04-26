package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryBatchBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryBatchTransactionRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryStockLockRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryStockLockDO;
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
    private final InventoryStockLockRepository inventoryStockLockRepository;
    private final InventoryBatchBalanceRepository inventoryBatchBalanceRepository;
    private final InventoryBatchTransactionRepository inventoryBatchTransactionRepository;

    /** 库存服务，负责相关业务规则和流程协作。 */
    public InventoryStockMutationService(InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                         InventoryTransactionRepository inventoryTransactionRepositoryValue,
                                         InventoryStockLockRepository inventoryStockLockRepositoryValue,
                                         InventoryBatchBalanceRepository inventoryBatchBalanceRepositoryValue,
                                         InventoryBatchTransactionRepository inventoryBatchTransactionRepositoryValue) {
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.inventoryTransactionRepository = inventoryTransactionRepositoryValue;
        this.inventoryStockLockRepository = inventoryStockLockRepositoryValue;
        this.inventoryBatchBalanceRepository = inventoryBatchBalanceRepositoryValue;
        this.inventoryBatchTransactionRepository = inventoryBatchTransactionRepositoryValue;
    }

    /**
     * 创建盘点锁库记录。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param warehouseName 仓库名称
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param sourceCode 来源单号
     * @param locks 锁库明细
     * @param operatorId 操作人
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockInventoryCheckItems(String scopeType,
                                        Long scopeId,
                                        String warehouseName,
                                        String sourceType,
                                        Long sourceId,
                                        String sourceCode,
                                        List<StockLockLine> locks,
                                        Long operatorId) {
        if (locks == null || locks.isEmpty()) {
            throw new BusinessException("锁库明细不能为空");
        }
        List<String> itemCodes = locks.stream().map(StockLockLine::itemCode).distinct().toList();
        List<InventoryStockLockDO> activeLocks = inventoryStockLockRepository.findActiveByScopeWarehouseAndItems(
                scopeType, scopeId, warehouseName, itemCodes
        );
        List<InventoryStockLockDO> conflicts = activeLocks.stream()
                .filter(lock -> !isSameLockSource(lock, sourceType, sourceId))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new BusinessException("盘点锁库冲突，物品已被其他盘点单锁定：" + conflicts.get(0).getItemCode());
        }
        Set<String> activeItemCodes = activeLocks.stream()
                .map(InventoryStockLockDO::getItemCode)
                .collect(HashSet::new, Set::add, Set::addAll);
        LocalDateTime lockedAt = LocalDateTime.now();
        for (StockLockLine line : locks) {
            if (activeItemCodes.contains(line.itemCode())) {
                continue;
            }
            InventoryStockLockDO stockLock = new InventoryStockLockDO();
            stockLock.setScopeType(scopeType);
            stockLock.setScopeId(scopeId);
            stockLock.setWarehouseName(warehouseName);
            stockLock.setItemCode(line.itemCode());
            stockLock.setItemName(line.itemName());
            stockLock.setSourceType(sourceType);
            stockLock.setSourceId(sourceId);
            stockLock.setSourceCode(sourceCode);
            stockLock.setLockReason("盘点冻结");
            stockLock.setActive(Boolean.TRUE);
            stockLock.setLockedBy(operatorId);
            stockLock.setLockedAt(lockedAt);
            stockLock.setRemark(line.remark());
            inventoryStockLockRepository.save(stockLock);
        }
    }

    /**
     * 释放指定盘点来源的锁库记录。
     *
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param operatorId 操作人
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseInventoryCheckLocks(String sourceType, Long sourceId, Long operatorId) {
        List<InventoryStockLockDO> activeLocks = inventoryStockLockRepository.findActiveBySource(sourceType, sourceId);
        LocalDateTime releasedAt = LocalDateTime.now();
        for (InventoryStockLockDO stockLock : activeLocks) {
            stockLock.setActive(Boolean.FALSE);
            stockLock.setReleasedBy(operatorId);
            stockLock.setReleasedAt(releasedAt);
            inventoryStockLockRepository.update(stockLock);
        }
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
        applyDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta, amount,
                businessDate, null, bizType, operatorId, null, null);
    }

    /**
     * 按指定业务变更库存余额、批次余额、移动加权金额并记录流水。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId 业务单据 ID
     * @param bizLineId 业务明细 ID
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param delta 库存变更数量
     * @param amount 金额
     * @param businessDate 业务日期
     * @param batchInfo 批次信息
     * @param bizType 业务类型
     * @param operatorId 操作人
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
                           BatchInfo batchInfo,
                           String bizType,
                           Long operatorId) {
        applyDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta, amount,
                businessDate, batchInfo, bizType, operatorId, null, null);
    }

    /**
     * 按指定业务变更库存余额，允许指定盘点锁库来源作为调整单放行依据。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId 业务单据 ID
     * @param bizLineId 业务明细 ID
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param delta 库存变更数量
     * @param amount 金额
     * @param businessDate 业务日期
     * @param bizType 业务类型
     * @param operatorId 操作人
     * @param lockSourceType 允许通过的锁库来源类型
     * @param lockSourceId 允许通过的锁库来源 ID
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
                           Long operatorId,
                           String lockSourceType,
                           Long lockSourceId) {
        applyDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta, amount,
                businessDate, null, bizType, operatorId, lockSourceType, lockSourceId);
    }

    /**
     * 按指定业务变更库存余额和批次余额，允许指定盘点锁库来源作为调整单放行依据。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId 业务单据 ID
     * @param bizLineId 业务明细 ID
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param delta 库存变更数量
     * @param amount 金额
     * @param businessDate 业务日期
     * @param batchInfo 批次信息
     * @param bizType 业务类型
     * @param operatorId 操作人
     * @param lockSourceType 允许通过的锁库来源类型
     * @param lockSourceId 允许通过的锁库来源 ID
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
                           BatchInfo batchInfo,
                           String bizType,
                           Long operatorId,
                           String lockSourceType,
                           Long lockSourceId) {
        BigDecimal normalizedDelta = defaultQuantity(delta);
        ensureStockNotLocked(scopeType, scopeId, warehouseName, itemCode, lockSourceType, lockSourceId);
        InventoryBalanceDO balance = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElse(null);
        if (balance == null) {
            createInitialBalance(scopeType, scopeId, warehouseName, itemCode, itemName, normalizedDelta,
                    amount, businessDate, bizType, bizId, bizLineId, operatorId, batchInfo);
            return;
        }
        applyDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode,
                itemName, normalizedDelta, amount, businessDate, bizType, operatorId, balance, batchInfo);
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
        applyAbsolute(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName,
                targetQuantity, targetAmount, businessDate, null, bizType, operatorId);
    }

    /**
     * 按指定业务将库存余额、批次余额和成本金额直接设置到目标值，并记录流水。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param warehouseName 仓库名称
     * @param bizId 业务单据 ID
     * @param bizLineId 业务明细 ID
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param targetQuantity 目标库存数量
     * @param targetAmount 目标库存金额
     * @param businessDate 业务日期
     * @param batchInfo 批次信息
     * @param bizType 业务类型
     * @param operatorId 操作人 ID
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
                              BatchInfo batchInfo,
                              String bizType,
                              Long operatorId) {
        BigDecimal normalizedTarget = normalizeNonNegative(targetQuantity);
        BigDecimal normalizedAmount = normalizeTargetAmount(normalizedTarget, targetAmount);
        ensureStockNotLocked(scopeType, scopeId, warehouseName, itemCode, null, null);
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
                    operatorId,
                    batchInfo
            );
            return;
        }
        BigDecimal before = defaultQuantity(balance.getQuantity());
        BigDecimal beforeAmount = defaultMoney(balance.getCostAmount());
        BigDecimal avgCost = averageCost(normalizedAmount, normalizedTarget);
        upsertBalance(balance, scopeType, scopeId, warehouseName, itemCode, itemName, normalizedTarget, normalizedAmount, avgCost);
        BigDecimal amountDelta = normalizedAmount.subtract(beforeAmount);
        BigDecimal delta = normalizedTarget.subtract(before);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType,
                scopeId,
                bizType,
                bizId,
                bizLineId,
                warehouseName,
                itemCode,
                itemName,
                delta,
                before,
                normalizedTarget,
                amountDelta,
                beforeAmount,
                normalizedAmount,
                avgCost,
                businessDate,
                operatorId
        ));
        applyBatchDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta,
                amountDelta, avgCost, businessDate, bizType, operatorId, batchInfo);
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
                                      Long operatorId,
                                      BatchInfo batchInfo) {
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
            applyBatchDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta,
                    normalizedAmount, avgCost, businessDate, bizType, operatorId, batchInfo);
            return;
        } catch (DataIntegrityViolationException ex) {
            // Another transaction inserted the same balance row first; fall through to the locked update path.
        }
        InventoryBalanceDO locked = inventoryBalanceRepository.lockByScopeWarehouseAndItem(
                scopeType, scopeId, warehouseName, itemCode
        ).orElseThrow(() -> new BusinessException("库存变更失败，请重试"));
        applyDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode,
                itemName, delta, amount, businessDate, bizType, operatorId, locked, batchInfo);
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
                                              Long operatorId,
                                              BatchInfo batchInfo) {
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
            applyBatchDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, targetQuantity,
                    normalizedAmount, avgCost, businessDate, bizType, operatorId, batchInfo);
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
        BigDecimal delta = targetQuantity.subtract(before);
        BigDecimal amountDelta = normalizedAmount.subtract(beforeAmount);
        inventoryTransactionRepository.save(buildTransaction(
                scopeType,
                scopeId,
                bizType,
                bizId,
                bizLineId,
                warehouseName,
                itemCode,
                itemName,
                delta,
                before,
                targetQuantity,
                amountDelta,
                beforeAmount,
                normalizedAmount,
                avgCost,
                businessDate,
                operatorId
        ));
        applyBatchDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta,
                amountDelta, avgCost, businessDate, bizType, operatorId, batchInfo);
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
                                           InventoryBalanceDO balance,
                                           BatchInfo batchInfo) {
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
        applyBatchDelta(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta,
                amountDelta, avgCost, businessDate, bizType, operatorId, batchInfo);
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

    private void applyBatchDelta(String scopeType,
                                 Long scopeId,
                                 String warehouseName,
                                 Long bizId,
                                 Long bizLineId,
                                 String itemCode,
                                 String itemName,
                                 BigDecimal delta,
                                 BigDecimal amountDelta,
                                 BigDecimal costPrice,
                                 LocalDate businessDate,
                                 String bizType,
                                 Long operatorId,
                                 BatchInfo batchInfo) {
        if (!hasBatch(batchInfo) || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        InventoryBatchBalanceDO batchBalance = inventoryBatchBalanceRepository.lockByScopeWarehouseItemAndBatch(
                scopeType, scopeId, warehouseName, itemCode, batchInfo.batchNo().trim()
        ).orElse(null);
        if (batchBalance == null) {
            if (delta.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("批次库存不足，无法完成库存扣减：" + batchInfo.batchNo());
            }
            if (amountDelta.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("批次库存金额不足，无法完成库存扣减：" + batchInfo.batchNo());
            }
            createInitialBatchBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName, delta,
                    amountDelta, costPrice, businessDate, bizType, operatorId, batchInfo);
            return;
        }
        applyBatchDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName,
                delta, amountDelta, costPrice, businessDate, bizType, operatorId, batchInfo, batchBalance);
    }

    private void createInitialBatchBalance(String scopeType,
                                           Long scopeId,
                                           String warehouseName,
                                           Long bizId,
                                           Long bizLineId,
                                           String itemCode,
                                           String itemName,
                                           BigDecimal delta,
                                           BigDecimal amountDelta,
                                           BigDecimal costPrice,
                                           LocalDate businessDate,
                                           String bizType,
                                           Long operatorId,
                                           BatchInfo batchInfo) {
        BigDecimal quantity = delta.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        BigDecimal amount = amountDelta.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次库存金额不足，无法完成库存扣减：" + batchInfo.batchNo());
        }
        if (quantity.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("批次入库金额必须大于0");
        }
        InventoryBatchBalanceDO created = new InventoryBatchBalanceDO();
        created.setScopeType(scopeType);
        created.setScopeId(scopeId);
        created.setWarehouseName(warehouseName);
        created.setItemCode(itemCode);
        created.setItemName(itemName);
        applyBatchInfo(created, batchInfo);
        created.setQuantity(quantity);
        created.setCostAmount(amount);
        created.setAvgCost(averageCost(amount, quantity));
        try {
            inventoryBatchBalanceRepository.save(created);
            inventoryBatchTransactionRepository.save(buildBatchTransaction(scopeType, scopeId, bizType, bizId, bizLineId,
                    warehouseName, itemCode, itemName, batchInfo, quantity, BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP),
                    quantity, amount, BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), amount,
                    costPrice, businessDate, operatorId));
            return;
        } catch (DataIntegrityViolationException ex) {
            // Another transaction inserted the same batch row first; fall through to the locked update path.
        }
        InventoryBatchBalanceDO locked = inventoryBatchBalanceRepository.lockByScopeWarehouseItemAndBatch(
                scopeType, scopeId, warehouseName, itemCode, batchInfo.batchNo().trim()
        ).orElseThrow(() -> new BusinessException("批次库存变更失败，请重试"));
        applyBatchDeltaToLockedBalance(scopeType, scopeId, warehouseName, bizId, bizLineId, itemCode, itemName,
                delta, amountDelta, costPrice, businessDate, bizType, operatorId, batchInfo, locked);
    }

    private void applyBatchDeltaToLockedBalance(String scopeType,
                                                Long scopeId,
                                                String warehouseName,
                                                Long bizId,
                                                Long bizLineId,
                                                String itemCode,
                                                String itemName,
                                                BigDecimal delta,
                                                BigDecimal amountDelta,
                                                BigDecimal costPrice,
                                                LocalDate businessDate,
                                                String bizType,
                                                Long operatorId,
                                                BatchInfo batchInfo,
                                                InventoryBatchBalanceDO batchBalance) {
        BigDecimal before = defaultQuantity(batchBalance.getQuantity());
        BigDecimal beforeAmount = defaultMoney(batchBalance.getCostAmount());
        BigDecimal after = before.add(delta).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次库存不足，无法完成库存扣减：" + batchInfo.batchNo());
        }
        BigDecimal afterAmount = beforeAmount.add(amountDelta).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (afterAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次库存金额不足，无法完成库存扣减：" + batchInfo.batchNo());
        }
        if (after.compareTo(BigDecimal.ZERO) == 0 && afterAmount.compareTo(BigDecimal.ZERO) != 0) {
            afterAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal avgCost = averageCost(afterAmount, after);
        batchBalance.setItemName(itemName);
        batchBalance.setManufacturer(trimNullable(batchInfo.manufacturer()));
        batchBalance.setProductionDate(batchInfo.productionDate());
        batchBalance.setExpiryDate(batchInfo.expiryDate());
        batchBalance.setQuantity(after);
        batchBalance.setCostAmount(afterAmount);
        batchBalance.setAvgCost(avgCost);
        inventoryBatchBalanceRepository.update(batchBalance);
        inventoryBatchTransactionRepository.save(buildBatchTransaction(scopeType, scopeId, bizType, bizId, bizLineId,
                warehouseName, itemCode, itemName, batchInfo, delta, before, after, amountDelta, beforeAmount,
                afterAmount, costPrice, businessDate, operatorId));
    }

    private InventoryBatchTransactionDO buildBatchTransaction(String scopeType,
                                                              Long scopeId,
                                                              String bizType,
                                                              Long bizId,
                                                              Long bizLineId,
                                                              String warehouseName,
                                                              String itemCode,
                                                              String itemName,
                                                              BatchInfo batchInfo,
                                                              BigDecimal delta,
                                                              BigDecimal before,
                                                              BigDecimal after,
                                                              BigDecimal amountDelta,
                                                              BigDecimal beforeAmount,
                                                              BigDecimal afterAmount,
                                                              BigDecimal costPrice,
                                                              LocalDate businessDate,
                                                              Long operatorId) {
        InventoryBatchTransactionDO transaction = new InventoryBatchTransactionDO();
        transaction.setScopeType(scopeType);
        transaction.setScopeId(scopeId);
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setBizLineId(bizLineId);
        transaction.setWarehouseName(warehouseName);
        transaction.setItemCode(itemCode);
        transaction.setItemName(itemName);
        transaction.setBatchNo(batchInfo.batchNo().trim());
        transaction.setManufacturer(trimNullable(batchInfo.manufacturer()));
        transaction.setProductionDate(batchInfo.productionDate());
        transaction.setExpiryDate(batchInfo.expiryDate());
        transaction.setBusinessDate(businessDate == null ? LocalDate.now() : businessDate);
        transaction.setQuantityDelta(delta.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        transaction.setBeforeQty(before.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        transaction.setAfterQty(after.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        transaction.setAmountDelta(amountDelta.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        transaction.setBeforeAmount(beforeAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        transaction.setAfterAmount(afterAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        transaction.setCostPrice(defaultMoney(costPrice));
        transaction.setOperatorId(operatorId);
        return transaction;
    }

    private void applyBatchInfo(InventoryBatchBalanceDO balance, BatchInfo batchInfo) {
        balance.setBatchNo(batchInfo.batchNo().trim());
        balance.setManufacturer(trimNullable(batchInfo.manufacturer()));
        balance.setProductionDate(batchInfo.productionDate());
        balance.setExpiryDate(batchInfo.expiryDate());
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

    private void ensureStockNotLocked(String scopeType,
                                      Long scopeId,
                                      String warehouseName,
                                      String itemCode,
                                      String allowedSourceType,
                                      Long allowedSourceId) {
        List<InventoryStockLockDO> locks = inventoryStockLockRepository.findActiveByScopeWarehouseAndItems(
                scopeType, scopeId, warehouseName, List.of(itemCode)
        );
        if (locks.isEmpty()) {
            return;
        }
        for (InventoryStockLockDO lock : locks) {
            if (isSameLockSource(lock, allowedSourceType, allowedSourceId)) {
                continue;
            }
            throw new BusinessException("库存已被盘点冻结，禁止出入库：" + itemCode);
        }
    }

    private boolean isSameLockSource(InventoryStockLockDO lock, String sourceType, Long sourceId) {
        return sourceType != null && sourceId != null && sourceId.equals(lock.getSourceId()) && sourceType.equals(lock.getSourceType());
    }

    private boolean hasBatch(BatchInfo batchInfo) {
        return batchInfo != null && StringUtils.hasText(batchInfo.batchNo());
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    /**
     * 锁库明细。
     *
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param remark 备注
     */
    public record StockLockLine(String itemCode, String itemName, String remark) {
    }

    /**
     * 批次信息。
     *
     * @param batchNo 批次号
     * @param manufacturer 生产厂家
     * @param productionDate 生产日期
     * @param expiryDate 到期日期
     */
    public record BatchInfo(String batchNo, String manufacturer, LocalDate productionDate, LocalDate expiryDate) {
    }
}
