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
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
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
    private static final int THIRD_TRANSACTION_INDEX = 2;
    private static final int NEXT_ID_OFFSET = 1;
    private static final int YEAR = 2026;
    private static final int MONTH = 5;
    private static final int FIRST_DAY = 1;
    private static final int SECOND_DAY = 2;
    private static final int THIRD_DAY = 3;

    private InMemoryBalanceRepository balanceRepository;
    private InMemoryTransactionRepository transactionRepository;
    private InventoryStockMutationService service;

    @BeforeEach
    void setUp() {
        balanceRepository = new InMemoryBalanceRepository();
        transactionRepository = new InMemoryTransactionRepository();
        service = new InventoryStockMutationService(balanceRepository, transactionRepository);
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
                "LOSS_OUTBOUND",
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
                "LOSS_OUTBOUND",
                OPERATOR_ID
        ));

        Assertions.assertEquals("库存金额不足，无法完成库存扣减", exception.getMessage());
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
}
