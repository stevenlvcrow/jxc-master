package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InterOrgTransferDetailReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InterOrgTransferSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InventoryInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.InventoryProfitLossReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.OtherInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.PageData;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StagnantStockReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StockInoutSummaryReportRow;
import com.boboboom.jxc.inventory.application.service.InventorySupplementReportRows.StockTurnoverRateReportRow;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryPeriodOpeningRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningLineDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 库存报表补充业务。
 */
@Service
public class InventorySupplementReportApplicationService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int INVENTORY_QUANTITY_SCALE = 4;
    private static final int TURNOVER_RATE_SCALE = 4;
    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);
    private static final String STORE_TRANSFER_SOURCE_WAREHOUSE = "sourceWarehouse";
    private static final String STORE_TRANSFER_TARGET_WAREHOUSE = "targetWarehouse";

    private final OrgScopeService orgScopeService;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final InventoryCheckRepository inventoryCheckRepository;
    private final ItemProfileRepository itemProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryPeriodOpeningRepository inventoryPeriodOpeningRepository;
    private final DataScopeAccessService dataScopeAccessService;
    private final DictionaryLookupService dictionaryLookupService;
    private final ObjectMapper objectMapper;

    /** 库存补充报表业务服务，负责预警、周转、跨组织调拨等扩展报表计算。 */
    public InventorySupplementReportApplicationService(OrgScopeService orgScopeServiceValue,
                                                       InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                                       InventoryDocumentRepository inventoryDocumentRepositoryValue,
                                                       InventoryCheckRepository inventoryCheckRepositoryValue,
                                                       ItemProfileRepository itemProfileRepositoryValue,
                                                       UserAccountRepository userAccountRepositoryValue,
                                                       WarehouseRepository warehouseRepositoryValue,
                                                       InventoryTransactionRepository inventoryTransactionRepositoryValue,
                                                       InventoryPeriodOpeningRepository inventoryPeriodOpeningRepositoryValue,
                                                       DataScopeAccessService dataScopeAccessServiceValue,
                                                       DictionaryLookupService dictionaryLookupServiceValue,
                                                       ObjectMapper objectMapperValue) {
        this.orgScopeService = orgScopeServiceValue;
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.inventoryDocumentRepository = inventoryDocumentRepositoryValue;
        this.inventoryCheckRepository = inventoryCheckRepositoryValue;
        this.itemProfileRepository = itemProfileRepositoryValue;
        this.userAccountRepository = userAccountRepositoryValue;
        this.warehouseRepository = warehouseRepositoryValue;
        this.inventoryTransactionRepository = inventoryTransactionRepositoryValue;
        this.inventoryPeriodOpeningRepository = inventoryPeriodOpeningRepositoryValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
        this.objectMapper = objectMapperValue;
    }

    /** 查询滞销库存报表。 */
    public PageData<StagnantStockReportRow> stagnantStockReport(Integer pageNo,
                                                                Integer pageSize,
                                                                String warehouse,
                                                                String itemCode,
                                                                String itemCategory,
                                                                String itemStatus,
                                                                String stagnant,
                                                                String stagnantDaysGreaterThan,
                                                                String unitType,
                                                                String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String warehouseValue = trimNullable(warehouse);
        String itemCodeValue = trimNullable(itemCode);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemStatusValue = trimNullable(itemStatus);
        String stagnantValue = trimNullable(stagnant);
        Integer stagnantDaysThreshold = parseIntegerNullable(stagnantDaysGreaterThan);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, TransactionStats> statsMap = loadTransactionStats(scope);
        List<StagnantStockReportRow> rows = inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(balance -> toStagnantStockRow(scope, balance, itemProfiles, statsMap))
                .filter(row -> !StringUtils.hasText(warehouseValue) || row.warehouse().contains(warehouseValue))
                .filter(row -> !StringUtils.hasText(itemCodeValue) || row.itemCode().contains(itemCodeValue))
                .filter(row -> !StringUtils.hasText(itemCategoryValue) || Objects.equals(row.itemCategory(), itemCategoryValue))
                .filter(row -> !StringUtils.hasText(itemStatusValue) || Objects.equals(row.itemStatus(), itemStatusValue))
                .filter(row -> !StringUtils.hasText(stagnantValue) || Objects.equals(row.stagnant(), stagnantValue))
                .filter(row -> stagnantDaysThreshold == null || row.retainedDays() > stagnantDaysThreshold)
                .filter(row -> !StringUtils.hasText(unitTypeValue) || Objects.equals(row.unit(), unitTypeValue))
                .sorted(Comparator.comparing(StagnantStockReportRow::warehouse)
                        .thenComparing(StagnantStockReportRow::itemCode))
                .toList();
        return page(rows, safePageNo, safePageSize);
    }

    /** 查询库存盘盈盘亏报表。 */
    public PageData<InventoryProfitLossReportRow> inventoryProfitLossReport(Integer pageNo,
                                                                           Integer pageSize,
                                                                           String warehouse,
                                                                           String dateRangeStart,
                                                                           String dateRangeEnd,
                                                                           String itemCategory,
                                                                           String statisticsType,
                                                                           String itemKeyword,
                                                                           String checkType,
                                                                           String profitLossResult,
                                                                           String unitType,
                                                                           String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(dateRangeStart);
        LocalDate end = parseDateNullable(dateRangeEnd);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String statisticsTypeValue = trimNullable(statisticsType);
        String itemKeywordValue = trimNullable(itemKeyword);
        String checkTypeValue = trimNullable(checkType);
        String profitLossResultValue = normalizeProfitLossResult(profitLossResult);
        String unitTypeValue = trimNullable(unitType);

        List<InventoryProfitLossReportRow> rows = collectInventoryProfitLossRows(
                scope,
                start,
                end,
                warehouseValue,
                itemCategoryValue,
                statisticsTypeValue,
                itemKeywordValue,
                checkTypeValue,
                profitLossResultValue,
                unitTypeValue
        );
        rows.sort(Comparator.comparing(InventoryProfitLossReportRow::checkTime)
                .reversed()
                .thenComparing(InventoryProfitLossReportRow::checkDocumentNo)
                .thenComparing(InventoryProfitLossReportRow::itemCode));
        return page(rows, safePageNo, safePageSize);
    }

    private List<InventoryProfitLossReportRow> collectInventoryProfitLossRows(InventoryScope scope,
                                                                              LocalDate start,
                                                                              LocalDate end,
                                                                              String warehouseValue,
                                                                              String itemCategoryValue,
                                                                              String statisticsTypeValue,
                                                                              String itemKeywordValue,
                                                                              String checkTypeValue,
                                                                              String profitLossResultValue,
                                                                              String unitTypeValue) {
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        List<InventoryProfitLossReportRow> rows = new ArrayList<>();
        for (InventoryCheckKind kind : List.of(InventoryCheckKind.INVENTORY_CHECK, InventoryCheckKind.MULTI_INVENTORY_CHECK)) {
            collectInventoryProfitLossRowsByKind(scope, kind, itemProfiles, rows, start, end, warehouseValue,
                    itemCategoryValue, statisticsTypeValue, itemKeywordValue, checkTypeValue, profitLossResultValue, unitTypeValue);
        }
        return rows;
    }

    private void collectInventoryProfitLossRowsByKind(InventoryScope scope,
                                                      InventoryCheckKind kind,
                                                      Map<String, ItemProfileSnapshot> itemProfiles,
                                                      List<InventoryProfitLossReportRow> rows,
                                                      LocalDate start,
                                                      LocalDate end,
                                                      String warehouseValue,
                                                      String itemCategoryValue,
                                                      String statisticsTypeValue,
                                                      String itemKeywordValue,
                                                      String checkTypeValue,
                                                      String profitLossResultValue,
                                                      String unitTypeValue) {
        List<InventoryCheckHeader> headers = loadVisibleCheckHeaders(kind, scope);
        Map<Long, List<InventoryCheckLine>> lineMap = inventoryCheckRepository.findLinesByHeaderIds(kind, headers.stream().map(InventoryCheckHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryCheckLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        for (InventoryCheckHeader header : headers) {
            appendInventoryProfitLossRowsForHeader(scope, kind, header, lineMap, itemProfiles, rows, start, end,
                    warehouseValue, itemCategoryValue, statisticsTypeValue, itemKeywordValue, checkTypeValue, profitLossResultValue, unitTypeValue);
        }
    }

    private void appendInventoryProfitLossRowsForHeader(InventoryScope scope,
                                                        InventoryCheckKind kind,
                                                        InventoryCheckHeader header,
                                                        Map<Long, List<InventoryCheckLine>> lineMap,
                                                        Map<String, ItemProfileSnapshot> itemProfiles,
                                                        List<InventoryProfitLossReportRow> rows,
                                                        LocalDate start,
                                                        LocalDate end,
                                                        String warehouseValue,
                                                        String itemCategoryValue,
                                                        String statisticsTypeValue,
                                                        String itemKeywordValue,
                                                        String checkTypeValue,
                                                        String profitLossResultValue,
                                                        String unitTypeValue) {
        if (!matchesInventoryProfitLossHeader(header, start, end, warehouseValue, checkTypeValue)) {
            return;
        }
        for (InventoryCheckLine line : lineMap.getOrDefault(header.getId(), List.of())) {
            ItemProfileSnapshot itemProfile = itemProfiles.get(line.getItemCode());
            InventoryProfitLossReportRow row = toInventoryProfitLossRow(scope, kind, header, line, itemProfile);
            if (matchesInventoryProfitLossRow(row, itemCategoryValue, statisticsTypeValue, itemKeywordValue, profitLossResultValue, unitTypeValue)) {
                rows.add(row);
            }
        }
    }

    private boolean matchesInventoryProfitLossHeader(InventoryCheckHeader header,
                                                     LocalDate start,
                                                     LocalDate end,
                                                     String warehouseValue,
                                                     String checkTypeValue) {
        return matchDate(header.getCheckDate(), start, end)
                && matchesInventoryProfitLossWarehouse(header, warehouseValue)
                && matchesInventoryProfitLossCheckType(header, checkTypeValue);
    }

    private boolean matchesInventoryProfitLossRow(InventoryProfitLossReportRow row,
                                                  String itemCategoryValue,
                                                  String statisticsTypeValue,
                                                  String itemKeywordValue,
                                                  String profitLossResultValue,
                                                  String unitTypeValue) {
        return (!StringUtils.hasText(itemCategoryValue) || Objects.equals(row.itemCategory(), itemCategoryValue))
                && (!StringUtils.hasText(statisticsTypeValue) || Objects.equals(row.statisticsType(), statisticsTypeValue))
                && matchesInventoryProfitLossItem(row, itemKeywordValue)
                && matchesProfitLossResult(row.profitLossResult(), profitLossResultValue)
                && (!StringUtils.hasText(unitTypeValue) || Objects.equals(row.unit(), unitTypeValue));
    }

    private boolean matchesProfitLossResult(String rowResult, String resultCode) {
        return !StringUtils.hasText(resultCode) || Objects.equals(profitLossResultCode(rowResult), resultCode);
    }

    private boolean matchesInventoryProfitLossWarehouse(InventoryCheckHeader header, String warehouseValue) {
        return !StringUtils.hasText(warehouseValue) || Objects.equals(defaultIfBlank(header.getWarehouseName(), ""), warehouseValue);
    }

    private boolean matchesInventoryProfitLossCheckType(InventoryCheckHeader header, String checkTypeValue) {
        return !StringUtils.hasText(checkTypeValue) || Objects.equals(defaultIfBlank(header.getCheckRangeType(), ""), checkTypeValue);
    }

    private boolean matchesInventoryProfitLossItem(InventoryProfitLossReportRow row, String itemKeywordValue) {
        return !StringUtils.hasText(itemKeywordValue)
                || row.itemCode().contains(itemKeywordValue)
                || row.itemName().contains(itemKeywordValue);
    }

    /** 查询库存进出汇总报表。 */
    public PageData<InventoryInoutSummaryReportRow> inventoryInoutSummaryReport(Integer pageNo,
                                                                                Integer pageSize,
                                                                                String statisticDimension,
                                                                                String warehouse,
                                                                                String warehouseType,
                                                                                String startDate,
                                                                                String endDate,
                                                                                String periodType,
                                                                                String periodStartDate,
                                                                                String itemCode,
                                                                                String itemCategory,
                                                                                String statisticType,
                                                                                String itemStatus,
                                                                                String inoutType,
                                                                                String unitType,
                                                                                String hideNoInout,
                                                                                String queryScheme,
                                                                                String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        PeriodReportRange periodRange = resolvePeriodReportRange(periodType, periodStartDate, start, end);
        String statisticDimensionValue = trimNullable(statisticDimension);
        String warehouseValue = trimNullable(warehouse);
        String warehouseTypeValue = trimNullable(warehouseType);
        String itemCodeValue = trimNullable(itemCode);
        String itemCategoryValue = trimNullable(itemCategory);
        String statisticTypeValue = trimNullable(statisticType);
        String itemStatusValue = trimNullable(itemStatus);
        String inoutTypeValue = trimNullable(inoutType);
        String unitTypeValue = trimNullable(unitType);
        boolean hideEmpty = Objects.equals(trimNullable(hideNoInout), "true") || Objects.equals(trimNullable(hideNoInout), "是");

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, TxnSummary> txnSummaryMap = loadTxnSummary(scope, periodRange.startDate(), periodRange.endDate(),
                inoutTypeValue, periodRange.periodType(), periodRange.periodStartDate(), itemProfiles);
        Map<String, InventoryProfitLossAgg> checkAggMap = loadInventoryCheckAgg(scope, periodRange.startDate(), periodRange.endDate());

        List<InventoryInoutSummaryReportRow> rows = collectInventoryInoutSummaryRows(scope, statisticDimensionValue,
                warehouseValue, itemCodeValue, itemCategoryValue, statisticTypeValue, itemStatusValue, unitTypeValue,
                inoutTypeValue, hideEmpty, itemProfiles, txnSummaryMap, checkAggMap);
        rows.sort(Comparator.comparing(InventoryInoutSummaryReportRow::warehouse)
                .thenComparing(InventoryInoutSummaryReportRow::itemCode));
        if ("物品类别".equals(statisticDimensionValue)) {
            rows = aggregateInventoryInoutByCategory(rows);
        }
        if (StringUtils.hasText(inoutTypeValue)) {
            rows = rows.stream().filter(row -> Objects.equals(row.inoutType(), inoutTypeValue)).toList();
        }
        return page(rows, safePageNo, safePageSize);
    }

    private List<InventoryInoutSummaryReportRow> collectInventoryInoutSummaryRows(InventoryScope scope,
                                                                                  String statisticDimensionValue,
                                                                                  String warehouseValue,
                                                                                  String itemCodeValue,
                                                                                  String itemCategoryValue,
                                                                                  String statisticTypeValue,
                                                                                  String itemStatusValue,
                                                                                  String unitTypeValue,
                                                                                  String inoutTypeValue,
                                                                                  boolean hideEmpty,
                                                                                  Map<String, ItemProfileSnapshot> itemProfiles,
                                                                                  Map<String, TxnSummary> txnSummaryMap,
                                                                                  Map<String, InventoryProfitLossAgg> checkAggMap) {
        List<InventoryInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryBalanceDO balance : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
            if (item == null || !matchesInventoryInoutSummaryItem(item, itemCodeValue, itemCategoryValue,
                    statisticTypeValue, itemStatusValue, unitTypeValue)) {
                continue;
            }
            InventoryInoutSummaryReportRow row = buildInventoryInoutSummaryRow(scope, balance, item, statisticDimensionValue,
                    inoutTypeValue, txnSummaryMap, checkAggMap);
            if (!matchesInventoryInoutSummaryRow(row, warehouseValue, inoutTypeValue, hideEmpty)) {
                continue;
            }
            rows.add(row);
        }
        return rows;
    }

    private InventoryInoutSummaryReportRow buildInventoryInoutSummaryRow(InventoryScope scope,
                                                                         InventoryBalanceDO balance,
                                                                         ItemProfileSnapshot item,
                                                                         String statisticDimensionValue,
                                                                         String inoutTypeValue,
                                                                         Map<String, TxnSummary> txnSummaryMap,
                                                                         Map<String, InventoryProfitLossAgg> checkAggMap) {
        String rowKey = summaryKey(balance.getWarehouseName(), item, statisticDimensionValue);
        TxnSummary txnSummary = txnSummaryMap.getOrDefault(rowKey, new TxnSummary());
        InventoryProfitLossAgg checkAgg = checkAggMap.getOrDefault(rowKey, new InventoryProfitLossAgg());
        BigDecimal openingQty = txnSummary.openingQty;
        BigDecimal inboundQty = txnSummary.inboundQty;
        BigDecimal outboundQty = txnSummary.outboundQty;
        BigDecimal closingQty = txnSummary.closingQty == null ? defaultQuantity(balance.getQuantity()) : txnSummary.closingQty;
        BigDecimal openingCost = txnSummary.openingAmount;
        BigDecimal inboundCost = txnSummary.inboundAmount;
        BigDecimal outboundCost = txnSummary.outboundAmount;
        BigDecimal closingCost = txnSummary.closingAmount.compareTo(BigDecimal.ZERO) == 0
                ? defaultMoney(balance.getCostAmount()) : txnSummary.closingAmount;
        BigDecimal inventoryCheckQty = checkAgg.inventoryCheckQty;
        BigDecimal inventoryProfitLossQty = checkAgg.inventoryProfitLossQty;
        BigDecimal unitCost = averageCost(closingCost, closingQty);
        BigDecimal inventoryProfitLossCostAmount = inventoryProfitLossQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal inventoryCheckCostAmount = inventoryCheckQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal closingCheckDiffQty = closingQty.subtract(inventoryCheckQty)
                .setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
        BigDecimal returnDifferenceQty = txnSummary.returnDifferenceQty;
        return new InventoryInoutSummaryReportRow(
                summaryRowId(balance.getWarehouseName(), item.itemCode(), rowKey),
                item.itemCode(), item.itemName(), item.spec(), item.category(), item.statisticsType(), item.stockUnit(),
                defaultOrgName(scope), defaultOrgCode(scope), defaultIfBlank(balance.getWarehouseName(), ""),
                resolveWarehouseType(scope, balance.getWarehouseName()),
                openingQty, openingCost, averageCost(openingCost, openingQty),
                inboundQty, inboundCost, averageCost(inboundCost, inboundQty),
                outboundQty, outboundCost, averageCost(outboundCost, outboundQty),
                closingQty, closingCost, averageCost(closingCost, closingQty),
                inventoryProfitLossQty, inventoryProfitLossCostAmount, inventoryProfitLossCostAmount,
                inventoryCheckQty, inventoryCheckCostAmount,
                closingCheckDiffQty, closingCheckDiffQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP),
                returnDifferenceQty, returnDifferenceQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP),
                defaultIfBlank(inoutTypeValue, "全部")
        );
    }

    private BigDecimal averageCost(BigDecimal amount, BigDecimal qty) {
        return qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : amount.divide(qty, 2, RoundingMode.HALF_UP);
    }

    private boolean matchesInventoryInoutSummaryRow(InventoryInoutSummaryReportRow row,
                                                    String warehouseValue,
                                                    String inoutTypeValue,
                                                    boolean hideEmpty) {
        return (!StringUtils.hasText(inoutTypeValue) || row.hasInoutMovement())
                && (!hideEmpty || !row.isEmpty())
                && (!StringUtils.hasText(warehouseValue) || Objects.equals(row.warehouse(), warehouseValue));
    }

    private boolean matchesInventoryInoutSummaryItem(ItemProfileSnapshot item,
                                                     String itemCodeValue,
                                                     String itemCategoryValue,
                                                     String statisticTypeValue,
                                                     String itemStatusValue,
                                                     String unitTypeValue) {
        return (!StringUtils.hasText(itemCodeValue) || Objects.equals(item.itemCode(), itemCodeValue))
                && (!StringUtils.hasText(itemCategoryValue) || Objects.equals(item.category(), itemCategoryValue))
                && (!StringUtils.hasText(statisticTypeValue) || Objects.equals(item.statisticsType(), statisticTypeValue))
                && (!StringUtils.hasText(itemStatusValue) || Objects.equals(item.status(), itemStatusValue))
                && (!StringUtils.hasText(unitTypeValue) || Objects.equals(item.stockUnit(), unitTypeValue));
    }

    /** 查询库存出入库汇总报表。 */
    public PageData<StockInoutSummaryReportRow> stockInoutSummaryReport(Integer pageNo,
                                                                       Integer pageSize,
                                                                       String statisticMode,
                                                                       String dateDimension,
                                                                       String startDate,
                                                                       String endDate,
                                                                       String periodType,
                                                                       String periodStartDate,
                                                                       String statisticDimension,
                                                                       String warehouse,
                                                                       String warehouseType,
                                                                       String targetStore,
                                                                       String itemKeyword,
                                                                       String itemCategory,
                                                                       String statisticType,
                                                                       String itemStatus,
                                                                       String inoutType,
                                                                       String inoutDirection,
                                                                       String oppositeOrg,
                                                                       String unitType,
                                                                       String queryScheme,
                                                                       String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        PeriodReportRange periodRange = resolvePeriodReportRange(periodType, periodStartDate, start, end);
        String statisticModeValue = trimNullable(statisticMode);
        String targetStoreValue = trimNullable(targetStore);
        String warehouseValue = trimNullable(warehouse);
        String warehouseTypeValue = trimNullable(warehouseType);
        String itemKeywordValue = trimNullable(itemKeyword);
        String itemCategoryValue = trimNullable(itemCategory);
        String statisticTypeValue = trimNullable(statisticType);
        String itemStatusValue = trimNullable(itemStatus);
        String inoutTypeValue = trimNullable(inoutType);
        String inoutDirectionValue = trimNullable(inoutDirection);
        String oppositeOrgValue = trimNullable(oppositeOrg);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        requireApprovedPeriodOpenings(scope, periodRange.periodType(), periodRange.periodStartDate(), warehouseValue);
        Map<String, LedgerLineSnapshot> ledgerMap = loadLedgerLineSnapshotMap(scope);
        StockInoutSummaryFilter filter = new StockInoutSummaryFilter(statisticModeValue, targetStoreValue,
                warehouseValue, warehouseTypeValue, itemKeywordValue, itemCategoryValue, statisticTypeValue,
                itemStatusValue, inoutTypeValue, inoutDirectionValue, oppositeOrgValue, unitTypeValue);
        List<StockInoutSummaryReportRow> rows = collectStockInoutSummaryRows(scope, periodRange.startDate(),
                periodRange.endDate(), filter, itemProfiles, ledgerMap);
        rows = aggregateStockInoutRows(rows);
        return page(rows, safePageNo, safePageSize);
    }

    private List<StockInoutSummaryReportRow> collectStockInoutSummaryRows(InventoryScope scope,
                                                                          LocalDate start,
                                                                          LocalDate end,
                                                                          StockInoutSummaryFilter filter,
                                                                          Map<String, ItemProfileSnapshot> itemProfiles,
                                                                          Map<String, LedgerLineSnapshot> ledgerMap) {
        List<StockInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : stockInoutSummaryTypes()) {
            appendStockInoutSummaryRowsForType(rows, scope, type, start, end, filter, itemProfiles, ledgerMap);
        }
        return rows;
    }

    private void appendStockInoutSummaryRowsForType(List<StockInoutSummaryReportRow> rows,
                                                    InventoryScope scope,
                                                    InventoryDocumentType type,
                                                    LocalDate start,
                                                    LocalDate end,
                                                    StockInoutSummaryFilter filter,
                                                    Map<String, ItemProfileSnapshot> itemProfiles,
                                                    Map<String, LedgerLineSnapshot> ledgerMap) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type,
                headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        for (InventoryDocumentHeader header : headers) {
            if (!isApprovedDocument(header)) {
                continue;
            }
            appendStockInoutSummaryRowsForHeader(rows, scope, type, header, lineMap, start, end, filter, itemProfiles, ledgerMap);
        }
    }

    private void appendStockInoutSummaryRowsForHeader(List<StockInoutSummaryReportRow> rows,
                                                      InventoryScope scope,
                                                      InventoryDocumentType type,
                                                      InventoryDocumentHeader header,
                                                      Map<Long, List<InventoryDocumentLine>> lineMap,
                                                      LocalDate start,
                                                      LocalDate end,
                                                      StockInoutSummaryFilter filter,
                                                      Map<String, ItemProfileSnapshot> itemProfiles,
                                                      Map<String, LedgerLineSnapshot> ledgerMap) {
        if (!matchesStockInoutSummaryHeader(header, start, end, filter)) {
            return;
        }
        for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
            ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
            if (item == null || !matchesStockInoutSummaryLine(line, item, filter)) {
                continue;
            }
            LedgerLineSnapshot ledger = requireLedgerLineSnapshot(ledgerMap, type.getBusinessCode(), header.getId(), line.getId());
            if (!matchDate(ledger.businessDate(), start, end)) {
                continue;
            }
            StockInoutSummaryReportRow row = buildStockInoutSummaryRow(scope, type, header, line, item, ledger);
            if (matchesStockInoutSummaryRow(row, type, filter)) {
                rows.add(row);
            }
        }
    }

    private boolean matchesStockInoutSummaryHeader(InventoryDocumentHeader header,
                                                   LocalDate start,
                                                   LocalDate end,
                                                   StockInoutSummaryFilter filter) {
        return matchesStockInoutTargetStore(header, filter.targetStore());
    }

    private boolean matchesStockInoutTargetStore(InventoryDocumentHeader header, String targetStore) {
        return !StringUtils.hasText(targetStore)
                || Objects.equals(header.getPrimaryName(), targetStore)
                || Objects.equals(header.getCounterpartyName(), targetStore)
                || Objects.equals(header.getSecondaryName(), targetStore);
    }

    private boolean matchesStockInoutSummaryLine(InventoryDocumentLine line,
                                                 ItemProfileSnapshot item,
                                                 StockInoutSummaryFilter filter) {
        return matchesStockInoutItemKeyword(line, filter.itemKeyword())
                && (!StringUtils.hasText(filter.itemCategory()) || Objects.equals(item.category(), filter.itemCategory()))
                && (!StringUtils.hasText(filter.statisticType()) || Objects.equals(item.statisticsType(), filter.statisticType()))
                && (!StringUtils.hasText(filter.itemStatus()) || Objects.equals(item.status(), filter.itemStatus()))
                && (!StringUtils.hasText(filter.unitType()) || Objects.equals(item.stockUnit(), filter.unitType()));
    }

    private boolean matchesStockInoutItemKeyword(InventoryDocumentLine line, String itemKeyword) {
        return !StringUtils.hasText(itemKeyword)
                || containsIgnoreCase(line.getItemCode(), itemKeyword)
                || containsIgnoreCase(line.getItemName(), itemKeyword)
                || containsIgnoreCase(line.getSpec(), itemKeyword);
    }

    private boolean matchesStockInoutSummaryRow(StockInoutSummaryReportRow row,
                                                InventoryDocumentType type,
                                                StockInoutSummaryFilter filter) {
        return matchesStockInoutStatisticMode(row, filter.statisticMode())
                && matchesStockInoutWarehouse(row, filter.warehouse())
                && matchesStockInoutWarehouseType(row, filter.warehouseType())
                && matchesStockInoutType(row, filter.inoutType())
                && matchesStockInoutDirection(type, filter.inoutDirection())
                && matchesStockInoutOppositeOrg(row, filter.oppositeOrg());
    }

    private boolean matchesStockInoutStatisticMode(StockInoutSummaryReportRow row, String statisticMode) {
        return !StringUtils.hasText(statisticMode) || Objects.equals(row.statisticMode(), statisticMode);
    }

    private boolean matchesStockInoutWarehouse(StockInoutSummaryReportRow row, String warehouse) {
        return !StringUtils.hasText(warehouse) || Objects.equals(row.warehouse(), warehouse);
    }

    private boolean matchesStockInoutWarehouseType(StockInoutSummaryReportRow row, String warehouseType) {
        return !StringUtils.hasText(warehouseType) || Objects.equals(row.warehouseType(), warehouseType);
    }

    private boolean matchesStockInoutType(StockInoutSummaryReportRow row, String inoutType) {
        return !StringUtils.hasText(inoutType) || Objects.equals(row.inoutType(), inoutType);
    }

    private boolean matchesStockInoutDirection(InventoryDocumentType type, String inoutDirection) {
        return !StringUtils.hasText(inoutDirection) || Objects.equals(resolveStockDirection(type), inoutDirection);
    }

    private boolean matchesStockInoutOppositeOrg(StockInoutSummaryReportRow row, String oppositeOrg) {
        return !StringUtils.hasText(oppositeOrg)
                || containsIgnoreCase(row.oppositeOrg(), oppositeOrg)
                || containsIgnoreCase(row.oppositeWarehouse(), oppositeOrg);
    }

    private List<InventoryDocumentType> stockInoutSummaryTypes() {
        return List.of(InventoryDocumentType.PURCHASE_INBOUND, InventoryDocumentType.PURCHASE_RETURN_OUTBOUND,
                InventoryDocumentType.DEPARTMENT_PICKING, InventoryDocumentType.DEPARTMENT_RETURN,
                InventoryDocumentType.STOCK_TRANSFER, InventoryDocumentType.STOCK_TRANSFER_INBOUND,
                InventoryDocumentType.DEPARTMENT_TRANSFER, InventoryDocumentType.DAMAGE_OUTBOUND,
                InventoryDocumentType.OTHER_INBOUND, InventoryDocumentType.OTHER_OUTBOUND,
                InventoryDocumentType.PRODUCTION_INBOUND, InventoryDocumentType.CUSTOMER_SALES_OUTBOUND,
                InventoryDocumentType.CUSTOMER_RETURN_INBOUND, InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND,
                InventoryDocumentType.STORE_TRANSFER, InventoryDocumentType.STOCK_TRANSFER_OUTBOUND);
    }

    /** 查询其他出入库汇总报表。 */
    public PageData<OtherInoutSummaryReportRow> otherInoutSummaryReport(Integer pageNo,
                                                                        Integer pageSize,
                                                                        String startDate,
                                                                        String endDate,
                                                                        String periodType,
                                                                        String periodStartDate,
                                                                        String warehouse,
                                                                        String itemCategory,
                                                                        String itemCode,
                                                                        String inoutType,
                                                                        String reasonType,
                                                                        String itemStatus,
                                                                        String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        PeriodReportRange periodRange = resolvePeriodReportRange(periodType, periodStartDate, start, end);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String inoutTypeValue = trimNullable(inoutType);
        String reasonTypeValue = trimNullable(reasonType);
        String itemStatusValue = trimNullable(itemStatus);
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        requireApprovedPeriodOpenings(scope, periodRange.periodType(), periodRange.periodStartDate(), warehouseValue);
        Map<String, LedgerLineSnapshot> ledgerMap = loadLedgerLineSnapshotMap(scope);
        OtherInoutSummaryFilter filter = new OtherInoutSummaryFilter(warehouseValue, itemCategoryValue,
                itemCodeValue, inoutTypeValue, reasonTypeValue, itemStatusValue);

        List<OtherInoutSummaryReportRow> rows = collectOtherInoutSummaryRows(scope, periodRange.startDate(),
                periodRange.endDate(), filter, itemProfiles, ledgerMap);
        rows = aggregateOtherInoutRows(rows);
        return page(rows, safePageNo, safePageSize);
    }

    private List<OtherInoutSummaryReportRow> collectOtherInoutSummaryRows(InventoryScope scope,
                                                                          LocalDate start,
                                                                          LocalDate end,
                                                                          OtherInoutSummaryFilter filter,
                                                                          Map<String, ItemProfileSnapshot> itemProfiles,
                                                                          Map<String, LedgerLineSnapshot> ledgerMap) {
        List<OtherInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : List.of(InventoryDocumentType.OTHER_INBOUND,
                InventoryDocumentType.OTHER_OUTBOUND, InventoryDocumentType.DAMAGE_OUTBOUND,
                InventoryDocumentType.PRODUCTION_INBOUND)) {
            appendOtherInoutSummaryRowsForType(rows, scope, type, start, end, filter, itemProfiles, ledgerMap);
        }
        return rows;
    }

    private void appendOtherInoutSummaryRowsForType(List<OtherInoutSummaryReportRow> rows,
                                                    InventoryScope scope,
                                                    InventoryDocumentType type,
                                                    LocalDate start,
                                                    LocalDate end,
                                                    OtherInoutSummaryFilter filter,
                                                    Map<String, ItemProfileSnapshot> itemProfiles,
                                                    Map<String, LedgerLineSnapshot> ledgerMap) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type,
                headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        for (InventoryDocumentHeader header : headers) {
            if (!isApprovedDocument(header)) {
                continue;
            }
            appendOtherInoutSummaryRowsForHeader(rows, type, header, lineMap, start, end, filter, itemProfiles, ledgerMap);
        }
    }

    private void appendOtherInoutSummaryRowsForHeader(List<OtherInoutSummaryReportRow> rows,
                                                      InventoryDocumentType type,
                                                      InventoryDocumentHeader header,
                                                      Map<Long, List<InventoryDocumentLine>> lineMap,
                                                      LocalDate start,
                                                      LocalDate end,
                                                      OtherInoutSummaryFilter filter,
                                                      Map<String, ItemProfileSnapshot> itemProfiles,
                                                      Map<String, LedgerLineSnapshot> ledgerMap) {
        String resolvedWarehouse = defaultIfBlank(header.getPrimaryName(), "");
        if (StringUtils.hasText(filter.warehouse()) && !Objects.equals(resolvedWarehouse, filter.warehouse())) {
            return;
        }
        for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
            ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
            if (item == null || !matchesOtherInoutSummaryItem(item, filter)) {
                continue;
            }
            LedgerLineSnapshot ledger = requireLedgerLineSnapshot(ledgerMap, type.getBusinessCode(), header.getId(), line.getId());
            if (!matchDate(ledger.businessDate(), start, end)) {
                continue;
            }
            OtherInoutSummaryReportRow row = buildOtherInoutSummaryRow(type, header, line, item, ledger, resolvedWarehouse);
            if (matchesOtherInoutSummaryRow(row, filter)) {
                rows.add(row);
            }
        }
    }

    private boolean matchesOtherInoutSummaryItem(ItemProfileSnapshot item, OtherInoutSummaryFilter filter) {
        return (!StringUtils.hasText(filter.itemCategory()) || Objects.equals(item.category(), filter.itemCategory()))
                && (!StringUtils.hasText(filter.itemCode()) || Objects.equals(item.itemCode(), filter.itemCode()))
                && (!StringUtils.hasText(filter.itemStatus()) || Objects.equals(item.status(), filter.itemStatus()));
    }

    private boolean matchesOtherInoutSummaryRow(OtherInoutSummaryReportRow row, OtherInoutSummaryFilter filter) {
        return (!StringUtils.hasText(filter.inoutType()) || Objects.equals(row.inoutType(), filter.inoutType()))
                && (!StringUtils.hasText(filter.reasonType()) || Objects.equals(row.reasonType(), filter.reasonType()));
    }

    private OtherInoutSummaryReportRow buildOtherInoutSummaryRow(InventoryDocumentType type,
                                                                 InventoryDocumentHeader header,
                                                                 InventoryDocumentLine line,
                                                                 ItemProfileSnapshot item,
                                                                 LedgerLineSnapshot ledger,
                                                                 String resolvedWarehouse) {
        String rowInoutType = resolveOtherInoutType(type, header, line);
        String rowReasonType = resolveOtherReasonType(header, line, type);
        BigDecimal qty = ledger.quantity().abs();
        BigDecimal amount = ledger.amount().abs();
        return new OtherInoutSummaryReportRow(summaryRowId(resolvedWarehouse, item.itemCode(), rowInoutType + rowReasonType),
                item.itemCode(), item.itemName(), item.spec(), item.category(), item.stockUnit(), resolvedWarehouse,
                rowInoutType, rowReasonType, qty, amount);
    }

    /** 查询跨组织调拨明细报表。 */
    public PageData<InterOrgTransferDetailReportRow> interOrgTransferDetailReport(Integer pageNo,
                                                                                 Integer pageSize,
                                                                                 String statisticMode,
                                                                                 String dateType,
                                                                                 String startDate,
                                                                                 String endDate,
                                                                                 String targetStore,
                                                                                 String sourceStore,
                                                                                 String itemName,
                                                                                 String itemCategory,
                                                                                 String sourceWarehouse,
                                                                                 String targetWarehouse,
                                                                                 String documentStatus,
                                                                                 String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        String statisticModeValue = trimNullable(statisticMode);
        String targetStoreValue = trimNullable(targetStore);
        String sourceStoreValue = trimNullable(sourceStore);
        String itemNameValue = trimNullable(itemName);
        String itemCategoryValue = trimNullable(itemCategory);
        String sourceWarehouseValue = trimNullable(sourceWarehouse);
        String targetWarehouseValue = trimNullable(targetWarehouse);
        String documentStatusValue = trimNullable(documentStatus);
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, LedgerLineSnapshot> ledgerMap = loadLedgerLineSnapshotMap(scope);
        InterOrgTransferDetailFilter filter = new InterOrgTransferDetailFilter(statisticModeValue, targetStoreValue,
                sourceStoreValue, itemNameValue, itemCategoryValue, sourceWarehouseValue, targetWarehouseValue,
                documentStatusValue);
        List<InterOrgTransferDetailReportRow> rows = collectInterOrgTransferDetailRows(scope, start, end, filter, itemProfiles, ledgerMap);
        rows.sort(Comparator.comparing(InterOrgTransferDetailReportRow::transferDate).reversed().thenComparing(InterOrgTransferDetailReportRow::transferNo));
        return page(rows, safePageNo, safePageSize);
    }

    private List<InterOrgTransferDetailReportRow> collectInterOrgTransferDetailRows(InventoryScope scope,
                                                                                    LocalDate start,
                                                                                    LocalDate end,
                                                                                    InterOrgTransferDetailFilter filter,
                                                                                    Map<String, ItemProfileSnapshot> itemProfiles,
                                                                                    Map<String, LedgerLineSnapshot> ledgerMap) {
        List<InterOrgTransferDetailReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : List.of(InventoryDocumentType.STOCK_TRANSFER,
                InventoryDocumentType.STOCK_TRANSFER_INBOUND, InventoryDocumentType.STORE_TRANSFER,
                InventoryDocumentType.STOCK_TRANSFER_OUTBOUND, InventoryDocumentType.DEPARTMENT_TRANSFER)) {
            appendInterOrgTransferDetailRowsForType(rows, scope, type, start, end, filter, itemProfiles, ledgerMap);
        }
        return rows;
    }

    private void appendInterOrgTransferDetailRowsForType(List<InterOrgTransferDetailReportRow> rows,
                                                         InventoryScope scope,
                                                         InventoryDocumentType type,
                                                         LocalDate start,
                                                         LocalDate end,
                                                         InterOrgTransferDetailFilter filter,
                                                         Map<String, ItemProfileSnapshot> itemProfiles,
                                                         Map<String, LedgerLineSnapshot> ledgerMap) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type,
                headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        for (InventoryDocumentHeader header : headers) {
            if (!isApprovedDocument(header)) {
                continue;
            }
            appendInterOrgTransferDetailRowsForHeader(rows, scope, type, header, lineMap, start, end, filter, itemProfiles, ledgerMap);
        }
    }

    private void appendInterOrgTransferDetailRowsForHeader(List<InterOrgTransferDetailReportRow> rows,
                                                           InventoryScope scope,
                                                           InventoryDocumentType type,
                                                           InventoryDocumentHeader header,
                                                           Map<Long, List<InventoryDocumentLine>> lineMap,
                                                           LocalDate start,
                                                           LocalDate end,
                                                           InterOrgTransferDetailFilter filter,
                                                           Map<String, ItemProfileSnapshot> itemProfiles,
                                                           Map<String, LedgerLineSnapshot> ledgerMap) {
        String resolvedSource = defaultIfBlank(header.getPrimaryName(), "");
        String resolvedTarget = defaultIfBlank(header.getSecondaryName(), defaultIfBlank(header.getCounterpartyName(), ""));
        if (!matchesInterOrgTransferHeader(header, resolvedSource, resolvedTarget, start, end, filter)) {
            return;
        }
        for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
            ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
            if (item == null || !matchesInterOrgTransferLine(line, item, filter)) {
                continue;
            }
            LedgerLineSnapshot ledger = requireLedgerLineSnapshot(ledgerMap, type.getBusinessCode(), header.getId(), line.getId());
            InterOrgTransferDetailReportRow row = buildInterOrgTransferDetailRow(scope, type, header, line, item, ledger,
                    resolvedSource, resolvedTarget);
            if (!StringUtils.hasText(filter.statisticMode()) || Objects.equals(row.statisticMode(), filter.statisticMode())) {
                rows.add(row);
            }
        }
    }

    private boolean matchesInterOrgTransferHeader(InventoryDocumentHeader header,
                                                  String resolvedSource,
                                                  String resolvedTarget,
                                                  LocalDate start,
                                                  LocalDate end,
                                                  InterOrgTransferDetailFilter filter) {
        return matchDate(header.getDocumentDate(), start, end)
                && matchesInterOrgTransferDocumentStatus(header, filter.documentStatus())
                && (!StringUtils.hasText(filter.sourceStore()) || containsIgnoreCase(resolvedSource, filter.sourceStore()))
                && (!StringUtils.hasText(filter.targetStore()) || containsIgnoreCase(resolvedTarget, filter.targetStore()))
                && (!StringUtils.hasText(filter.sourceWarehouse()) || containsIgnoreCase(resolvedSource, filter.sourceWarehouse()))
                && (!StringUtils.hasText(filter.targetWarehouse()) || containsIgnoreCase(resolvedTarget, filter.targetWarehouse()));
    }

    private boolean matchesInterOrgTransferDocumentStatus(InventoryDocumentHeader header, String documentStatus) {
        return !StringUtils.hasText(documentStatus) || Objects.equals(defaultIfBlank(header.getStatus(), ""), documentStatus);
    }

    private boolean matchesInterOrgTransferLine(InventoryDocumentLine line,
                                                ItemProfileSnapshot item,
                                                InterOrgTransferDetailFilter filter) {
        return (!StringUtils.hasText(filter.itemName()) || containsIgnoreCase(line.getItemName(), filter.itemName()))
                && (!StringUtils.hasText(filter.itemCategory()) || Objects.equals(item.category(), filter.itemCategory()));
    }

    /** 查询跨组织调拨汇总报表。 */
    public PageData<InterOrgTransferSummaryReportRow> interOrgTransferSummaryReport(Integer pageNo,
                                                                                    Integer pageSize,
                                                                                    String statisticMode,
                                                                                    String dateType,
                                                                                    String startDate,
                                                                                    String endDate,
                                                                                    String statisticDimension,
                                                                                    String targetStore,
                                                                                    String itemKeyword,
                                                                                    String itemCategory,
        String unitType,
        String queryScheme,
        String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        InterOrgTransferDetailFilter filter = new InterOrgTransferDetailFilter(
                trimNullable(statisticMode),
                trimNullable(targetStore),
                null,
                trimNullable(itemKeyword),
                trimNullable(itemCategory),
                null,
                null,
                null
        );
        List<InterOrgTransferDetailReportRow> detailRows = collectInterOrgTransferDetailRows(scope, start, end, filter,
                loadItemProfiles(scope), loadLedgerLineSnapshotMap(scope));
        List<InterOrgTransferSummaryReportRow> rows = aggregateInterOrgTransferSummaryRows(detailRows);
        return page(rows, safePageNo, safePageSize);
    }

    /** 查询库存周转率报表。 */
    public PageData<StockTurnoverRateReportRow> stockTurnoverRateReport(Integer pageNo,
                                                                        Integer pageSize,
                                                                        String statisticDimension,
                                                                        String statisticMethod,
                                                                        String startDate,
                                                                        String endDate,
                                                                        String periodType,
                                                                        String periodStartDate,
                                                                        String warehouse,
                                                                        String itemCategory,
                                                                        String itemCode,
                                                                        String itemStatus,
                                                                        String unitType,
                                                                        String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate);
        LocalDate end = parseDateNullable(endDate);
        PeriodReportRange periodRange = resolvePeriodReportRange(periodType, periodStartDate, start, end);
        String statisticDimensionValue = trimNullable(statisticDimension);
        String statisticMethodValue = trimNullable(statisticMethod);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String itemStatusValue = trimNullable(itemStatus);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, TxnSummary> summaryMap = loadTxnSummary(scope, periodRange.startDate(), periodRange.endDate(),
                null, periodRange.periodType(), periodRange.periodStartDate(), itemProfiles);
        StockTurnoverRateFilter filter = new StockTurnoverRateFilter(statisticDimensionValue, warehouseValue,
                itemCategoryValue, itemCodeValue, itemStatusValue, unitTypeValue);
        List<StockTurnoverRateReportRow> rows = collectStockTurnoverRateRows(scope, filter, itemProfiles, summaryMap);
        rows.sort(Comparator.comparing(StockTurnoverRateReportRow::warehouse).thenComparing(StockTurnoverRateReportRow::itemCode));
        if ("物品".equals(statisticDimensionValue)) {
            rows = aggregateStockTurnoverRows(rows);
        }
        if (StringUtils.hasText(statisticMethodValue) && Objects.equals(statisticMethodValue, "按数量计算")) {
            throw new BusinessException("库存周转率只支持移动加权金额口径，不支持按物品档案单价折算数量");
        }
        return page(rows, safePageNo, safePageSize);
    }

    private List<StockTurnoverRateReportRow> collectStockTurnoverRateRows(InventoryScope scope,
                                                                          StockTurnoverRateFilter filter,
                                                                          Map<String, ItemProfileSnapshot> itemProfiles,
                                                                          Map<String, TxnSummary> summaryMap) {
        List<StockTurnoverRateReportRow> rows = new ArrayList<>();
        for (InventoryBalanceDO balance : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
            if (item == null || !matchesStockTurnoverBalance(balance, item, filter)) {
                continue;
            }
            rows.add(buildStockTurnoverRateRow(scope, balance, item, filter.statisticDimension(), summaryMap));
        }
        return rows;
    }

    private boolean matchesStockTurnoverBalance(InventoryBalanceDO balance,
                                                ItemProfileSnapshot item,
                                                StockTurnoverRateFilter filter) {
        return (!StringUtils.hasText(filter.warehouse()) || containsIgnoreCase(balance.getWarehouseName(), filter.warehouse()))
                && (!StringUtils.hasText(filter.itemCategory()) || Objects.equals(item.category(), filter.itemCategory()))
                && (!StringUtils.hasText(filter.itemCode()) || Objects.equals(item.itemCode(), filter.itemCode()))
                && (!StringUtils.hasText(filter.itemStatus()) || Objects.equals(item.status(), filter.itemStatus()))
                && (!StringUtils.hasText(filter.unitType()) || Objects.equals(item.stockUnit(), filter.unitType()));
    }

    private StockTurnoverRateReportRow buildStockTurnoverRateRow(InventoryScope scope,
                                                                 InventoryBalanceDO balance,
                                                                 ItemProfileSnapshot item,
                                                                 String statisticDimensionValue,
                                                                 Map<String, TxnSummary> summaryMap) {
        String key = turnoverKey(balance.getWarehouseName(), item, statisticDimensionValue);
        TxnSummary txnSummary = summaryMap.getOrDefault(key, new TxnSummary());
        BigDecimal opening = txnSummary.openingQty;
        BigDecimal closing = txnSummary.closingQty == null ? defaultQuantity(balance.getQuantity()) : txnSummary.closingQty;
        BigDecimal outbound = txnSummary.outboundQty;
        BigDecimal openingAmount = txnSummary.openingAmount;
        BigDecimal closingAmount = txnSummary.closingAmount.compareTo(BigDecimal.ZERO) == 0
                ? defaultMoney(balance.getCostAmount()) : txnSummary.closingAmount;
        BigDecimal avgStockAmount = openingAmount.add(closingAmount).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal outboundAmount = txnSummary.outboundAmount;
        BigDecimal turnoverRate = avgStockAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(TURNOVER_RATE_SCALE, RoundingMode.HALF_UP)
                : outboundAmount.divide(avgStockAmount, TURNOVER_RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal turnoverDays = turnoverRate.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(TURNOVER_RATE_SCALE, RoundingMode.HALF_UP)
                : DAYS_PER_YEAR.divide(turnoverRate, TURNOVER_RATE_SCALE, RoundingMode.HALF_UP);
        return new StockTurnoverRateReportRow(summaryRowId(balance.getWarehouseName(), item.itemCode(), key),
                defaultOrgName(scope), defaultIfBlank(balance.getWarehouseName(), ""), item.itemName(), item.itemCode(),
                item.stockUnit(), item.category(), item.status(), openingAmount, closingAmount, avgStockAmount,
                outboundAmount, turnoverRate, turnoverDays);
    }

    private StagnantStockReportRow toStagnantStockRow(InventoryScope scope,
                                                      InventoryBalanceDO balance,
                                                      Map<String, ItemProfileSnapshot> itemProfiles,
                                                      Map<String, TransactionStats> statsMap) {
        ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
        TransactionStats stats = statsMap.getOrDefault(transactionKey(balance.getWarehouseName(), balance.getItemCode()), new TransactionStats());
        BigDecimal stockQty = defaultQuantity(balance.getQuantity());
        int retainedDays = stats.latestMovementTime == null
                ? 0
                : (int) ChronoUnit.DAYS.between(stats.latestMovementTime.toLocalDate(), LocalDate.now());
        Integer stagnantDays = item == null ? 0 : item.stagnantDays();
        String stagnantStatus = isStagnantStock(stagnantDays, retainedDays, stockQty) ? "是" : "否";
        return new StagnantStockReportRow(
                balance.getWarehouseName() + "|" + balance.getItemCode(),
                defaultIfBlank(balance.getWarehouseName(), ""),
                defaultIfBlank(balance.getItemName(), ""),
                defaultIfBlank(balance.getItemCode(), ""),
                defaultIfBlank(item == null ? null : item.spec(), ""),
                defaultIfBlank(item == null ? null : item.stockUnit(), "库存单位"),
                formatDateTime(stats.firstInboundTime),
                formatDateTime(stats.latestInboundTime),
                formatDateTime(stats.latestOutboundTime),
                stats.latestInboundQty,
                stats.latestOutboundQty,
                stockQty,
                retainedDays,
                stagnantDays == null ? 0 : stagnantDays,
                stagnantStatus,
                defaultIfBlank(item == null ? null : item.status(), "启用"),
                defaultIfBlank(item == null ? null : item.category(), "")
        );
    }

    private boolean isStagnantStock(Integer stagnantDays, int retainedDays, BigDecimal stockQty) {
        return stagnantDays != null
                && stagnantDays > 0
                && retainedDays >= stagnantDays
                && stockQty.compareTo(BigDecimal.ZERO) > 0;
    }

    private InventoryProfitLossReportRow toInventoryProfitLossRow(InventoryScope scope,
                                                                  InventoryCheckKind kind,
                                                                  InventoryCheckHeader header,
                                                                  InventoryCheckLine line,
                                                                  ItemProfileSnapshot item) {
        BigDecimal bookQty = defaultQuantity(line.getBookQty());
        BigDecimal actualQty = defaultQuantity(line.getActualQty());
        BigDecimal diffQty = defaultQuantity(line.getDiffQty());
        BigDecimal bookAmount = defaultMoney(line.getBookAmount());
        BigDecimal actualAmount = defaultMoney(line.getActualAmount());
        BigDecimal diffAmount = defaultMoney(line.getDiffAmount());
        BigDecimal profitQty = defaultQuantity(line.getProfitQty());
        BigDecimal lossQty = defaultQuantity(line.getLossQty());
        BigDecimal profitAmount = defaultMoney(line.getProfitAmount());
        BigDecimal lossAmount = defaultMoney(line.getLossAmount());
        return new InventoryProfitLossReportRow(
                buildRowId(kind.getHeaderTable(), header.getId(), line.getId()),
                defaultIfBlank(line.getItemCode(), ""),
                defaultIfBlank(line.getItemName(), ""),
                defaultIfBlank(line.getSpec(), defaultIfBlank(item == null ? null : item.spec(), "")),
                defaultIfBlank(line.getCategory(), defaultIfBlank(item == null ? null : item.category(), "")),
                defaultIfBlank(item == null ? null : item.statisticsType(), defaultIfBlank(line.getCategory(), "")),
                defaultIfBlank(line.getUnitName(), defaultIfBlank(item == null ? null : item.stockUnit(), "库存单位")),
                defaultIfBlank(header.getDocumentCode(), ""),
                defaultIfBlank(header.getCheckRangeType(), ""),
                defaultIfBlank(header.getThirdPartyDocument(), ""),
                defaultOrgName(scope),
                defaultOrgCode(scope),
                defaultIfBlank(header.getWarehouseName(), ""),
                formatDate(header.getCheckDate()),
                formatDateTime(header.getApprovedAt()),
                resolveUserName(header.getApprovedBy()),
                bookQty,
                bookAmount,
                actualQty,
                actualAmount,
                diffQty,
                diffAmount,
                diffQty.abs(),
                diffAmount.abs(),
                diffAmount,
                profitQty.compareTo(BigDecimal.ZERO) > 0 ? "盘盈" : (lossQty.compareTo(BigDecimal.ZERO) > 0 ? "盘亏" : "无差异"),
                profitQty.compareTo(BigDecimal.ZERO) > 0 ? defaultMoney(line.getProfitInboundPrice()) : defaultMoney(line.getLossOutboundPrice()),
                lossQty.compareTo(BigDecimal.ZERO) > 0 ? defaultMoney(line.getLossOutboundPrice()) : defaultMoney(line.getProfitInboundPrice()),
                defaultIfBlank(line.getProfitLossReason(), defaultIfBlank(header.getRemark(), "")),
                defaultIfBlank(header.getRemark(), "")
        );
    }

    private Map<String, ItemProfileSnapshot> loadItemProfiles(InventoryScope scope) {
        return itemProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(profile -> Boolean.FALSE.equals(profile.getDraft()))
                .map(this::toItemProfileSnapshot)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ItemProfileSnapshot::itemCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private ItemProfileSnapshot toItemProfileSnapshot(ItemProfileDO profile) {
        ItemCreateRequest request = parseItemRequest(profile.getDetailJson());
        return new ItemProfileSnapshot(
                defaultIfBlank(profile.getItemCode(), ""),
                defaultIfBlank(trimNullable(request.name()), ""),
                defaultIfBlank(trimNullable(request.category()), ""),
                defaultIfBlank(trimNullable(resolveStockUnit(request)), "库存单位"),
                defaultIfBlank(trimNullable(request.spec()), ""),
                defaultIfBlank(normalizeBigDecimalString(request.stockMin()), "0"),
                defaultIfBlank(normalizeBigDecimalString(request.stockMax()), "0"),
                request.warningDays(),
                request.stagnantDays(),
                defaultIfBlank(trimNullable(request.status()), "启用"),
                defaultIfBlank(trimNullable(request.statType()), "")
        );
    }

    private Map<String, TransactionStats> loadTransactionStats(InventoryScope scope) {
        List<InventoryTransactionDO> transactions = loadVisibleTransactions(scope, inventoryTransactionRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()));
        Map<String, TransactionStats> statsMap = new LinkedHashMap<>();
        for (InventoryTransactionDO transaction : transactions) {
            mergeTransactionStats(statsMap, transaction);
        }
        return statsMap;
    }

    private void mergeTransactionStats(Map<String, TransactionStats> statsMap, InventoryTransactionDO transaction) {
        String key = transactionKey(transaction.getWarehouseName(), transaction.getItemCode());
        TransactionStats stats = statsMap.computeIfAbsent(key, ignored -> new TransactionStats());
        BigDecimal quantityDelta = transaction.getQuantityDelta();
        if (quantityDelta != null && quantityDelta.compareTo(BigDecimal.ZERO) > 0) {
            mergeInboundTransactionStats(stats, transaction);
        } else if (quantityDelta != null && quantityDelta.compareTo(BigDecimal.ZERO) < 0) {
            mergeOutboundTransactionStats(stats, transaction);
        }
        mergeLatestMovementStats(stats, transactionDateTime(transaction));
    }

    private void mergeInboundTransactionStats(TransactionStats stats, InventoryTransactionDO transaction) {
        LocalDateTime businessTime = transactionDateTime(transaction);
        stats.latestInboundTime = businessTime;
        stats.latestInboundQty = defaultQuantity(transaction.getQuantityDelta());
        if (stats.firstInboundTime == null) {
            stats.firstInboundTime = businessTime;
        }
    }

    private void mergeOutboundTransactionStats(TransactionStats stats, InventoryTransactionDO transaction) {
        LocalDateTime businessTime = transactionDateTime(transaction);
        stats.latestOutboundTime = businessTime;
        stats.latestOutboundQty = defaultQuantity(transaction.getQuantityDelta().abs());
        if (stats.firstInboundTime == null) {
            stats.firstInboundTime = businessTime;
        }
    }

    private LocalDateTime transactionDateTime(InventoryTransactionDO transaction) {
        return (transaction.getBusinessDate() == null ? LocalDate.now() : transaction.getBusinessDate()).atStartOfDay();
    }

    private void mergeLatestMovementStats(TransactionStats stats, LocalDateTime movementTime) {
        if (movementTime != null && (stats.latestMovementTime == null || movementTime.isAfter(stats.latestMovementTime))) {
            stats.latestMovementTime = movementTime;
        }
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "";
        }
        Optional<UserAccountDO> user = userAccountRepository.findById(userId);
        return user.map(UserAccountDO::getRealName).filter(StringUtils::hasText).orElse(String.valueOf(userId));
    }

    private Map<String, TxnSummary> loadTxnSummary(InventoryScope scope,
                                                   LocalDate start,
                                                   LocalDate end,
                                                   String inoutType,
                                                   String periodType,
                                                   LocalDate periodStartDate,
                                                   Map<String, ItemProfileSnapshot> itemProfiles) {
        if (!StringUtils.hasText(periodType) || periodStartDate == null) {
            throw new BusinessException("周期成本报表必须选择期初周期类型和期初周期开始日");
        }
        Map<String, TxnSummary> summaryMap = loadApprovedPeriodOpenings(scope, periodType, periodStartDate, itemProfiles);
        List<InventoryTransactionDO> transactions = loadVisibleTransactions(scope, inventoryTransactionRepository.findByScopeAndBusinessDateRange(
                scope.scopeType(),
                scope.scopeId(),
                start,
                end
        ));
        for (InventoryTransactionDO transaction : transactions) {
            if (!matchesInventoryInoutSummaryBizType(transaction, inoutType)) {
                continue;
            }
            mergeTxnSummary(summaryMap, transaction);
        }
        return summaryMap;
    }

    private Map<String, TxnSummary> loadApprovedPeriodOpenings(InventoryScope scope,
                                                               String periodType,
                                                               LocalDate periodStartDate,
                                                               Map<String, ItemProfileSnapshot> itemProfiles) {
        Map<String, TxnSummary> result = new LinkedHashMap<>();
        List<String> warehouses = inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(InventoryBalanceDO::getWarehouseName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        for (String warehouse : warehouses) {
            InventoryPeriodOpeningDO opening = inventoryPeriodOpeningRepository
                    .findByPeriod(scope.scopeType(), scope.scopeId(), warehouse, periodType, periodStartDate)
                    .filter(header -> Objects.equals(header.getStatus(), approvedStatus()))
                    .orElseThrow(() -> new BusinessException("缺少已审核期初库存，无法生成成本报表：" + warehouse));
            for (InventoryPeriodOpeningLineDO line : inventoryPeriodOpeningRepository.findLinesByHeaderId(opening.getId())) {
                ItemProfileSnapshot item = itemProfiles.get(line.getItemCode());
                if (item == null) {
                    throw new BusinessException("期初库存物品不存在，请清理脏数据：" + line.getItemCode());
                }
                TxnSummary summary = result.computeIfAbsent(transactionKey(warehouse, line.getItemCode()), ignored -> new TxnSummary());
                summary.openingQty = defaultQuantity(line.getOpeningQty());
                summary.openingAmount = defaultMoney(line.getOpeningAmount());
                summary.closingQty = summary.openingQty;
                summary.closingAmount = summary.openingAmount;
            }
        }
        return result;
    }

    private void requireApprovedPeriodOpenings(InventoryScope scope,
                                               String periodType,
                                               LocalDate periodStartDate,
                                               String warehouse) {
        List<String> warehouses = StringUtils.hasText(warehouse) ? List.of(warehouse)
                : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                        .map(InventoryBalanceDO::getWarehouseName)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();
        for (String warehouseName : warehouses) {
            inventoryPeriodOpeningRepository
                    .findByPeriod(scope.scopeType(), scope.scopeId(), warehouseName, periodType, periodStartDate)
                    .filter(header -> Objects.equals(header.getStatus(), approvedStatus()))
                    .orElseThrow(() -> new BusinessException("缺少已审核期初库存，无法生成成本报表：" + warehouseName));
        }
    }

    private boolean matchesInventoryInoutSummaryBizType(InventoryTransactionDO transaction, String inoutType) {
        if (isOpeningBalanceBizType(transaction.getBizType())) {
            return false;
        }
        return !StringUtils.hasText(inoutType)
                || inventoryInoutSummaryBizTypes(inoutType).stream()
                .anyMatch(businessCode -> matchesTransactionBusinessCode(transaction.getBizType(), businessCode));
    }

    private List<String> inventoryInoutSummaryBizTypes(String inoutType) {
        if (Objects.equals(inoutType, "入库") || Objects.equals(inoutType, "出库")) {
            return stockInoutSummaryTypes().stream()
                    .filter(type -> Objects.equals(resolveStockDirection(type), inoutType))
                    .map(InventoryDocumentType::getBusinessCode)
                    .toList();
        }
        List<String> businessCodes = stockInoutSummaryTypes().stream()
                .filter(type -> Objects.equals(resolveStockInoutType(type), inoutType))
                .map(InventoryDocumentType::getBusinessCode)
                .toList();
        if (businessCodes.isEmpty()) {
            throw new BusinessException("不支持的出入库类型");
        }
        return businessCodes;
    }

    private boolean matchesTransactionBusinessCode(String bizType, String businessCode) {
        return Objects.equals(bizType, businessCode)
                || Objects.equals(bizType, businessCode + "_APPROVE")
                || Objects.equals(bizType, businessCode + "_CONFIRM")
                || Objects.equals(bizType, businessCode + "_UNAPPROVE");
    }

    private boolean isOpeningBalanceBizType(String bizType) {
        return matchesTransactionBusinessCode(bizType, InventoryDocumentType.WAREHOUSE_OPENING_BALANCE.getBusinessCode())
                || matchesTransactionBusinessCode(bizType, PeriodOpeningBalanceApplicationService.BUSINESS_CODE)
                || Objects.equals(bizType, "QA_OPENING_BALANCE");
    }

    private Map<String, LedgerLineSnapshot> loadLedgerLineSnapshotMap(InventoryScope scope) {
        return loadVisibleTransactions(scope, inventoryTransactionRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()))
                .stream()
                .filter(transaction -> !isOpeningBalanceBizType(transaction.getBizType()))
                .collect(Collectors.toMap(
                        transaction -> ledgerKey(normalizeTransactionBusinessCode(transaction.getBizType()),
                                transaction.getBizId(), transaction.getBizLineId()),
                        this::toLedgerLineSnapshot,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private LedgerLineSnapshot requireLedgerLineSnapshot(Map<String, LedgerLineSnapshot> ledgerMap,
                                                        String businessCode,
                                                        Long bizId,
                                                        Long bizLineId) {
        LedgerLineSnapshot snapshot = ledgerMap.get(ledgerKey(businessCode, bizId, bizLineId));
        if (snapshot == null) {
            throw new BusinessException("缺少库存流水，无法生成正式报表，请清理脏账本后重建：" + businessCode);
        }
        if (snapshot.businessDate() == null) {
            throw new BusinessException("库存流水缺少业务日期，无法生成正式报表，请清理脏账本后重建：" + businessCode);
        }
        return snapshot;
    }

    private LedgerLineSnapshot toLedgerLineSnapshot(InventoryTransactionDO transaction) {
        return new LedgerLineSnapshot(
                defaultQuantity(transaction.getQuantityDelta()),
                defaultMoney(transaction.getAmountDelta()),
                defaultMoney(transaction.getCostPrice()),
                transaction.getBusinessDate(),
                defaultIfBlank(transaction.getWarehouseName(), "")
        );
    }

    private String ledgerKey(String businessCode, Long bizId, Long bizLineId) {
        return defaultIfBlank(businessCode, "") + "|" + bizId + "|" + bizLineId;
    }

    private String normalizeTransactionBusinessCode(String bizType) {
        String value = defaultIfBlank(bizType, "");
        for (String suffix : List.of("_APPROVE", "_CONFIRM", "_UNAPPROVE")) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    private void mergeTxnSummary(Map<String, TxnSummary> summaryMap, InventoryTransactionDO transaction) {
        String key = transaction.getWarehouseName() + "|" + transaction.getItemCode();
        TxnSummary summary = summaryMap.computeIfAbsent(key, ignored -> new TxnSummary());
        BigDecimal delta = defaultQuantity(transaction.getQuantityDelta());
        BigDecimal amountDelta = defaultMoney(transaction.getAmountDelta());
        if (delta.compareTo(BigDecimal.ZERO) >= 0) {
            summary.inboundQty = summary.inboundQty.add(delta);
            summary.inboundAmount = summary.inboundAmount.add(amountDelta.max(BigDecimal.ZERO));
        } else {
            mergeOutboundTxnSummary(summary, transaction.getBizType(), delta.abs(), amountDelta.abs());
        }
        mergeTxnOpeningQty(summary, transaction.getBeforeQty(), transaction.getBeforeAmount());
        summary.closingQty = defaultQuantity(transaction.getAfterQty());
        summary.closingAmount = defaultMoney(transaction.getAfterAmount());
    }

    private void mergeOutboundTxnSummary(TxnSummary summary, String bizType, BigDecimal outboundQty, BigDecimal outboundAmount) {
        summary.outboundQty = summary.outboundQty.add(outboundQty);
        summary.outboundAmount = summary.outboundAmount.add(outboundAmount);
        if (isPurchaseReturnBizType(bizType)) {
            summary.returnDifferenceQty = summary.returnDifferenceQty.add(outboundQty);
        }
    }

    private void mergeTxnOpeningQty(TxnSummary summary, BigDecimal beforeQty, BigDecimal beforeAmount) {
        if (summary.openingQty.compareTo(BigDecimal.ZERO) == 0 && beforeQty != null) {
            summary.openingQty = defaultQuantity(beforeQty);
            summary.openingAmount = defaultMoney(beforeAmount);
        }
    }

    private Map<String, InventoryProfitLossAgg> loadInventoryCheckAgg(InventoryScope scope, LocalDate start, LocalDate end) {
        Map<String, InventoryProfitLossAgg> result = new LinkedHashMap<>();
        for (InventoryCheckKind kind : List.of(InventoryCheckKind.INVENTORY_CHECK, InventoryCheckKind.MULTI_INVENTORY_CHECK)) {
            List<InventoryCheckHeader> headers = loadVisibleCheckHeaders(kind, scope);
            Map<Long, List<InventoryCheckLine>> lineMap = inventoryCheckRepository.findLinesByHeaderIds(kind, headers.stream().map(InventoryCheckHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryCheckLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryCheckHeader header : headers) {
                if (!matchDate(header.getCheckDate(), start, end)) {
                    continue;
                }
                for (InventoryCheckLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    String key = header.getWarehouseName() + "|" + line.getItemCode();
                    InventoryProfitLossAgg agg = result.computeIfAbsent(key, ignored -> new InventoryProfitLossAgg());
                    agg.inventoryCheckQty = agg.inventoryCheckQty.add(defaultQuantity(line.getActualQty()));
                    agg.inventoryProfitLossQty = agg.inventoryProfitLossQty.add(defaultQuantity(line.getDiffQty()));
                }
            }
        }
        return result;
    }

    private List<InventoryInoutSummaryReportRow> aggregateInventoryInoutByCategory(List<InventoryInoutSummaryReportRow> rows) {
        Map<String, InventoryInoutSummaryReportRow> grouped = new LinkedHashMap<>();
        for (InventoryInoutSummaryReportRow row : rows) {
            String key = row.warehouse() + "|" + defaultIfBlank(row.itemCategory(), "");
            grouped.merge(key, row, this::mergeInventoryInoutSummaryRow);
        }
        return new ArrayList<>(grouped.values());
    }

    private InventoryInoutSummaryReportRow mergeInventoryInoutSummaryRow(InventoryInoutSummaryReportRow row,
                                                                         InventoryInoutSummaryReportRow value) {
        return new InventoryInoutSummaryReportRow(row.id(), row.itemCode(), row.itemName(), row.specModel(),
                row.itemCategory(), row.statisticType(), row.unit(), row.orgName(), row.orgCode(), row.warehouse(),
                row.warehouseType(), row.openingQty().add(value.openingQty()),
                row.openingCostAmountExTax().add(value.openingCostAmountExTax()), row.openingAvgCostExTax(),
                row.inboundQty().add(value.inboundQty()),
                row.inboundCostAmountExTax().add(value.inboundCostAmountExTax()), row.inboundAvgCostExTax(),
                row.outboundQty().add(value.outboundQty()),
                row.outboundCostAmountExTax().add(value.outboundCostAmountExTax()), row.outboundAvgCostExTax(),
                row.closingQty().add(value.closingQty()),
                row.closingCostAmountExTax().add(value.closingCostAmountExTax()), row.closingAvgCostExTax(),
                row.inventoryProfitLossQty().add(value.inventoryProfitLossQty()),
                row.inventoryProfitLossCostAmountTaxIncluded().add(value.inventoryProfitLossCostAmountTaxIncluded()),
                row.inventoryProfitLossCostAmountExTax().add(value.inventoryProfitLossCostAmountExTax()),
                row.inventoryCheckQty().add(value.inventoryCheckQty()),
                row.inventoryCheckCostAmountTaxIncluded().add(value.inventoryCheckCostAmountTaxIncluded()),
                row.closingCheckDiffQty().add(value.closingCheckDiffQty()),
                row.closingCheckDiffAmountExTax().add(value.closingCheckDiffAmountExTax()),
                row.returnDifferenceQty().add(value.returnDifferenceQty()),
                row.returnDifferenceCostAmountExTax().add(value.returnDifferenceCostAmountExTax()), row.inoutType());
    }

    private List<StockInoutSummaryReportRow> aggregateStockInoutRows(List<StockInoutSummaryReportRow> rows) {
        Map<String, MutableStockInoutSummaryRow> grouped = new LinkedHashMap<>();
        for (StockInoutSummaryReportRow row : rows) {
            String key = row.warehouse() + "|" + row.itemCode() + "|" + row.inoutType() + "|" + defaultIfBlank(row.oppositeOrg(), "") + "|" + defaultIfBlank(row.oppositeWarehouse(), "");
            grouped.computeIfAbsent(key, ignored -> new MutableStockInoutSummaryRow()).merge(row);
        }
        return grouped.values().stream().map(MutableStockInoutSummaryRow::toRow).toList();
    }

    private List<OtherInoutSummaryReportRow> aggregateOtherInoutRows(List<OtherInoutSummaryReportRow> rows) {
        Map<String, MutableOtherInoutSummaryRow> grouped = new LinkedHashMap<>();
        for (OtherInoutSummaryReportRow row : rows) {
            String key = row.warehouse() + "|" + row.itemCode() + "|" + row.inoutType() + "|" + row.reasonType();
            grouped.computeIfAbsent(key, ignored -> new MutableOtherInoutSummaryRow()).merge(row);
        }
        return grouped.values().stream().map(MutableOtherInoutSummaryRow::toRow).toList();
    }

    private List<InterOrgTransferSummaryReportRow> aggregateInterOrgTransferSummaryRows(List<InterOrgTransferDetailReportRow> rows) {
        Map<String, MutableInterOrgTransferSummaryRow> grouped = new LinkedHashMap<>();
        for (InterOrgTransferDetailReportRow row : rows) {
            String key = row.sourceStore() + "|" + row.targetStore() + "|" + row.itemCode();
            grouped.computeIfAbsent(key, ignored -> new MutableInterOrgTransferSummaryRow()).merge(row);
        }
        return grouped.values().stream().map(MutableInterOrgTransferSummaryRow::toRow).toList();
    }

    private List<StockTurnoverRateReportRow> aggregateStockTurnoverRows(List<StockTurnoverRateReportRow> rows) {
        Map<String, MutableStockTurnoverRateRow> grouped = new LinkedHashMap<>();
        for (StockTurnoverRateReportRow row : rows) {
            String key = row.warehouse() + "|" + row.itemCode();
            grouped.computeIfAbsent(key, ignored -> new MutableStockTurnoverRateRow()).merge(row);
        }
        return grouped.values().stream().map(MutableStockTurnoverRateRow::toRow).toList();
    }

    private StockInoutSummaryReportRow buildStockInoutSummaryRow(InventoryScope scope,
                                                                 InventoryDocumentType type,
                                                                 InventoryDocumentHeader header,
                                                                 InventoryDocumentLine line,
                                                                 ItemProfileSnapshot item,
                                                                 LedgerLineSnapshot ledger) {
        String inoutType = resolveStockInoutType(type);
        String direction = resolveStockDirection(type);
        String warehouse = resolveStockWarehouse(type, header);
        BigDecimal qty = ledger.quantity().abs();
        BigDecimal amount = ledger.amount().abs();
        BigDecimal avg = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.divide(qty, 2, RoundingMode.HALF_UP);
        return new StockInoutSummaryReportRow(
                summaryRowId(warehouse, item.itemCode(), inoutType + defaultIfBlank(header.getCounterpartyName(), "")),
                item.itemCode(),
                item.itemName(),
                item.spec(),
                item.category(),
                item.statisticsType(),
                item.stockUnit(),
                inoutType,
                defaultIfBlank(warehouse, ""),
                resolveWarehouseType(scope, warehouse),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getSecondaryName(), defaultIfBlank(header.getCounterpartyName(), "")),
                stockQuantityByDirection(direction, "入库", qty),
                stockMoneyByDirection(direction, "入库", amount),
                stockMoneyByDirection(direction, "入库", avg),
                stockMoneyByDirection(direction, "入库", amount),
                stockMoneyByDirection(direction, "入库", avg),
                stockQuantityByDirection(direction, "出库", qty),
                stockMoneyByDirection(direction, "出库", amount),
                stockMoneyByDirection(direction, "出库", avg),
                stockMoneyByDirection(direction, "出库", amount),
                stockMoneyByDirection(direction, "出库", avg),
                resolveStockStatisticMode(type)
        );
    }

    private InterOrgTransferDetailReportRow buildInterOrgTransferDetailRow(InventoryScope scope,
                                                                           InventoryDocumentType type,
                                                                           InventoryDocumentHeader header,
                                                                           InventoryDocumentLine line,
                                                                           ItemProfileSnapshot item,
                                                                           LedgerLineSnapshot ledger,
                                                                           String sourceStore,
                                                                           String targetStore) {
        BigDecimal qty = ledger.quantity().abs();
        BigDecimal amount = ledger.amount().abs();
        return new InterOrgTransferDetailReportRow(
                summaryRowId(sourceStore, item.itemCode(), defaultIfBlank(header.getDocumentCode(), "")),
                defaultIfBlank(header.getDocumentCode(), ""),
                item.itemCode(),
                item.itemName(),
                defaultIfBlank(header.getStatus(), ""),
                formatDate(header.getDocumentDate()),
                formatDateTime(header.getApprovedAt()),
                defaultIfBlank(sourceStore, ""),
                defaultIfBlank(resolveStockWarehouse(type, header), ""),
                formatDate(header.getDocumentDate()),
                formatDateTime(header.getApprovedAt()),
                defaultIfBlank(targetStore, ""),
                defaultIfBlank(resolveTargetWarehouse(type, header), ""),
                item.spec(),
                item.category(),
                item.stockUnit(),
                qty,
                item.stockUnit(),
                qty,
                amount,
                amount,
                amount,
                amount,
                amount,
                amount,
                defaultIfBlank(header.getRemark(), ""),
                resolveStockStatisticMode(type)
        );
    }

    private String resolveStockStatisticMode(InventoryDocumentType type) {
        return InventorySupplementDocumentNames.stockStatisticMode(type);
    }

    private String resolveStockInoutType(InventoryDocumentType type) {
        return InventorySupplementDocumentNames.stockInoutType(type);
    }

    private String resolveStockDirection(InventoryDocumentType type) {
        return InventorySupplementDocumentNames.stockDirection(type);
    }

    private String resolveStockWarehouse(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.STORE_TRANSFER) {
            return defaultIfBlank(parseHeaderExtraJson(header).get(STORE_TRANSFER_SOURCE_WAREHOUSE), "");
        }
        if (InventorySupplementDocumentNames.isInboundWarehouseDocument(type)) {
            return defaultIfBlank(header.getPrimaryName(), "");
        }
        return defaultIfBlank(header.getPrimaryName(), defaultIfBlank(header.getCounterpartyName(), ""));
    }

    private String resolveTargetWarehouse(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.STORE_TRANSFER) {
            return defaultIfBlank(parseHeaderExtraJson(header).get(STORE_TRANSFER_TARGET_WAREHOUSE), "");
        }
        return defaultIfBlank(header.getSecondaryName(), "");
    }

    private BigDecimal stockQuantityByDirection(String direction, String expectedDirection, BigDecimal qty) {
        return expectedDirection.equals(direction) ? qty
                : BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal stockMoneyByDirection(String direction, String expectedDirection, BigDecimal amount) {
        return expectedDirection.equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveOtherInoutType(InventoryDocumentType type, InventoryDocumentHeader header, InventoryDocumentLine line) {
        return switch (type) {
            case OTHER_INBOUND -> "其他入库";
            case OTHER_OUTBOUND -> "其他出库";
            case DAMAGE_OUTBOUND -> "盘亏出库";
            case PRODUCTION_INBOUND -> "盘盈入库";
            default -> resolveStockInoutType(type);
        };
    }

    private String resolveOtherReasonType(InventoryDocumentHeader header, InventoryDocumentLine line, InventoryDocumentType type) {
        String text = (defaultIfBlank(header.getReason(), "") + defaultIfBlank(line.getLineReason(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("盘盈")) {
            return "盘盈";
        }
        if (text.contains("盘亏")) {
            return "盘亏";
        }
        if (text.contains("报损")) {
            return "报损";
        }
        if (text.contains("调整")) {
            return "调整";
        }
        if (text.contains("赠")) {
            return "赠品";
        }
        return "其他";
    }

    private boolean isPurchaseReturnBizType(String bizType) {
        return Objects.equals(bizType, InventoryDocumentType.PURCHASE_RETURN_OUTBOUND.getBusinessCode())
                || Objects.equals(bizType, InventoryDocumentType.OTHER_OUTBOUND.getBusinessCode());
    }

    private boolean isApprovedDocument(InventoryDocumentHeader header) {
        return header != null && Objects.equals(defaultIfBlank(header.getStatus(), ""), approvedStatus());
    }

    private String approvedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.APPROVED);
    }

    private String summaryKey(String warehouse, ItemProfileSnapshot item, String statisticDimension) {
        if ("物品类别".equals(statisticDimension)) {
            return defaultIfBlank(warehouse, "") + "|" + item.category();
        }
        return defaultIfBlank(warehouse, "") + "|" + item.itemCode();
    }

    private String turnoverKey(String warehouse, ItemProfileSnapshot item, String statisticDimension) {
        return summaryKey(warehouse, item, statisticDimension);
    }

    private String summaryRowId(String left, String right, String third) {
        return defaultIfBlank(left, "") + "|" + defaultIfBlank(right, "") + "|" + defaultIfBlank(third, "");
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && StringUtils.hasText(keyword) && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private List<StockInoutSummaryReportRow> aggregateStockInoutRowsByMode(List<StockInoutSummaryReportRow> rows) {
        return rows;
    }

    private ItemCreateRequest parseItemRequest(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            throw new BusinessException("物品详情数据为空");
        }
        try {
            return objectMapper.readValue(detailJson, ItemCreateRequest.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("物品详情数据已损坏");
        }
    }

    private String resolveStockUnit(ItemCreateRequest request) {
        if (request.defaultStockUnit() != null && !request.defaultStockUnit().isBlank()) {
            return request.defaultStockUnit();
        }
        if (request.defaultCostUnit() != null && !request.defaultCostUnit().isBlank()) {
            return request.defaultCostUnit();
        }
        if (request.defaultPurchaseUnit() != null && !request.defaultPurchaseUnit().isBlank()) {
            return request.defaultPurchaseUnit();
        }
        return "库存单位";
    }

    private String resolveWarehouseType(InventoryScope scope, String warehouseName) {
        if (!StringUtils.hasText(warehouseName)) {
            return "";
        }
        List<WarehouseDO> warehouses;
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP)) {
            warehouses = warehouseRepository.findByGroupId(scope.scopeId());
        } else if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE)) {
            warehouses = warehouseRepository.findByStoreId(scope.scopeId());
        } else {
            warehouses = List.of();
        }
        for (WarehouseDO warehouse : warehouses) {
            if (Objects.equals(warehouse.getWarehouseName(), warehouseName)) {
                return defaultIfBlank(warehouse.getWarehouseType(), "");
            }
        }
        return "";
    }

    private boolean matchDate(LocalDate value, LocalDate start, LocalDate end) {
        if (value == null) {
            return start == null && end == null;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        return end == null || !value.isAfter(end);
    }

    private LocalDate parseDateNullable(String value) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return LocalDate.parse(normalized);
    }

    private Integer parseIntegerNullable(String value) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return Integer.valueOf(normalized);
    }

    private String transactionKey(String warehouseName, String itemCode) {
        return defaultIfBlank(warehouseName, "") + "|" + defaultIfBlank(itemCode, "");
    }

    private String buildRowId(String prefix, Long headerId, Long lineId) {
        return prefix + "-" + headerId + "-" + lineId;
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private List<InventoryCheckHeader> loadVisibleCheckHeaders(InventoryCheckKind kind, InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = canViewScopeData(scope, operatorId);
        return inventoryCheckRepository.findHeadersByScopeAndKindOrdered(kind, scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> viewAll || belongsToOperator(header.getCreatedBy(), header.getSalesmanUserId(), operatorId))
                .toList();
    }

    private List<InventoryDocumentHeader> loadVisibleDocumentHeaders(InventoryDocumentType type, InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = canViewScopeData(scope, operatorId);
        return inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> viewAll || belongsToOperator(header.getCreatedBy(), header.getSalesmanUserId(), operatorId))
                .toList();
    }

    private List<InventoryTransactionDO> loadVisibleTransactions(InventoryScope scope, List<InventoryTransactionDO> transactions) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        if (canViewScopeData(scope, operatorId)) {
            return transactions;
        }
        Map<String, List<InventoryDocumentHeader>> documentHeaderMap = loadVisibleDocumentHeadersByBusinessCode(scope);
        return transactions.stream()
                .filter(transaction -> isVisibleTransaction(transaction, documentHeaderMap, operatorId))
                .toList();
    }

    private Map<String, List<InventoryDocumentHeader>> loadVisibleDocumentHeadersByBusinessCode(InventoryScope scope) {
        Map<String, List<InventoryDocumentHeader>> result = new LinkedHashMap<>();
        for (InventoryDocumentType type : InventoryDocumentType.values()) {
            result.put(type.getBusinessCode(), loadVisibleDocumentHeaders(type, scope));
            result.put(type.getBusinessCode() + "_APPROVE", result.get(type.getBusinessCode()));
            result.put(type.getBusinessCode() + "_CONFIRM", result.get(type.getBusinessCode()));
            result.put(type.getBusinessCode() + "_UNAPPROVE", result.get(type.getBusinessCode()));
        }
        return result;
    }

    private boolean isVisibleTransaction(InventoryTransactionDO transaction,
                                         Map<String, List<InventoryDocumentHeader>> documentHeaderMap,
                                         Long operatorId) {
        if (Objects.equals(transaction.getOperatorId(), operatorId)) {
            return true;
        }
        List<InventoryDocumentHeader> headers = documentHeaderMap.getOrDefault(defaultIfBlank(transaction.getBizType(), ""), List.of());
        return headers.stream().anyMatch(header -> Objects.equals(header.getId(), transaction.getBizId()));
    }

    private boolean canViewScopeData(InventoryScope scope, Long operatorId) {
        return dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
    }

    private boolean belongsToOperator(Long createdBy, Long salesmanUserId, Long operatorId) {
        return Objects.equals(createdBy, operatorId) || Objects.equals(salesmanUserId, operatorId);
    }

    private static BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String defaultOrgName(InventoryScope scope) {
        return Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP) ? "集团" : (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE) ? "门店" : "平台");
    }

    private String defaultOrgCode(InventoryScope scope) {
        return Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP) ? "group" : (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE) ? "store" : "platform");
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String normalizeBigDecimalString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new BigDecimal(value.trim()).stripTrailingZeros().toPlainString();
    }

    private PeriodReportRange resolvePeriodReportRange(String periodType,
                                                       String periodStartDate,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        String normalizedType = requiredPeriodType(periodType);
        LocalDate normalizedStart = parseRequiredDate(periodStartDate, "期初周期开始日不能为空");
        LocalDate normalizedEnd = endDate == null ? periodEndDate(normalizedType, normalizedStart) : endDate;
        LocalDate businessStart = startDate == null ? normalizedStart : startDate;
        if (businessStart.isBefore(normalizedStart)) {
            throw new BusinessException("报表开始日期不能早于期初周期开始日");
        }
        if (normalizedEnd.isBefore(businessStart)) {
            throw new BusinessException("报表结束日期不能早于开始日期");
        }
        return new PeriodReportRange(normalizedType, normalizedStart, businessStart, normalizedEnd);
    }

    private String requiredPeriodType(String periodType) {
        String normalized = trimNullable(periodType);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("期初周期类型不能为空");
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.INVENTORY_PERIOD_TYPE, normalized);
    }

    private String normalizeProfitLossResult(String profitLossResult) {
        String normalized = trimNullable(profitLossResult);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return dictionaryLookupService.requireEnabledCode(DictionaryCodes.INVENTORY_PROFIT_LOSS_RESULT, normalized);
    }

    private String profitLossResultCode(String profitLossResult) {
        if (Objects.equals(profitLossResult, "盘盈")) {
            return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_PROFIT_LOSS_RESULT, DictionaryCodes.PROFIT);
        }
        if (Objects.equals(profitLossResult, "盘亏")) {
            return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_PROFIT_LOSS_RESULT, DictionaryCodes.LOSS);
        }
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_PROFIT_LOSS_RESULT, DictionaryCodes.NO_DIFF);
    }

    private LocalDate parseRequiredDate(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(message);
        }
        return LocalDate.parse(normalized);
    }

    private LocalDate periodEndDate(String periodType, LocalDate startDate) {
        return switch (periodType) {
            case "DAY" -> startDate;
            case "MONTH" -> startDate.plusMonths(1).minusDays(1);
            case "YEAR" -> startDate.plusYears(1).minusDays(1);
            default -> throw new BusinessException("不支持的期初周期类型");
        };
    }

    private Map<String, String> parseHeaderExtraJson(InventoryDocumentHeader header) {
        if (header == null || !StringUtils.hasText(header.getExtraJson())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(header.getExtraJson(),
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("报表扩展字段格式错误，请清理脏数据");
        }
    }

    private <T> PageData<T> page(List<T> rows, int pageNo, int pageSize) {
        long total = rows.size();
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), total, pageNo, pageSize);
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }

    private record PeriodReportRange(String periodType, LocalDate periodStartDate, LocalDate startDate,
                                     LocalDate endDate) {
    }

    private record ItemProfileSnapshot(String itemCode, String itemName, String category, String stockUnit,
                                       String spec, String stockMin, String stockMax, Integer warningDays,
                                       Integer stagnantDays, String status, String statisticsType) {
    }

}
