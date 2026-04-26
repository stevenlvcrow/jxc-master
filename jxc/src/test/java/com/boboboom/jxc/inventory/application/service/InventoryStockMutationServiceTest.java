package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/** 库存移动加权变更服务测试。 */
class InventoryStockMutationServiceTest {

    private static final String SCOPE_TYPE = "STORE";
    private static final Long SCOPE_ID = 1L;
    private static final String WAREHOUSE = "主仓";
    private static final String ITEM_CODE = "ITEM-001";
    private static final String ITEM_NAME = "牛肉";
    private static final Long OPERATOR_ID = 9L;
    private static final Long OPENING_ID = 1L;
    private static final Long INBOUND_ID = 2L;
    private static final Long OUTBOUND_ID = 3L;
    private static final Long CHECK_ID = 4L;
    private static final int THIRD_TRANSACTION_INDEX = 2;
    private static final int NEXT_ID_OFFSET = 1;
    private static final int YEAR = 2026;
    private static final int MONTH = 5;
    private static final int FIRST_DAY = 1;
    private static final int SECOND_DAY = 2;
    private static final int THIRD_DAY = 3;

    private InMemoryBalanceRepository balanceRepository;
    private InMemoryTransactionRepository transactionRepository;
    private InMemoryStockLockRepository stockLockRepository;
    private InMemoryBatchBalanceRepository batchBalanceRepository;
    private InMemoryBatchTransactionRepository batchTransactionRepository;
    private InventoryStockMutationService service;

    @BeforeEach
    void setUp() {
        balanceRepository = new InMemoryBalanceRepository();
        transactionRepository = new InMemoryTransactionRepository();
        stockLockRepository = new InMemoryStockLockRepository();
        batchBalanceRepository = new InMemoryBatchBalanceRepository();
        batchTransactionRepository = new InMemoryBatchTransactionRepository();
        service = new InventoryStockMutationService(
                balanceRepository,
                transactionRepository,
                stockLockRepository,
                batchBalanceRepository,
                batchTransactionRepository
        );
    }

