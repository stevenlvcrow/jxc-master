package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存报表补充业务。
 */
@Service
public class InventorySupplementReportApplicationService {

    private static final int MAX_PAGE_SIZE = 200;

    private final OrgScopeService orgScopeService;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final InventoryCheckRepository inventoryCheckRepository;
    private final ItemProfileRepository itemProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final WarehouseRepository warehouseRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public InventorySupplementReportApplicationService(OrgScopeService orgScopeService,
                                                       InventoryBalanceRepository inventoryBalanceRepository,
                                                       InventoryDocumentRepository inventoryDocumentRepository,
                                                       InventoryCheckRepository inventoryCheckRepository,
                                                       ItemProfileRepository itemProfileRepository,
                                                       UserAccountRepository userAccountRepository,
                                                       WarehouseRepository warehouseRepository,
                                                       NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                                       ObjectMapper objectMapper) {
        this.orgScopeService = orgScopeService;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryDocumentRepository = inventoryDocumentRepository;
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.itemProfileRepository = itemProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.warehouseRepository = warehouseRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public PageData<StockWarningReportRow> stockWarningReport(Integer pageNo,
                                                             Integer pageSize,
                                                             String statisticDimension,
                                                             String warehouse,
                                                             String itemCategory,
                                                             String itemCode,
                                                             String itemStatus,
                                                             String warningStatus,
                                                             String unitType,
                                                             String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String statisticDimensionValue = trimNullable(statisticDimension);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String itemStatusValue = trimNullable(itemStatus);
        String warningStatusValue = trimNullable(warningStatus);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        List<StockWarningReportRow> rows = inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(balance -> toStockWarningRow(scope, balance, itemProfiles))
                .filter(row -> !StringUtils.hasText(warehouseValue) || Objects.equals(row.warehouse(), warehouseValue))
                .filter(row -> !StringUtils.hasText(itemCategoryValue) || Objects.equals(row.itemCategory(), itemCategoryValue))
                .filter(row -> !StringUtils.hasText(itemCodeValue) || row.itemCode().contains(itemCodeValue))
                .filter(row -> !StringUtils.hasText(itemStatusValue) || Objects.equals(row.itemStatus(), itemStatusValue))
                .filter(row -> !StringUtils.hasText(warningStatusValue) || Objects.equals(row.warningStatus(), warningStatusValue))
                .filter(row -> !StringUtils.hasText(unitTypeValue) || Objects.equals(row.unit(), unitTypeValue))
                .toList();

        List<StockWarningReportRow> normalized = "仓库".equals(statisticDimensionValue)
                ? rows
                : aggregateStockWarningRows(rows);
        List<StockWarningReportRow> sorted = normalized.stream()
                .sorted(Comparator.comparing(StockWarningReportRow::itemCode)
                        .thenComparing(row -> defaultIfBlank(row.warehouse(), "")))
                .toList();
        return page(sorted, safePageNo, safePageSize);
    }

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
        String profitLossResultValue = trimNullable(profitLossResult);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        List<InventoryProfitLossReportRow> rows = new ArrayList<>();
        for (InventoryCheckKind kind : List.of(InventoryCheckKind.INVENTORY_CHECK, InventoryCheckKind.MULTI_INVENTORY_CHECK)) {
            List<InventoryCheckHeader> headers = inventoryCheckRepository.findHeadersByScopeAndKindOrdered(kind, scope.scopeType(), scope.scopeId());
            Map<Long, List<InventoryCheckLine>> lineMap = inventoryCheckRepository.findLinesByHeaderIds(kind, headers.stream().map(InventoryCheckHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryCheckLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryCheckHeader header : headers) {
                if (!matchDate(header.getCheckDate(), start, end)) {
                    continue;
                }
                if (StringUtils.hasText(warehouseValue) && !Objects.equals(defaultIfBlank(header.getWarehouseName(), ""), warehouseValue)) {
                    continue;
                }
                if (StringUtils.hasText(checkTypeValue) && !Objects.equals(defaultIfBlank(header.getCheckRangeType(), ""), checkTypeValue)) {
                    continue;
                }
                for (InventoryCheckLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    ItemProfileSnapshot itemProfile = itemProfiles.get(line.getItemCode());
                    InventoryProfitLossReportRow row = toInventoryProfitLossRow(scope, kind, header, line, itemProfile);
                    if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(row.itemCategory(), itemCategoryValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(statisticsTypeValue) && !Objects.equals(row.statisticsType(), statisticsTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(itemKeywordValue)
                            && !(row.itemCode().contains(itemKeywordValue) || row.itemName().contains(itemKeywordValue))) {
                        continue;
                    }
                    if (StringUtils.hasText(profitLossResultValue) && !Objects.equals(row.profitLossResult(), profitLossResultValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(unitTypeValue) && !Objects.equals(row.unit(), unitTypeValue)) {
                        continue;
                    }
                    rows.add(row);
                }
            }
        }
        rows.sort(Comparator.comparing(InventoryProfitLossReportRow::checkTime)
                .reversed()
                .thenComparing(InventoryProfitLossReportRow::checkDocumentNo)
                .thenComparing(InventoryProfitLossReportRow::itemCode));
        return page(rows, safePageNo, safePageSize);
    }

    public PageData<InventoryInoutSummaryReportRow> inventoryInoutSummaryReport(Integer pageNo,
                                                                                Integer pageSize,
                                                                                String statisticDimension,
                                                                                String warehouse,
                                                                                String warehouseType,
                                                                                String startDate,
                                                                                String endDate,
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
        Map<String, TxnSummary> txnSummaryMap = loadTxnSummary(scope, start, end);
        Map<String, InventoryProfitLossAgg> checkAggMap = loadInventoryCheckAgg(scope, start, end);

        List<InventoryInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryBalanceDO balance : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
            if (item == null) {
                continue;
            }
            if (StringUtils.hasText(warehouseValue) && !Objects.equals(balance.getWarehouseName(), warehouseValue)) {
                continue;
            }
            if (StringUtils.hasText(itemCodeValue) && !Objects.equals(item.itemCode(), itemCodeValue)) {
                continue;
            }
            if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(item.category(), itemCategoryValue)) {
                continue;
            }
            if (StringUtils.hasText(statisticTypeValue) && !Objects.equals(item.statisticsType(), statisticTypeValue)) {
                continue;
            }
            if (StringUtils.hasText(itemStatusValue) && !Objects.equals(item.status(), itemStatusValue)) {
                continue;
            }
            if (StringUtils.hasText(unitTypeValue) && !Objects.equals(item.stockUnit(), unitTypeValue)) {
                continue;
            }
            String rowKey = summaryKey(balance.getWarehouseName(), item, statisticDimensionValue);
            TxnSummary txnSummary = txnSummaryMap.getOrDefault(rowKey, new TxnSummary());
            InventoryProfitLossAgg checkAgg = checkAggMap.getOrDefault(rowKey, new InventoryProfitLossAgg());
            BigDecimal openingQty = txnSummary.openingQty;
            BigDecimal inboundQty = txnSummary.inboundQty;
            BigDecimal outboundQty = txnSummary.outboundQty;
            BigDecimal closingQty = txnSummary.closingQty == null ? defaultQuantity(balance.getQuantity()) : txnSummary.closingQty;
            BigDecimal unitCost = item.unitCost();
            BigDecimal openingCost = openingQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal inboundCost = inboundQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal outboundCost = outboundQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closingCost = closingQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgOpening = openingQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : openingCost.divide(openingQty, 2, RoundingMode.HALF_UP);
            BigDecimal avgInbound = inboundQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : inboundCost.divide(inboundQty, 2, RoundingMode.HALF_UP);
            BigDecimal avgOutbound = outboundQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : outboundCost.divide(outboundQty, 2, RoundingMode.HALF_UP);
            BigDecimal avgClosing = closingQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : closingCost.divide(closingQty, 2, RoundingMode.HALF_UP);
            BigDecimal inventoryCheckQty = checkAgg.inventoryCheckQty;
            BigDecimal inventoryProfitLossQty = checkAgg.inventoryProfitLossQty;
            BigDecimal inventoryProfitLossCostAmount = inventoryProfitLossQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal inventoryCheckCostAmount = inventoryCheckQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closingCheckDiffQty = closingQty.subtract(inventoryCheckQty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal closingCheckDiffAmount = closingCheckDiffQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal returnDifferenceQty = txnSummary.returnDifferenceQty;
            BigDecimal returnDifferenceAmount = returnDifferenceQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            InventoryInoutSummaryReportRow row = new InventoryInoutSummaryReportRow(
                    summaryRowId(balance.getWarehouseName(), item.itemCode(), rowKey),
                    item.itemCode(),
                    item.itemName(),
                    item.spec(),
                    item.category(),
                    item.statisticsType(),
                    item.stockUnit(),
                    defaultOrgName(scope),
                    defaultOrgCode(scope),
                    defaultIfBlank(balance.getWarehouseName(), ""),
                    resolveWarehouseType(scope, balance.getWarehouseName()),
                    openingQty,
                    openingCost,
                    avgOpening,
                    inboundQty,
                    inboundCost,
                    avgInbound,
                    outboundQty,
                    outboundCost,
                    avgOutbound,
                    closingQty,
                    closingCost,
                    avgClosing,
                    inventoryProfitLossQty,
                    inventoryProfitLossCostAmount,
                    inventoryProfitLossCostAmount,
                    inventoryCheckQty,
                    inventoryCheckCostAmount,
                    closingCheckDiffQty,
                    closingCheckDiffAmount,
                    returnDifferenceQty,
                    returnDifferenceAmount,
                    defaultIfBlank(inoutTypeValue, "全部")
            );
            if (hideEmpty && row.isEmpty()) {
                continue;
            }
            rows.add(row);
        }
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

    public PageData<StockInoutSummaryReportRow> stockInoutSummaryReport(Integer pageNo,
                                                                       Integer pageSize,
                                                                       String statisticMode,
                                                                       String dateDimension,
                                                                       String startDate,
                                                                       String endDate,
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
        List<StockInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : List.of(
                InventoryDocumentType.PURCHASE_INBOUND,
                InventoryDocumentType.PURCHASE_RETURN_OUTBOUND,
                InventoryDocumentType.DEPARTMENT_PICKING,
                InventoryDocumentType.DEPARTMENT_RETURN,
                InventoryDocumentType.STOCK_TRANSFER,
                InventoryDocumentType.STOCK_TRANSFER_INBOUND,
                InventoryDocumentType.DEPARTMENT_TRANSFER,
                InventoryDocumentType.DAMAGE_OUTBOUND,
                InventoryDocumentType.OTHER_INBOUND,
                InventoryDocumentType.OTHER_OUTBOUND,
                InventoryDocumentType.PRODUCTION_INBOUND,
                InventoryDocumentType.CUSTOMER_SALES_OUTBOUND,
                InventoryDocumentType.CUSTOMER_RETURN_INBOUND,
                InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND,
                InventoryDocumentType.STORE_TRANSFER,
                InventoryDocumentType.STOCK_TRANSFER_OUTBOUND)) {
            List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
            Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryDocumentHeader header : headers) {
                if (!matchDate(header.getDocumentDate(), start, end)) {
                    continue;
                }
                if (StringUtils.hasText(targetStoreValue)
                        && !(Objects.equals(header.getPrimaryName(), targetStoreValue) || Objects.equals(header.getCounterpartyName(), targetStoreValue)
                        || Objects.equals(header.getSecondaryName(), targetStoreValue))) {
                    continue;
                }
                for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
                    if (item == null) {
                        continue;
                    }
                    if (StringUtils.hasText(itemKeywordValue)
                            && !(containsIgnoreCase(line.getItemCode(), itemKeywordValue)
                            || containsIgnoreCase(line.getItemName(), itemKeywordValue)
                            || containsIgnoreCase(line.getSpec(), itemKeywordValue))) {
                        continue;
                    }
                    if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(item.category(), itemCategoryValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(statisticTypeValue) && !Objects.equals(item.statisticsType(), statisticTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(itemStatusValue) && !Objects.equals(item.status(), itemStatusValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(unitTypeValue) && !Objects.equals(item.stockUnit(), unitTypeValue)) {
                        continue;
                    }
                    StockInoutSummaryReportRow row = buildStockInoutSummaryRow(scope, type, header, line, item);
                    if (StringUtils.hasText(statisticModeValue) && !Objects.equals(row.statisticMode(), statisticModeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(warehouseValue) && !Objects.equals(row.warehouse(), warehouseValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(warehouseTypeValue) && !Objects.equals(row.warehouseType(), warehouseTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(inoutTypeValue) && !Objects.equals(row.inoutType(), inoutTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(inoutDirectionValue) && !Objects.equals(resolveStockDirection(type), inoutDirectionValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(oppositeOrgValue)
                            && !(containsIgnoreCase(row.oppositeOrg(), oppositeOrgValue)
                            || containsIgnoreCase(row.oppositeWarehouse(), oppositeOrgValue))) {
                        continue;
                    }
                    rows.add(row);
                }
            }
        }
        rows = aggregateStockInoutRows(rows);
        return page(rows, safePageNo, safePageSize);
    }

    public PageData<OtherInoutSummaryReportRow> otherInoutSummaryReport(Integer pageNo,
                                                                        Integer pageSize,
                                                                        String startDate,
                                                                        String endDate,
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
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String inoutTypeValue = trimNullable(inoutType);
        String reasonTypeValue = trimNullable(reasonType);
        String itemStatusValue = trimNullable(itemStatus);
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);

        List<OtherInoutSummaryReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : List.of(
                InventoryDocumentType.OTHER_INBOUND,
                InventoryDocumentType.OTHER_OUTBOUND,
                InventoryDocumentType.DAMAGE_OUTBOUND,
                InventoryDocumentType.PRODUCTION_INBOUND)) {
            List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
            Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryDocumentHeader header : headers) {
                if (!matchDate(header.getDocumentDate(), start, end)) {
                    continue;
                }
                String resolvedWarehouse = defaultIfBlank(header.getPrimaryName(), "");
                if (StringUtils.hasText(warehouseValue) && !Objects.equals(resolvedWarehouse, warehouseValue)) {
                    continue;
                }
                for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
                    if (item == null) {
                        continue;
                    }
                    if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(item.category(), itemCategoryValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(itemCodeValue) && !Objects.equals(item.itemCode(), itemCodeValue)) {
                        continue;
                    }
                    String rowInoutType = resolveOtherInoutType(type, header, line);
                    String rowReasonType = resolveOtherReasonType(header, line, type);
                    if (StringUtils.hasText(inoutTypeValue) && !Objects.equals(rowInoutType, inoutTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(reasonTypeValue) && !Objects.equals(rowReasonType, reasonTypeValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(itemStatusValue) && !Objects.equals(item.status(), itemStatusValue)) {
                        continue;
                    }
                    BigDecimal qty = defaultQuantity(line.getQuantity());
                    BigDecimal amount = defaultMoney(line.getAmount() == null ? qty.multiply(item.unitCost()) : line.getAmount());
                    rows.add(new OtherInoutSummaryReportRow(
                            summaryRowId(resolvedWarehouse, item.itemCode(), rowInoutType + rowReasonType),
                            item.itemCode(),
                            item.itemName(),
                            item.spec(),
                            item.category(),
                            item.stockUnit(),
                            resolvedWarehouse,
                            rowInoutType,
                            rowReasonType,
                            qty,
                            amount
                    ));
                }
            }
        }
        rows = aggregateOtherInoutRows(rows);
        return page(rows, safePageNo, safePageSize);
    }

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
        List<InterOrgTransferDetailReportRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : List.of(InventoryDocumentType.STOCK_TRANSFER, InventoryDocumentType.STOCK_TRANSFER_INBOUND, InventoryDocumentType.STORE_TRANSFER, InventoryDocumentType.DEPARTMENT_TRANSFER)) {
            List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
            Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryDocumentHeader header : headers) {
                if (!matchDate(header.getDocumentDate(), start, end)) {
                    continue;
                }
                if (StringUtils.hasText(documentStatusValue) && !Objects.equals(defaultIfBlank(header.getStatus(), ""), documentStatusValue)) {
                    continue;
                }
                String resolvedSource = defaultIfBlank(header.getPrimaryName(), "");
                String resolvedTarget = defaultIfBlank(header.getSecondaryName(), defaultIfBlank(header.getCounterpartyName(), ""));
                if (StringUtils.hasText(sourceStoreValue) && !containsIgnoreCase(resolvedSource, sourceStoreValue)) {
                    continue;
                }
                if (StringUtils.hasText(targetStoreValue) && !containsIgnoreCase(resolvedTarget, targetStoreValue)) {
                    continue;
                }
                if (StringUtils.hasText(sourceWarehouseValue) && !containsIgnoreCase(resolvedSource, sourceWarehouseValue)) {
                    continue;
                }
                if (StringUtils.hasText(targetWarehouseValue) && !containsIgnoreCase(resolvedTarget, targetWarehouseValue)) {
                    continue;
                }
                for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    ItemProfileSnapshot item = itemProfiles.get(defaultIfBlank(line.getItemCode(), ""));
                    if (item == null) {
                        continue;
                    }
                    if (StringUtils.hasText(itemNameValue) && !containsIgnoreCase(line.getItemName(), itemNameValue)) {
                        continue;
                    }
                    if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(item.category(), itemCategoryValue)) {
                        continue;
                    }
                    InterOrgTransferDetailReportRow row = buildInterOrgTransferDetailRow(scope, type, header, line, item, resolvedSource, resolvedTarget);
                    if (StringUtils.hasText(statisticModeValue) && !Objects.equals(row.statisticMode(), statisticModeValue)) {
                        continue;
                    }
                    rows.add(row);
                }
            }
        }
        rows.sort(Comparator.comparing(InterOrgTransferDetailReportRow::transferDate).reversed().thenComparing(InterOrgTransferDetailReportRow::transferNo));
        return page(rows, safePageNo, safePageSize);
    }

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
        PageData<InterOrgTransferDetailReportRow> detailPage = interOrgTransferDetailReport(pageNo, pageSize, statisticMode, dateType, startDate, endDate, targetStore, null, itemKeyword, itemCategory, null, null, null, orgId);
        List<InterOrgTransferSummaryReportRow> rows = aggregateInterOrgTransferSummaryRows(detailPage.list());
        return new PageData<>(rows, rows.size(), pageNo == null || pageNo < 1 ? 1 : pageNo, pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE));
    }

    public PageData<StockTurnoverRateReportRow> stockTurnoverRateReport(Integer pageNo,
                                                                        Integer pageSize,
                                                                        String statisticDimension,
                                                                        String statisticMethod,
                                                                        String startDate,
                                                                        String endDate,
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
        String statisticDimensionValue = trimNullable(statisticDimension);
        String statisticMethodValue = trimNullable(statisticMethod);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String itemStatusValue = trimNullable(itemStatus);
        String unitTypeValue = trimNullable(unitType);

        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, TxnSummary> summaryMap = loadTxnSummary(scope, start, end);
        List<StockTurnoverRateReportRow> rows = new ArrayList<>();
        for (InventoryBalanceDO balance : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
            if (item == null) {
                continue;
            }
            if (StringUtils.hasText(warehouseValue) && !containsIgnoreCase(balance.getWarehouseName(), warehouseValue)) {
                continue;
            }
            if (StringUtils.hasText(itemCategoryValue) && !Objects.equals(item.category(), itemCategoryValue)) {
                continue;
            }
            if (StringUtils.hasText(itemCodeValue) && !Objects.equals(item.itemCode(), itemCodeValue)) {
                continue;
            }
            if (StringUtils.hasText(itemStatusValue) && !Objects.equals(item.status(), itemStatusValue)) {
                continue;
            }
            if (StringUtils.hasText(unitTypeValue) && !Objects.equals(item.stockUnit(), unitTypeValue)) {
                continue;
            }
            String key = turnoverKey(balance.getWarehouseName(), item, statisticDimensionValue);
            TxnSummary txnSummary = summaryMap.getOrDefault(key, new TxnSummary());
            BigDecimal opening = txnSummary.openingQty;
            BigDecimal closing = txnSummary.closingQty == null ? defaultQuantity(balance.getQuantity()) : txnSummary.closingQty;
            BigDecimal outbound = txnSummary.outboundQty;
            BigDecimal openingAmount = opening.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closingAmount = closing.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgStockAmount = openingAmount.add(closingAmount).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal outboundAmount = outbound.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal turnoverRate = avgStockAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : outboundAmount.divide(avgStockAmount, 4, RoundingMode.HALF_UP);
            BigDecimal turnoverDays = turnoverRate.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(365).divide(turnoverRate, 4, RoundingMode.HALF_UP);
            rows.add(new StockTurnoverRateReportRow(
                    summaryRowId(balance.getWarehouseName(), item.itemCode(), key),
                    defaultOrgName(scope),
                    defaultIfBlank(balance.getWarehouseName(), ""),
                    item.itemName(),
                    item.itemCode(),
                    item.stockUnit(),
                    item.category(),
                    item.status(),
                    openingAmount,
                    closingAmount,
                    avgStockAmount,
                    outboundAmount,
                    turnoverRate,
                    turnoverDays
            ));
        }
        rows.sort(Comparator.comparing(StockTurnoverRateReportRow::warehouse).thenComparing(StockTurnoverRateReportRow::itemCode));
        if ("物品".equals(statisticDimensionValue)) {
            rows = aggregateStockTurnoverRows(rows);
        }
        if (StringUtils.hasText(statisticMethodValue) && Objects.equals(statisticMethodValue, "按数量计算")) {
            rows = convertTurnoverToQuantity(rows, scope, itemProfiles, summaryMap);
        }
        return page(rows, safePageNo, safePageSize);
    }

    private StockWarningReportRow toStockWarningRow(InventoryScope scope,
                                                    InventoryBalanceDO balance,
                                                    Map<String, ItemProfileSnapshot> itemProfiles) {
        ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
        BigDecimal currentStock = defaultQuantity(balance.getQuantity());
        BigDecimal lower = item == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : defaultQuantity(parseBigDecimal(item.stockMin()));
        BigDecimal upper = item == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : defaultQuantity(parseBigDecimal(item.stockMax()));
        String warningStatus;
        if (currentStock.compareTo(lower) < 0) {
            warningStatus = "库存不足";
        } else if (currentStock.compareTo(upper) > 0) {
            warningStatus = "库存超储";
        } else {
            warningStatus = "正常";
        }
        return new StockWarningReportRow(
                balance.getWarehouseName() + "|" + balance.getItemCode(),
                defaultIfBlank(balance.getItemCode(), ""),
                defaultIfBlank(balance.getItemName(), ""),
                defaultIfBlank(item == null ? null : item.stockUnit(), "库存单位"),
                defaultIfBlank(item == null ? null : item.category(), ""),
                defaultIfBlank(balance.getWarehouseName(), ""),
                currentStock,
                upper,
                lower,
                warningStatus,
                defaultIfBlank(item == null ? null : item.status(), "启用")
        );
    }

    private List<StockWarningReportRow> aggregateStockWarningRows(List<StockWarningReportRow> rows) {
        Map<String, MutableStockWarningRow> grouped = new LinkedHashMap<>();
        for (StockWarningReportRow row : rows) {
            grouped.computeIfAbsent(row.itemCode(), ignored -> new MutableStockWarningRow()).merge(row);
        }
        return grouped.values().stream().map(MutableStockWarningRow::toRow).toList();
    }

    private StagnantStockReportRow toStagnantStockRow(InventoryScope scope,
                                                      InventoryBalanceDO balance,
                                                      Map<String, ItemProfileSnapshot> itemProfiles,
                                                      Map<String, TransactionStats> statsMap) {
        ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
        TransactionStats stats = statsMap.getOrDefault(transactionKey(balance.getWarehouseName(), balance.getItemCode()), new TransactionStats());
        BigDecimal stockQty = defaultQuantity(balance.getQuantity());
        int retainedDays = stats.latestMovementTime() == null
                ? 0
                : (int) ChronoUnit.DAYS.between(stats.latestMovementTime().toLocalDate(), LocalDate.now());
        Integer stagnantDays = item == null ? 0 : item.stagnantDays();
        String stagnantStatus = stagnantDays != null && stagnantDays > 0 && retainedDays >= stagnantDays && stockQty.compareTo(BigDecimal.ZERO) > 0
                ? "是"
                : "否";
        return new StagnantStockReportRow(
                balance.getWarehouseName() + "|" + balance.getItemCode(),
                defaultIfBlank(balance.getWarehouseName(), ""),
                defaultIfBlank(balance.getItemName(), ""),
                defaultIfBlank(balance.getItemCode(), ""),
                defaultIfBlank(item == null ? null : item.spec(), ""),
                defaultIfBlank(item == null ? null : item.stockUnit(), "库存单位"),
                formatDateTime(stats.firstInboundTime()),
                formatDateTime(stats.latestInboundTime()),
                formatDateTime(stats.latestOutboundTime()),
                stats.latestInboundQty(),
                stats.latestOutboundQty(),
                stockQty,
                retainedDays,
                stagnantDays == null ? 0 : stagnantDays,
                stagnantStatus,
                defaultIfBlank(item == null ? null : item.status(), "启用"),
                defaultIfBlank(item == null ? null : item.category(), "")
        );
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
                defaultIfBlank(trimNullable(request.statType()), ""),
                resolveUnitCost(request)
        );
    }

    private Map<String, TransactionStats> loadTransactionStats(InventoryScope scope) {
        List<InventoryTransactionDO> transactions = namedParameterJdbcTemplate.query(
                "SELECT * FROM dev.inventory_transaction WHERE scope_type = :scopeType AND scope_id = :scopeId ORDER BY created_at ASC, id ASC",
                new MapSqlParameterSource()
                        .addValue("scopeType", scope.scopeType())
                        .addValue("scopeId", scope.scopeId()),
                (rs, rowNum) -> {
                    InventoryTransactionDO transaction = new InventoryTransactionDO();
                    transaction.setScopeType(rs.getString("scope_type"));
                    transaction.setScopeId(rs.getLong("scope_id"));
                    transaction.setBizType(rs.getString("biz_type"));
                    transaction.setBizId(rs.getLong("biz_id"));
                    transaction.setBizLineId(rs.getObject("biz_line_id", Long.class));
                    transaction.setWarehouseName(rs.getString("warehouse_name"));
                    transaction.setItemCode(rs.getString("item_code"));
                    transaction.setItemName(rs.getString("item_name"));
                    transaction.setQuantityDelta(rs.getBigDecimal("quantity_delta"));
                    transaction.setBeforeQty(rs.getBigDecimal("before_qty"));
                    transaction.setAfterQty(rs.getBigDecimal("after_qty"));
                    transaction.setOperatorId(rs.getObject("operator_id", Long.class));
                    transaction.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    return transaction;
                }
        );
        Map<String, TransactionStats> statsMap = new LinkedHashMap<>();
        for (InventoryTransactionDO transaction : transactions) {
            String key = transactionKey(transaction.getWarehouseName(), transaction.getItemCode());
            TransactionStats stats = statsMap.computeIfAbsent(key, ignored -> new TransactionStats());
            if (transaction.getQuantityDelta() != null && transaction.getQuantityDelta().compareTo(BigDecimal.ZERO) > 0) {
                stats.latestInboundTime = transaction.getCreatedAt();
                stats.latestInboundQty = defaultQuantity(transaction.getQuantityDelta());
                if (stats.firstInboundTime == null) {
                    stats.firstInboundTime = transaction.getCreatedAt();
                }
            } else if (transaction.getQuantityDelta() != null && transaction.getQuantityDelta().compareTo(BigDecimal.ZERO) < 0) {
                stats.latestOutboundTime = transaction.getCreatedAt();
                stats.latestOutboundQty = defaultQuantity(transaction.getQuantityDelta().abs());
                if (stats.firstInboundTime == null) {
                    stats.firstInboundTime = transaction.getCreatedAt();
                }
            }
            if (stats.latestMovementTime == null || transaction.getCreatedAt() != null && transaction.getCreatedAt().isAfter(stats.latestMovementTime)) {
                stats.latestMovementTime = transaction.getCreatedAt();
            }
        }
        return statsMap;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "";
        }
        Optional<UserAccountDO> user = userAccountRepository.findById(userId);
        return user.map(UserAccountDO::getRealName).filter(StringUtils::hasText).orElse(String.valueOf(userId));
    }

    private Map<String, TxnSummary> loadTxnSummary(InventoryScope scope, LocalDate start, LocalDate end) {
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.plusDays(1).atStartOfDay().minusNanos(1);
        StringBuilder sql = new StringBuilder("SELECT * FROM dev.inventory_transaction WHERE scope_type = :scopeType AND scope_id = :scopeId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("scopeType", scope.scopeType())
                .addValue("scopeId", scope.scopeId());
        if (startTime != null) {
            sql.append(" AND created_at >= :startTime");
            params.addValue("startTime", startTime);
        }
        if (endTime != null) {
            sql.append(" AND created_at <= :endTime");
            params.addValue("endTime", endTime);
        }
        sql.append(" ORDER BY created_at ASC, id ASC");
        List<InventoryTransactionDO> transactions = namedParameterJdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            InventoryTransactionDO transaction = new InventoryTransactionDO();
            transaction.setScopeType(rs.getString("scope_type"));
            transaction.setScopeId(rs.getLong("scope_id"));
            transaction.setBizType(rs.getString("biz_type"));
            transaction.setBizId(rs.getLong("biz_id"));
            transaction.setBizLineId(rs.getObject("biz_line_id", Long.class));
            transaction.setWarehouseName(rs.getString("warehouse_name"));
            transaction.setItemCode(rs.getString("item_code"));
            transaction.setItemName(rs.getString("item_name"));
            transaction.setQuantityDelta(rs.getBigDecimal("quantity_delta"));
            transaction.setBeforeQty(rs.getBigDecimal("before_qty"));
            transaction.setAfterQty(rs.getBigDecimal("after_qty"));
            transaction.setOperatorId(rs.getObject("operator_id", Long.class));
            transaction.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            return transaction;
        });
        Map<String, TxnSummary> summaryMap = new LinkedHashMap<>();
        Map<String, InventoryBalanceDO> balanceMap = inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .collect(Collectors.toMap(
                        balance -> balance.getWarehouseName() + "|" + balance.getItemCode(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (InventoryTransactionDO transaction : transactions) {
            String key = transaction.getWarehouseName() + "|" + transaction.getItemCode();
            TxnSummary summary = summaryMap.computeIfAbsent(key, ignored -> new TxnSummary());
            BigDecimal delta = defaultQuantity(transaction.getQuantityDelta());
            if (delta.compareTo(BigDecimal.ZERO) >= 0) {
                summary.inboundQty = summary.inboundQty.add(delta);
            } else {
                summary.outboundQty = summary.outboundQty.add(delta.abs());
                if (isPurchaseReturnBizType(transaction.getBizType())) {
                    summary.returnDifferenceQty = summary.returnDifferenceQty.add(delta.abs());
                }
            }
            if (summary.openingQty.compareTo(BigDecimal.ZERO) == 0 && transaction.getBeforeQty() != null) {
                summary.openingQty = defaultQuantity(transaction.getBeforeQty());
            }
            summary.closingQty = defaultQuantity(transaction.getAfterQty());
        }
        for (Map.Entry<String, InventoryBalanceDO> entry : balanceMap.entrySet()) {
            TxnSummary summary = summaryMap.computeIfAbsent(entry.getKey(), ignored -> new TxnSummary());
            if (summary.closingQty.compareTo(BigDecimal.ZERO) == 0) {
                summary.closingQty = defaultQuantity(entry.getValue().getQuantity());
            }
        }
        return summaryMap;
    }

    private Map<String, InventoryProfitLossAgg> loadInventoryCheckAgg(InventoryScope scope, LocalDate start, LocalDate end) {
        Map<String, InventoryProfitLossAgg> result = new LinkedHashMap<>();
        for (InventoryCheckKind kind : List.of(InventoryCheckKind.INVENTORY_CHECK, InventoryCheckKind.MULTI_INVENTORY_CHECK)) {
            List<InventoryCheckHeader> headers = inventoryCheckRepository.findHeadersByScopeAndKindOrdered(kind, scope.scopeType(), scope.scopeId());
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
        Map<String, MutableInventoryInoutSummaryRow> grouped = new LinkedHashMap<>();
        for (InventoryInoutSummaryReportRow row : rows) {
            String key = row.warehouse() + "|" + defaultIfBlank(row.itemCategory(), "");
            grouped.computeIfAbsent(key, ignored -> new MutableInventoryInoutSummaryRow()).merge(row);
        }
        return grouped.values().stream().map(MutableInventoryInoutSummaryRow::toRow).toList();
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

    private List<StockTurnoverRateReportRow> convertTurnoverToQuantity(List<StockTurnoverRateReportRow> rows,
                                                                       InventoryScope scope,
                                                                       Map<String, ItemProfileSnapshot> itemProfiles,
                                                                       Map<String, TxnSummary> summaryMap) {
        List<StockTurnoverRateReportRow> converted = new ArrayList<>();
        for (StockTurnoverRateReportRow row : rows) {
            ItemProfileSnapshot item = itemProfiles.get(row.itemCode());
            if (item == null) {
                converted.add(row);
                continue;
            }
            String key = turnoverKey(row.warehouse(), item, "物品");
            TxnSummary summary = summaryMap.getOrDefault(key, new TxnSummary());
            BigDecimal opening = summary.openingQty;
            BigDecimal closing = summary.closingQty;
            BigDecimal outbound = summary.outboundQty;
            BigDecimal avg = opening.add(closing).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            BigDecimal rate = avg.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : outbound.divide(avg, 4, RoundingMode.HALF_UP);
            BigDecimal days = rate.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(365).divide(rate, 4, RoundingMode.HALF_UP);
            converted.add(new StockTurnoverRateReportRow(
                    row.id(),
                    row.orgName(),
                    row.warehouse(),
                    row.itemName(),
                    row.itemCode(),
                    row.unit(),
                    row.itemCategory(),
                    row.itemStatus(),
                    opening.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP),
                    closing.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP),
                    avg.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP),
                    outbound.multiply(item.unitCost()).setScale(2, RoundingMode.HALF_UP),
                    rate,
                    days
            ));
        }
        return converted;
    }

    private StockInoutSummaryReportRow buildStockInoutSummaryRow(InventoryScope scope,
                                                                 InventoryDocumentType type,
                                                                 InventoryDocumentHeader header,
                                                                 InventoryDocumentLine line,
                                                                 ItemProfileSnapshot item) {
        String inoutType = resolveStockInoutType(type);
        String direction = resolveStockDirection(type);
        BigDecimal qty = defaultQuantity(line.getQuantity());
        BigDecimal amount = defaultMoney(line.getAmount() == null ? qty.multiply(item.unitCost()) : line.getAmount());
        BigDecimal avg = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.divide(qty, 2, RoundingMode.HALF_UP);
        return new StockInoutSummaryReportRow(
                summaryRowId(resolveStockWarehouse(type, header), item.itemCode(), inoutType + defaultIfBlank(header.getCounterpartyName(), "")),
                item.itemCode(),
                item.itemName(),
                item.spec(),
                item.category(),
                item.statisticsType(),
                item.stockUnit(),
                inoutType,
                defaultIfBlank(resolveStockWarehouse(type, header), ""),
                resolveWarehouseType(scope, resolveStockWarehouse(type, header)),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getSecondaryName(), defaultIfBlank(header.getCounterpartyName(), "")),
                "入库".equals(direction) ? qty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                "入库".equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "入库".equals(direction) ? avg : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "入库".equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "入库".equals(direction) ? avg : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "出库".equals(direction) ? qty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                "出库".equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "出库".equals(direction) ? avg : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "出库".equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "出库".equals(direction) ? avg : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                resolveStockStatisticMode(type)
        );
    }

    private InterOrgTransferDetailReportRow buildInterOrgTransferDetailRow(InventoryScope scope,
                                                                           InventoryDocumentType type,
                                                                           InventoryDocumentHeader header,
                                                                           InventoryDocumentLine line,
                                                                           ItemProfileSnapshot item,
                                                                           String sourceStore,
                                                                           String targetStore) {
        BigDecimal qty = defaultQuantity(line.getQuantity());
        BigDecimal amount = defaultMoney(line.getAmount() == null ? qty.multiply(item.unitCost()) : line.getAmount());
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
                defaultIfBlank(header.getSecondaryName(), ""),
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
        return switch (type) {
            case PURCHASE_INBOUND, DEPARTMENT_RETURN, OTHER_INBOUND, PRODUCTION_INBOUND, CUSTOMER_RETURN_INBOUND, STOCK_TRANSFER_INBOUND -> "调入本店";
            default -> "从本店调出";
        };
    }

    private String resolveStockInoutType(InventoryDocumentType type) {
        return switch (type) {
            case PURCHASE_INBOUND -> "采购入库";
            case PURCHASE_RETURN_OUTBOUND -> "采购退货出库";
            case DEPARTMENT_PICKING -> "部门领料";
            case DEPARTMENT_RETURN -> "部门退料";
            case STOCK_TRANSFER -> "移库出库";
            case STOCK_TRANSFER_INBOUND -> "移库入库";
            case DEPARTMENT_TRANSFER -> "部门调拨";
            case DAMAGE_OUTBOUND -> "报损出库";
            case OTHER_INBOUND -> "其他入库";
            case OTHER_OUTBOUND -> "其他出库";
            case PRODUCTION_INBOUND -> "生产入库";
            case CUSTOMER_SALES_OUTBOUND -> "客户销售出库";
            case CUSTOMER_RETURN_INBOUND -> "客户退货入库";
            case DISH_CONSUMPTION_OUTBOUND -> "菜品消耗出库";
            case STORE_TRANSFER -> "店间调拨";
            case STOCK_TRANSFER_OUTBOUND -> "移库出库";
            default -> type.getBusinessName();
        };
    }

    private String resolveStockDirection(InventoryDocumentType type) {
        return switch (type) {
            case PURCHASE_INBOUND, DEPARTMENT_RETURN, OTHER_INBOUND, PRODUCTION_INBOUND, CUSTOMER_RETURN_INBOUND, STOCK_TRANSFER_INBOUND -> "入库";
            default -> "出库";
        };
    }

    private String resolveStockWarehouse(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.PURCHASE_INBOUND || type == InventoryDocumentType.OTHER_INBOUND || type == InventoryDocumentType.PRODUCTION_INBOUND || type == InventoryDocumentType.CUSTOMER_RETURN_INBOUND || type == InventoryDocumentType.STOCK_TRANSFER_INBOUND) {
            return defaultIfBlank(header.getPrimaryName(), "");
        }
        return defaultIfBlank(header.getPrimaryName(), defaultIfBlank(header.getCounterpartyName(), ""));
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

    private BigDecimal resolveUnitCost(ItemCreateRequest request) {
        String preferred = trimNullable(request.suggestPurchasePrice());
        if (!StringUtils.hasText(preferred)) {
            preferred = trimNullable(request.productionRefCost());
        }
        if (!StringUtils.hasText(preferred)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(preferred).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
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
        return new InventoryScope(scope.scopeType(), scope.scopeId());
    }

    private static BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
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

    private BigDecimal parseBigDecimal(String value) {
        String normalized = normalizeBigDecimalString(value);
        if (!StringUtils.hasText(normalized)) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return new BigDecimal(normalized).setScale(4, RoundingMode.HALF_UP);
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
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private record InventoryScope(String scopeType, Long scopeId) {
    }

    private record ItemProfileSnapshot(String itemCode,
                                       String itemName,
                                       String category,
                                       String stockUnit,
                                       String spec,
                                       String stockMin,
                                       String stockMax,
                                       Integer warningDays,
                                       Integer stagnantDays,
                                       String status,
                                       String statisticsType,
                                       BigDecimal unitCost) {
    }

    private static final class TransactionStats {
        private LocalDateTime firstInboundTime;
        private LocalDateTime latestInboundTime;
        private LocalDateTime latestOutboundTime;
        private LocalDateTime latestMovementTime;
        private BigDecimal latestInboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal latestOutboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        private LocalDateTime firstInboundTime() {
            return firstInboundTime;
        }

        private LocalDateTime latestInboundTime() {
            return latestInboundTime;
        }

        private LocalDateTime latestOutboundTime() {
            return latestOutboundTime;
        }

        private LocalDateTime latestMovementTime() {
            return latestMovementTime;
        }

        private BigDecimal latestInboundQty() {
            return latestInboundQty;
        }

        private BigDecimal latestOutboundQty() {
            return latestOutboundQty;
        }
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    public record StockWarningReportRow(String id,
                                        String itemCode,
                                        String itemName,
                                        String unit,
                                        String itemCategory,
                                        String warehouse,
                                        BigDecimal currentStock,
                                        BigDecimal stockUpperLimit,
                                        BigDecimal stockLowerLimit,
                                        String warningStatus,
                                        String itemStatus) {
    }

    private static final class MutableStockWarningRow {
        private String id = "";
        private String itemCode = "";
        private String itemName = "";
        private String unit = "";
        private String itemCategory = "";
        private String warehouse = "";
        private BigDecimal currentStock = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal stockUpperLimit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal stockLowerLimit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private String warningStatus = "正常";
        private String itemStatus = "";

        private void merge(StockWarningReportRow row) {
            if (!StringUtils.hasText(id)) {
                id = row.id();
            }
            itemCode = row.itemCode();
            itemName = row.itemName();
            unit = row.unit();
            itemCategory = row.itemCategory();
            warehouse = row.warehouse();
            currentStock = currentStock.add(defaultQuantity(row.currentStock()));
            stockUpperLimit = row.stockUpperLimit();
            stockLowerLimit = row.stockLowerLimit();
            warningStatus = row.warningStatus();
            itemStatus = row.itemStatus();
        }

        private StockWarningReportRow toRow() {
            return new StockWarningReportRow(id, itemCode, itemName, unit, itemCategory, warehouse, currentStock, stockUpperLimit, stockLowerLimit, warningStatus, itemStatus);
        }
    }

    public record StagnantStockReportRow(String id,
                                         String warehouse,
                                         String itemName,
                                         String itemCode,
                                         String specModel,
                                         String unit,
                                         String firstInboundTime,
                                         String latestInboundTime,
                                         String latestOutboundTime,
                                         BigDecimal latestInboundQty,
                                         BigDecimal latestOutboundQty,
                                         BigDecimal stockQty,
                                         int retainedDays,
                                         int itemStagnantDays,
                                         String stagnant,
                                         String itemStatus,
                                         String itemCategory) {
    }

    public record InventoryProfitLossReportRow(String id,
                                               String itemCode,
                                               String itemName,
                                               String specModel,
                                               String itemCategory,
                                               String statisticsType,
                                               String unit,
                                               String checkDocumentNo,
                                               String checkType,
                                               String stockDocumentNo,
                                               String orgName,
                                               String orgCode,
                                               String warehouse,
                                               String checkTime,
                                               String auditTime,
                                               String auditor,
                                               BigDecimal bookQty,
                                               BigDecimal bookAmount,
                                               BigDecimal actualQty,
                                               BigDecimal actualAmount,
                                               BigDecimal profitLossQty,
                                               BigDecimal profitLossAmount,
                                               BigDecimal profitLossQtyAbs,
                                               BigDecimal profitLossAmountAbs,
                                               BigDecimal adjustmentAmount,
                                               String profitLossResult,
                                               BigDecimal profitInboundPrice,
                                               BigDecimal lossOutboundPrice,
                                               String checkReason,
                                               String remark) {
    }

    public record InventoryInoutSummaryReportRow(String id,
                                                 String itemCode,
                                                 String itemName,
                                                 String specModel,
                                                 String itemCategory,
                                                 String statisticType,
                                                 String unit,
                                                 String orgName,
                                                 String orgCode,
                                                 String warehouse,
                                                 String warehouseType,
                                                 BigDecimal openingQty,
                                                 BigDecimal openingCostAmountExTax,
                                                 BigDecimal openingAvgCostExTax,
                                                 BigDecimal inboundQty,
                                                 BigDecimal inboundCostAmountExTax,
                                                 BigDecimal inboundAvgCostExTax,
                                                 BigDecimal outboundQty,
                                                 BigDecimal outboundCostAmountExTax,
                                                 BigDecimal outboundAvgCostExTax,
                                                 BigDecimal closingQty,
                                                 BigDecimal closingCostAmountExTax,
                                                 BigDecimal closingAvgCostExTax,
                                                 BigDecimal inventoryProfitLossQty,
                                                 BigDecimal inventoryProfitLossCostAmountTaxIncluded,
                                                 BigDecimal inventoryProfitLossCostAmountExTax,
                                                 BigDecimal inventoryCheckQty,
                                                 BigDecimal inventoryCheckCostAmountTaxIncluded,
                                                 BigDecimal closingCheckDiffQty,
                                                 BigDecimal closingCheckDiffAmountExTax,
                                                 BigDecimal returnDifferenceQty,
                                                 BigDecimal returnDifferenceCostAmountExTax,
                                                 String inoutType) {
        private boolean isEmpty() {
            return openingQty.compareTo(BigDecimal.ZERO) == 0
                    && inboundQty.compareTo(BigDecimal.ZERO) == 0
                    && outboundQty.compareTo(BigDecimal.ZERO) == 0
                    && closingQty.compareTo(BigDecimal.ZERO) == 0
                    && inventoryProfitLossQty.compareTo(BigDecimal.ZERO) == 0
                    && inventoryCheckQty.compareTo(BigDecimal.ZERO) == 0
                    && closingCheckDiffQty.compareTo(BigDecimal.ZERO) == 0
                    && returnDifferenceQty.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    public record StockInoutSummaryReportRow(String id,
                                             String itemCode,
                                             String itemName,
                                             String specModel,
                                             String itemCategory,
                                             String statisticType,
                                             String unit,
                                             String inoutType,
                                             String warehouse,
                                             String warehouseType,
                                             String oppositeOrg,
                                             String oppositeWarehouse,
                                             BigDecimal inboundQty,
                                             BigDecimal inboundCostAmountTaxIncluded,
                                             BigDecimal inboundAvgCostTaxIncluded,
                                             BigDecimal inboundSettlementAmountTaxIncluded,
                                             BigDecimal inboundAvgSettlementTaxIncluded,
                                             BigDecimal outboundQty,
                                             BigDecimal outboundCostAmountTaxIncluded,
                                             BigDecimal outboundAvgCostTaxIncluded,
                                             BigDecimal outboundSettlementAmountTaxIncluded,
                                             BigDecimal outboundAvgSettlementTaxIncluded,
                                             String statisticMode) {
    }

    public record OtherInoutSummaryReportRow(String id,
                                             String itemCode,
                                             String itemName,
                                             String specModel,
                                             String itemCategory,
                                             String baseUnit,
                                             String warehouse,
                                             String inoutType,
                                             String reasonType,
                                             BigDecimal quantity,
                                             BigDecimal amountExTax) {
    }

    public record InterOrgTransferDetailReportRow(String id,
                                                  String transferNo,
                                                  String itemCode,
                                                  String itemName,
                                                  String documentStatus,
                                                  String transferDate,
                                                  String outboundAuditTime,
                                                  String sourceStore,
                                                  String sourceWarehouse,
                                                  String inboundDate,
                                                  String inboundAuditTime,
                                                  String targetStore,
                                                  String targetWarehouse,
                                                  String specModel,
                                                  String itemCategory,
                                                  String baseUnit,
                                                  BigDecimal transferBaseQty,
                                                  String businessUnit,
                BigDecimal transferQty,
                BigDecimal inboundAmountTaxIncluded,
                BigDecimal outboundCostAmountExTax,
                BigDecimal outboundSettlementAmountTaxIncluded,
                BigDecimal inboundPriceTaxIncluded,
                BigDecimal outboundCostPriceExTax,
                BigDecimal outboundSettlementPriceTaxIncluded,
                String remark,
                String statisticMode) {
    }

    public record InterOrgTransferSummaryReportRow(String id,
                                                   String itemCode,
                                                   String itemName,
                                                   String sourceStore,
                                                   String targetStore,
                                                   String specModel,
                                                   String itemCategory,
                                                   String unit,
                                                   BigDecimal transferQty,
                                                   BigDecimal inboundAmountTaxIncluded,
                                                   BigDecimal outboundCostAmountExTax,
                                                   BigDecimal outboundSettlementAmountTaxIncluded,
                                                   BigDecimal inboundAvgPriceTaxIncluded,
                                                   BigDecimal outboundCostAvgPriceExTax,
                                                   BigDecimal outboundSettlementAvgPriceTaxIncluded) {
    }

    public record StockTurnoverRateReportRow(String id,
                                             String orgName,
                                             String warehouse,
                                             String itemName,
                                             String itemCode,
                                             String unit,
                                             String itemCategory,
                                             String itemStatus,
                                             BigDecimal openingAmount,
                                             BigDecimal closingAmount,
                                             BigDecimal avgStockAmount,
                                             BigDecimal outboundAmount,
                                             BigDecimal turnoverRate,
                                             BigDecimal turnoverDays) {
    }

    private static final class TxnSummary {
        private BigDecimal openingQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal inboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal outboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal closingQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal returnDifferenceQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private static final class InventoryProfitLossAgg {
        private BigDecimal inventoryProfitLossQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal inventoryCheckQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private static final class MutableInventoryInoutSummaryRow {
        private InventoryInoutSummaryReportRow row;

        private void merge(InventoryInoutSummaryReportRow value) {
            if (row == null) {
                row = value;
                return;
            }
            row = new InventoryInoutSummaryReportRow(
                    row.id(),
                    row.itemCode(),
                    row.itemName(),
                    row.specModel(),
                    row.itemCategory(),
                    row.statisticType(),
                    row.unit(),
                    row.orgName(),
                    row.orgCode(),
                    row.warehouse(),
                    row.warehouseType(),
                    row.openingQty().add(value.openingQty()),
                    row.openingCostAmountExTax().add(value.openingCostAmountExTax()),
                    row.openingAvgCostExTax(),
                    row.inboundQty().add(value.inboundQty()),
                    row.inboundCostAmountExTax().add(value.inboundCostAmountExTax()),
                    row.inboundAvgCostExTax(),
                    row.outboundQty().add(value.outboundQty()),
                    row.outboundCostAmountExTax().add(value.outboundCostAmountExTax()),
                    row.outboundAvgCostExTax(),
                    row.closingQty().add(value.closingQty()),
                    row.closingCostAmountExTax().add(value.closingCostAmountExTax()),
                    row.closingAvgCostExTax(),
                    row.inventoryProfitLossQty().add(value.inventoryProfitLossQty()),
                    row.inventoryProfitLossCostAmountTaxIncluded().add(value.inventoryProfitLossCostAmountTaxIncluded()),
                    row.inventoryProfitLossCostAmountExTax().add(value.inventoryProfitLossCostAmountExTax()),
                    row.inventoryCheckQty().add(value.inventoryCheckQty()),
                    row.inventoryCheckCostAmountTaxIncluded().add(value.inventoryCheckCostAmountTaxIncluded()),
                    row.closingCheckDiffQty().add(value.closingCheckDiffQty()),
                    row.closingCheckDiffAmountExTax().add(value.closingCheckDiffAmountExTax()),
                    row.returnDifferenceQty().add(value.returnDifferenceQty()),
                    row.returnDifferenceCostAmountExTax().add(value.returnDifferenceCostAmountExTax()),
                    row.inoutType()
            );
        }

        private InventoryInoutSummaryReportRow toRow() {
            return row;
        }
    }

    private static final class MutableStockInoutSummaryRow {
        private StockInoutSummaryReportRow row;

        private void merge(StockInoutSummaryReportRow value) {
            if (row == null) {
                row = value;
                return;
            }
            row = new StockInoutSummaryReportRow(
                    row.id(),
                    row.itemCode(),
                    row.itemName(),
                    row.specModel(),
                    row.itemCategory(),
                    row.statisticType(),
                    row.unit(),
                    row.inoutType(),
                    row.warehouse(),
                    row.warehouseType(),
                    row.oppositeOrg(),
                    row.oppositeWarehouse(),
                    row.inboundQty().add(value.inboundQty()),
                    row.inboundCostAmountTaxIncluded().add(value.inboundCostAmountTaxIncluded()),
                    row.inboundAvgCostTaxIncluded(),
                    row.inboundSettlementAmountTaxIncluded().add(value.inboundSettlementAmountTaxIncluded()),
                    row.inboundAvgSettlementTaxIncluded(),
                    row.outboundQty().add(value.outboundQty()),
                    row.outboundCostAmountTaxIncluded().add(value.outboundCostAmountTaxIncluded()),
                    row.outboundAvgCostTaxIncluded(),
                    row.outboundSettlementAmountTaxIncluded().add(value.outboundSettlementAmountTaxIncluded()),
                    row.outboundAvgSettlementTaxIncluded(),
                    row.statisticMode()
            );
        }

        private StockInoutSummaryReportRow toRow() {
            return row;
        }
    }

    private static final class MutableOtherInoutSummaryRow {
        private OtherInoutSummaryReportRow row;

        private void merge(OtherInoutSummaryReportRow value) {
            if (row == null) {
                row = value;
                return;
            }
            row = new OtherInoutSummaryReportRow(
                    row.id(),
                    row.itemCode(),
                    row.itemName(),
                    row.specModel(),
                    row.itemCategory(),
                    row.baseUnit(),
                    row.warehouse(),
                    row.inoutType(),
                    row.reasonType(),
                    row.quantity().add(value.quantity()),
                    row.amountExTax().add(value.amountExTax())
            );
        }

        private OtherInoutSummaryReportRow toRow() {
            return row;
        }
    }

    private static final class MutableInterOrgTransferSummaryRow {
        private InterOrgTransferSummaryReportRow row;

        private void merge(InterOrgTransferDetailReportRow value) {
            if (row == null) {
                row = new InterOrgTransferSummaryReportRow(
                        value.id(),
                        value.itemCode(),
                        value.itemName(),
                        value.sourceStore(),
                        value.targetStore(),
                        value.specModel(),
                        value.itemCategory(),
                        value.baseUnit(),
                        value.transferQty(),
                        value.inboundAmountTaxIncluded(),
                        value.outboundCostAmountExTax(),
                        value.outboundSettlementAmountTaxIncluded(),
                        value.inboundPriceTaxIncluded(),
                        value.outboundCostPriceExTax(),
                        value.outboundSettlementPriceTaxIncluded()
                );
                return;
            }
            row = new InterOrgTransferSummaryReportRow(
                    row.id(),
                    row.itemCode(),
                    row.itemName(),
                    row.sourceStore(),
                    row.targetStore(),
                    row.specModel(),
                    row.itemCategory(),
                    row.unit(),
                    row.transferQty().add(value.transferQty()),
                    row.inboundAmountTaxIncluded().add(value.inboundAmountTaxIncluded()),
                    row.outboundCostAmountExTax().add(value.outboundCostAmountExTax()),
                    row.outboundSettlementAmountTaxIncluded().add(value.outboundSettlementAmountTaxIncluded()),
                    row.inboundAvgPriceTaxIncluded(),
                    row.outboundCostAvgPriceExTax(),
                    row.outboundSettlementAvgPriceTaxIncluded()
            );
        }

        private InterOrgTransferSummaryReportRow toRow() {
            return row;
        }
    }

    private static final class MutableStockTurnoverRateRow {
        private StockTurnoverRateReportRow row;

        private void merge(StockTurnoverRateReportRow value) {
            if (row == null) {
                row = value;
                return;
            }
            row = new StockTurnoverRateReportRow(
                    row.id(),
                    row.orgName(),
                    row.warehouse(),
                    row.itemName(),
                    row.itemCode(),
                    row.unit(),
                    row.itemCategory(),
                    row.itemStatus(),
                    row.openingAmount().add(value.openingAmount()),
                    row.closingAmount().add(value.closingAmount()),
                    row.avgStockAmount().add(value.avgStockAmount()),
                    row.outboundAmount().add(value.outboundAmount()),
                    row.turnoverRate().add(value.turnoverRate()),
                    row.turnoverDays().add(value.turnoverDays())
            );
        }

        private StockTurnoverRateReportRow toRow() {
            return row;
        }
    }
}
