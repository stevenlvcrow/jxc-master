package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
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
    private final InventoryCheckRepository inventoryCheckRepository;
    private final ItemProfileRepository itemProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public InventorySupplementReportApplicationService(OrgScopeService orgScopeService,
                                                       InventoryBalanceRepository inventoryBalanceRepository,
                                                       InventoryCheckRepository inventoryCheckRepository,
                                                       ItemProfileRepository itemProfileRepository,
                                                       UserAccountRepository userAccountRepository,
                                                       NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                                       ObjectMapper objectMapper) {
        this.orgScopeService = orgScopeService;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.itemProfileRepository = itemProfileRepository;
        this.userAccountRepository = userAccountRepository;
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
                profitQty.abs(),
                profitAmount.abs(),
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
                                       String statisticsType) {
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
                                               String profitLossResult,
                                               BigDecimal profitInboundPrice,
                                               BigDecimal lossOutboundPrice,
                                               String checkReason,
                                               String remark) {
    }
}