    @Test
    void shouldCalculateMovingAverageForOpeningInboundAndOutbound() {
        service.applyAbsolute(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OPENING_ID,
                OPENING_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("100"),
                LocalDate.of(YEAR, MONTH, FIRST_DAY),
                "PERIOD_OPENING_BALANCE_CONFIRM",
                OPERATOR_ID
        );

        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                INBOUND_ID,
                INBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("200"),
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                "PURCHASE_INBOUND_APPROVE",
                OPERATOR_ID
        );

        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OUTBOUND_ID,
                OUTBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-5"),
                null,
                LocalDate.of(YEAR, MONTH, THIRD_DAY),
                "DEPARTMENT_PICKING_APPROVE",
                OPERATOR_ID
        );

        InventoryBalanceDO balance = balanceRepository.require(WAREHOUSE, ITEM_CODE);
        assertAmountEquals("15.0000", balance.getQuantity());
        assertAmountEquals("225.00", balance.getCostAmount());
        assertAmountEquals("15.00", balance.getAvgCost());

        InventoryTransactionDO outboundTxn = transactionRepository.saved().get(THIRD_TRANSACTION_INDEX);
        assertAmountEquals("-75.00", outboundTxn.getAmountDelta());
        assertAmountEquals("300.00", outboundTxn.getBeforeAmount());
        assertAmountEquals("225.00", outboundTxn.getAfterAmount());
        assertAmountEquals("15.00", outboundTxn.getCostPrice());
        Assertions.assertEquals(LocalDate.of(YEAR, MONTH, THIRD_DAY), outboundTxn.getBusinessDate());
    }

    @Test
    void shouldRejectOutboundBeyondQuantity() {
        service.applyAbsolute(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OPENING_ID,
                OPENING_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("100"),
                LocalDate.of(YEAR, MONTH, FIRST_DAY),
                "PERIOD_OPENING_BALANCE_CONFIRM",
                OPERATOR_ID
        );

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                INBOUND_ID,
                INBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-11"),
                null,
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                "DAMAGE_OUTBOUND",
                OPERATOR_ID
        ));

        Assertions.assertEquals("库存不足，无法完成库存扣减", exception.getMessage());
    }

    @Test
    void shouldRejectOutboundBeyondCostAmount() {
        service.applyAbsolute(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OPENING_ID,
                OPENING_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("100"),
                LocalDate.of(YEAR, MONTH, FIRST_DAY),
                "PERIOD_OPENING_BALANCE_CONFIRM",
                OPERATOR_ID
        );

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                INBOUND_ID,
                INBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-5"),
                decimal("200"),
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                "DAMAGE_OUTBOUND",
                OPERATOR_ID
        ));

        Assertions.assertEquals("库存金额不足，无法完成库存扣减", exception.getMessage());
    }

    @Test
    void shouldRejectNormalMutationWhenStockLockedAndAllowSameInventoryCheckSource() {
        service.applyAbsolute(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OPENING_ID,
                OPENING_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("100"),
                LocalDate.of(YEAR, MONTH, FIRST_DAY),
                "PERIOD_OPENING_BALANCE_CONFIRM",
                OPERATOR_ID
        );
        service.lockInventoryCheckItems(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                "INVENTORY_CHECK",
                CHECK_ID,
                "PD202605010001",
                List.of(new InventoryStockMutationService.StockLockLine(ITEM_CODE, ITEM_NAME, null)),
                OPERATOR_ID
        );

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                INBOUND_ID,
                INBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("1"),
                decimal("10"),
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                "OTHER_INBOUND",
                OPERATOR_ID
        ));
        Assertions.assertEquals("库存已被盘点冻结，禁止出入库：ITEM-001", exception.getMessage());

        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OUTBOUND_ID,
                OUTBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-1"),
                null,
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                "DAMAGE_OUTBOUND",
                OPERATOR_ID,
                "INVENTORY_CHECK",
                CHECK_ID
        );
        assertAmountEquals("9.0000", balanceRepository.require(WAREHOUSE, ITEM_CODE).getQuantity());

        service.releaseInventoryCheckLocks("INVENTORY_CHECK", CHECK_ID, OPERATOR_ID);
        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OUTBOUND_ID,
                OUTBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-1"),
                null,
                LocalDate.of(YEAR, MONTH, THIRD_DAY),
                "DAMAGE_OUTBOUND",
                OPERATOR_ID
        );
        assertAmountEquals("8.0000", balanceRepository.require(WAREHOUSE, ITEM_CODE).getQuantity());
    }

    @Test
    void shouldTrackBatchBalanceAndRejectBatchOverdraw() {
        InventoryStockMutationService.BatchInfo batchInfo = new InventoryStockMutationService.BatchInfo(
                "BATCH-001",
                "供应商A",
                LocalDate.of(YEAR, MONTH, FIRST_DAY),
                LocalDate.of(YEAR, MONTH, THIRD_DAY)
        );
        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                INBOUND_ID,
                INBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("10"),
                decimal("200"),
                LocalDate.of(YEAR, MONTH, SECOND_DAY),
                batchInfo,
                "PURCHASE_INBOUND_APPROVE",
                OPERATOR_ID
        );
        service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OUTBOUND_ID,
                OUTBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-4"),
                null,
                LocalDate.of(YEAR, MONTH, THIRD_DAY),
                batchInfo,
                "DAMAGE_OUTBOUND_APPROVE",
                OPERATOR_ID
        );

        InventoryBatchBalanceDO batchBalance = batchBalanceRepository.require(WAREHOUSE, ITEM_CODE, "BATCH-001");
        assertAmountEquals("6.0000", batchBalance.getQuantity());
        assertAmountEquals("120.00", batchBalance.getCostAmount());
        Assertions.assertEquals(2, batchTransactionRepository.saved().size());

        InventoryStockMutationService.BatchInfo otherBatch = new InventoryStockMutationService.BatchInfo(
                "BATCH-002",
                null,
                null,
                null
        );
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> service.applyDelta(
                SCOPE_TYPE,
                SCOPE_ID,
                WAREHOUSE,
                OUTBOUND_ID,
                OUTBOUND_ID,
                ITEM_CODE,
                ITEM_NAME,
                decimal("-1"),
                null,
                LocalDate.of(YEAR, MONTH, THIRD_DAY),
                otherBatch,
                "DAMAGE_OUTBOUND_APPROVE",
                OPERATOR_ID
        ));
        Assertions.assertEquals("批次库存不足，无法完成库存扣减：BATCH-002", exception.getMessage());
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private void assertAmountEquals(String expected, BigDecimal actual) {
        Assertions.assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static final class InMemoryBalanceRepository implements InventoryBalanceRepository {

        private final List<InventoryBalanceDO> balances = new ArrayList<>();

        @Override
        public List<InventoryBalanceDO> findByScopeOrdered(String scopeType, Long scopeId) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .sorted(Comparator.comparing(InventoryBalanceDO::getWarehouseName)
                            .thenComparing(InventoryBalanceDO::getItemCode))
                    .toList();
        }

        @Override
        public List<InventoryBalanceDO> findByScopeAndWarehouseOrdered(String scopeType,
                                                                       Long scopeId,
                                                                       String warehouseName) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .filter(balance -> balance.getWarehouseName().equals(warehouseName))
                    .sorted(Comparator.comparing(InventoryBalanceDO::getItemCode))
                    .toList();
        }

        @Override
        public Optional<InventoryBalanceDO> findByScopeWarehouseAndItem(String scopeType,
                                                                        Long scopeId,
                                                                        String warehouseName,
                                                                        String itemCode) {
            return find(scopeType, scopeId, warehouseName, itemCode);
        }

        @Override
        public Optional<InventoryBalanceDO> lockByScopeWarehouseAndItem(String scopeType,
                                                                        Long scopeId,
                                                                        String warehouseName,
                                                                        String itemCode) {
            return find(scopeType, scopeId, warehouseName, itemCode);
        }

        @Override
        public void save(InventoryBalanceDO balance) {
            balance.setId((long) balances.size() + NEXT_ID_OFFSET);
            balances.add(balance);
        }

        @Override
        public void update(InventoryBalanceDO balance) {
            // 对象引用已在内存列表中，更新由调用方直接完成。
        }

        private InventoryBalanceDO require(String warehouseName, String itemCode) {
            return find(SCOPE_TYPE, SCOPE_ID, warehouseName, itemCode)
                    .orElseThrow(() -> new AssertionError("库存余额不存在"));
        }

        private Optional<InventoryBalanceDO> find(String scopeType,
                                                  Long scopeId,
                                                  String warehouseName,
                                                  String itemCode) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .filter(balance -> balance.getWarehouseName().equals(warehouseName))
                    .filter(balance -> balance.getItemCode().equals(itemCode))
                    .findFirst();
        }

        private boolean matchesScope(InventoryBalanceDO balance, String scopeType, Long scopeId) {
            return balance.getScopeType().equals(scopeType) && balance.getScopeId().equals(scopeId);
        }
    }

    private static final class InMemoryTransactionRepository implements InventoryTransactionRepository {

        private final List<InventoryTransactionDO> transactions = new ArrayList<>();

        @Override
        public void save(InventoryTransactionDO transaction) {
            transaction.setId((long) transactions.size() + NEXT_ID_OFFSET);
            transaction.setCreatedAt(LocalDateTime.now());
            transactions.add(transaction);
        }

        @Override
        public List<InventoryTransactionDO> findByScopeOrdered(String scopeType, Long scopeId) {
            return transactions.stream()
                    .filter(transaction -> matchesScope(transaction, scopeType, scopeId))
                    .toList();
        }

        @Override
        public List<InventoryTransactionDO> findByScopeAndCreatedAtRange(String scopeType,
                                                                         Long scopeId,
                                                                         LocalDateTime startTime,
                                                                         LocalDateTime endTime) {
            return findByScopeOrdered(scopeType, scopeId).stream()
                    .filter(transaction -> !transaction.getCreatedAt().isBefore(startTime))
                    .filter(transaction -> !transaction.getCreatedAt().isAfter(endTime))
                    .toList();
        }

        @Override
        public List<InventoryTransactionDO> findByScopeAndBusinessDateRange(String scopeType,
                                                                            Long scopeId,
                                                                            LocalDate startDate,
                                                                            LocalDate endDate) {
            return findByScopeOrdered(scopeType, scopeId).stream()
                    .filter(transaction -> !transaction.getBusinessDate().isBefore(startDate))
                    .filter(transaction -> !transaction.getBusinessDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public boolean existsByScopeWarehouseAndBusinessDateOnOrAfter(String scopeType,
                                                                      Long scopeId,
                                                                      String warehouseName,
                                                                      LocalDate businessDate) {
            return findByScopeOrdered(scopeType, scopeId).stream()
                    .filter(transaction -> transaction.getWarehouseName().equals(warehouseName))
                    .anyMatch(transaction -> !transaction.getBusinessDate().isBefore(businessDate));
        }

        @Override
        public List<InventoryBalanceDO> findLatestBalancesBefore(String scopeType,
                                                                 Long scopeId,
                                                                 String warehouseName,
                                                                 LocalDateTime cutoff) {
            return List.of();
        }

        private List<InventoryTransactionDO> saved() {
            return transactions;
        }

        private boolean matchesScope(InventoryTransactionDO transaction, String scopeType, Long scopeId) {
            return transaction.getScopeType().equals(scopeType) && transaction.getScopeId().equals(scopeId);
        }
    }

    private static final class InMemoryStockLockRepository implements InventoryStockLockRepository {

        private final List<InventoryStockLockDO> locks = new ArrayList<>();

        @Override
        public List<InventoryStockLockDO> findActiveByScopeWarehouseAndItems(String scopeType,
                                                                             Long scopeId,
                                                                             String warehouseName,
                                                                             List<String> itemCodes) {
            return locks.stream()
                    .filter(lock -> Boolean.TRUE.equals(lock.getActive()))
                    .filter(lock -> lock.getScopeType().equals(scopeType))
                    .filter(lock -> lock.getScopeId().equals(scopeId))
                    .filter(lock -> lock.getWarehouseName().equals(warehouseName))
                    .filter(lock -> itemCodes.contains(lock.getItemCode()))
                    .toList();
        }

        @Override
        public List<InventoryStockLockDO> findActiveBySource(String sourceType, Long sourceId) {
            return locks.stream()
                    .filter(lock -> Boolean.TRUE.equals(lock.getActive()))
                    .filter(lock -> lock.getSourceType().equals(sourceType))
                    .filter(lock -> lock.getSourceId().equals(sourceId))
                    .toList();
        }

        @Override
        public void save(InventoryStockLockDO stockLock) {
            stockLock.setId((long) locks.size() + NEXT_ID_OFFSET);
            locks.add(stockLock);
        }

        @Override
        public void update(InventoryStockLockDO stockLock) {
            // 对象引用已在内存列表中，更新由调用方直接完成。
        }
    }

    private static final class InMemoryBatchBalanceRepository implements InventoryBatchBalanceRepository {

        private final List<InventoryBatchBalanceDO> balances = new ArrayList<>();

        @Override
        public List<InventoryBatchBalanceDO> findByScopeOrdered(String scopeType, Long scopeId) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .toList();
        }

        @Override
        public List<InventoryBatchBalanceDO> findByScopeWarehouseAndItemOrdered(String scopeType,
                                                                                Long scopeId,
                                                                                String warehouseName,
                                                                                String itemCode) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .filter(balance -> balance.getWarehouseName().equals(warehouseName))
                    .filter(balance -> balance.getItemCode().equals(itemCode))
                    .sorted(Comparator.comparing(InventoryBatchBalanceDO::getBatchNo))
                    .toList();
        }

        @Override
        public List<InventoryBatchBalanceDO> findByScopeAndExpiryDateOnOrBefore(String scopeType,
                                                                                Long scopeId,
                                                                                LocalDate expiryDate) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .filter(balance -> balance.getExpiryDate() != null)
                    .filter(balance -> !balance.getExpiryDate().isAfter(expiryDate))
                    .toList();
        }

        @Override
        public Optional<InventoryBatchBalanceDO> lockByScopeWarehouseItemAndBatch(String scopeType,
                                                                                  Long scopeId,
                                                                                  String warehouseName,
                                                                                  String itemCode,
                                                                                  String batchNo) {
            return balances.stream()
                    .filter(balance -> matchesScope(balance, scopeType, scopeId))
                    .filter(balance -> balance.getWarehouseName().equals(warehouseName))
                    .filter(balance -> balance.getItemCode().equals(itemCode))
                    .filter(balance -> balance.getBatchNo().equals(batchNo))
                    .findFirst();
        }

        @Override
        public void save(InventoryBatchBalanceDO balance) {
            balance.setId((long) balances.size() + NEXT_ID_OFFSET);
            balances.add(balance);
        }

        @Override
        public void update(InventoryBatchBalanceDO balance) {
            // 对象引用已在内存列表中，更新由调用方直接完成。
        }

        private InventoryBatchBalanceDO require(String warehouseName, String itemCode, String batchNo) {
            return lockByScopeWarehouseItemAndBatch(SCOPE_TYPE, SCOPE_ID, warehouseName, itemCode, batchNo)
                    .orElseThrow(() -> new AssertionError("批次余额不存在"));
        }

        private boolean matchesScope(InventoryBatchBalanceDO balance, String scopeType, Long scopeId) {
            return balance.getScopeType().equals(scopeType) && balance.getScopeId().equals(scopeId);
        }
    }

    private static final class InMemoryBatchTransactionRepository implements InventoryBatchTransactionRepository {

        private final List<InventoryBatchTransactionDO> transactions = new ArrayList<>();

        @Override
        public void save(InventoryBatchTransactionDO transaction) {
            transaction.setId((long) transactions.size() + NEXT_ID_OFFSET);
            transactions.add(transaction);
        }

        @Override
        public List<InventoryBatchTransactionDO> findByScopeOrdered(String scopeType, Long scopeId) {
            return transactions.stream()
                    .filter(transaction -> transaction.getScopeType().equals(scopeType))
                    .filter(transaction -> transaction.getScopeId().equals(scopeId))
                    .toList();
        }

        @Override
        public List<InventoryBatchTransactionDO> findByScopeAndBusinessDateRange(String scopeType,
                                                                                 Long scopeId,
                                                                                 LocalDate startDate,
                                                                                 LocalDate endDate) {
            return findByScopeOrdered(scopeType, scopeId).stream()
                    .filter(transaction -> !transaction.getBusinessDate().isBefore(startDate))
                    .filter(transaction -> !transaction.getBusinessDate().isAfter(endDate))
                    .toList();
        }

        private List<InventoryBatchTransactionDO> saved() {
            return transactions;
        }
    }
}
