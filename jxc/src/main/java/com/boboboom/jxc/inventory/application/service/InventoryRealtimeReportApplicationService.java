package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryBatchBalanceRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchBalanceDO;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 实时库存和库存预警报表业务。 */
@Service
public class InventoryRealtimeReportApplicationService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int INVENTORY_QUANTITY_SCALE = 4;

    private final OrgScopeService orgScopeService;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryBatchBalanceRepository inventoryBatchBalanceRepository;
    private final ItemProfileRepository itemProfileRepository;
    private final WarehouseRepository warehouseRepository;
    private final ObjectMapper objectMapper;

    /** 构造实时库存报表服务。 */
    public InventoryRealtimeReportApplicationService(OrgScopeService orgScopeServiceValue,
                                                     InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                                     InventoryBatchBalanceRepository inventoryBatchBalanceRepositoryValue,
                                                     ItemProfileRepository itemProfileRepositoryValue,
                                                     WarehouseRepository warehouseRepositoryValue,
                                                     ObjectMapper objectMapperValue) {
        this.orgScopeService = orgScopeServiceValue;
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.inventoryBatchBalanceRepository = inventoryBatchBalanceRepositoryValue;
        this.itemProfileRepository = itemProfileRepositoryValue;
        this.warehouseRepository = warehouseRepositoryValue;
        this.objectMapper = objectMapperValue;
    }

    /** 查询实时库存正式报表。 */
    public PageData<RealtimeStockReportRow> realtimeStockReport(Integer pageNo,
                                                                Integer pageSize,
                                                                String warehouse,
                                                                String itemCategory,
                                                                String itemCode,
                                                                String itemStatus,
                                                                String batchNo,
                                                                String shelfLifeStatus,
                                                                String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String itemStatusValue = trimNullable(itemStatus);
        String batchNoValue = trimNullable(batchNo);
        String shelfLifeStatusValue = trimNullable(shelfLifeStatus);
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        Map<String, List<InventoryBatchBalanceDO>> batchMap = inventoryBatchBalanceRepository
                .findByScopeOrdered(scope.scopeType(), scope.scopeId())
                .stream()
                .filter(batch -> defaultQuantity(batch.getQuantity()).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(batch -> transactionKey(batch.getWarehouseName(), batch.getItemCode()),
                        LinkedHashMap::new, Collectors.toList()));
        List<RealtimeStockReportRow> rows = collectRealtimeStockRows(scope, warehouseValue, itemCategoryValue,
                itemCodeValue, itemStatusValue, batchNoValue, shelfLifeStatusValue, itemProfiles, batchMap);
        rows.sort(Comparator.comparing(RealtimeStockReportRow::warehouse)
                .thenComparing(RealtimeStockReportRow::itemCode)
                .thenComparing(RealtimeStockReportRow::batchNo));
        return page(rows, safePageNo, safePageSize);
    }

    /** 查询库存预警报表。 */
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
        Map<String, ItemProfileSnapshot> itemProfiles = loadItemProfiles(scope);
        List<StockWarningReportRow> rows = new ArrayList<>(inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(balance -> toStockWarningRow(balance, itemProfiles))
                .toList());
        rows.addAll(loadBatchShelfLifeWarningRows(scope, itemProfiles));
        rows = filterStockWarningRows(rows, warehouse, itemCategory, itemCode, itemStatus, warningStatus, unitType);
        List<StockWarningReportRow> normalized = "仓库".equals(trimNullable(statisticDimension)) ? rows : aggregateStockWarningRows(rows);
        List<StockWarningReportRow> sorted = normalized.stream()
                .sorted(Comparator.comparing(StockWarningReportRow::itemCode)
                        .thenComparing(row -> defaultIfBlank(row.warehouse(), "")))
                .toList();
        return page(sorted, safePageNo, safePageSize);
    }

    private List<RealtimeStockReportRow> collectRealtimeStockRows(InventoryScope scope,
                                                                  String warehouse,
                                                                  String itemCategory,
                                                                  String itemCode,
                                                                  String itemStatus,
                                                                  String batchNo,
                                                                  String shelfLifeStatus,
                                                                  Map<String, ItemProfileSnapshot> itemProfiles,
                                                                  Map<String, List<InventoryBatchBalanceDO>> batchMap) {
        List<RealtimeStockReportRow> rows = new ArrayList<>();
        for (InventoryBalanceDO balance : inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
            if (item == null || !matchesRealtimeStockBalance(balance, item, warehouse, itemCategory, itemCode, itemStatus)) {
                continue;
            }
            appendRealtimeStockRowsForBalance(rows, scope, balance, item, batchNo, shelfLifeStatus, batchMap);
        }
        return rows;
    }

    private void appendRealtimeStockRowsForBalance(List<RealtimeStockReportRow> rows,
                                                   InventoryScope scope,
                                                   InventoryBalanceDO balance,
                                                   ItemProfileSnapshot item,
                                                   String batchNo,
                                                   String shelfLifeStatus,
                                                   Map<String, List<InventoryBatchBalanceDO>> batchMap) {
        List<InventoryBatchBalanceDO> batches = batchMap.getOrDefault(transactionKey(balance.getWarehouseName(), balance.getItemCode()), List.of());
        if (batches.isEmpty()) {
            rows.add(toRealtimeStockRow(scope, balance, item, null));
            return;
        }
        for (InventoryBatchBalanceDO batch : batches) {
            RealtimeStockReportRow row = toRealtimeStockRow(scope, balance, item, batch);
            if ((!StringUtils.hasText(batchNo) || containsIgnoreCase(row.batchNo(), batchNo))
                    && (!StringUtils.hasText(shelfLifeStatus) || Objects.equals(row.shelfLifeStatus(), shelfLifeStatus))) {
                rows.add(row);
            }
        }
    }

    private List<StockWarningReportRow> filterStockWarningRows(List<StockWarningReportRow> rows,
                                                               String warehouse,
                                                               String itemCategory,
                                                               String itemCode,
                                                               String itemStatus,
                                                               String warningStatus,
                                                               String unitType) {
        String warehouseValue = trimNullable(warehouse);
        String itemCategoryValue = trimNullable(itemCategory);
        String itemCodeValue = trimNullable(itemCode);
        String itemStatusValue = trimNullable(itemStatus);
        String warningStatusValue = trimNullable(warningStatus);
        String unitTypeValue = trimNullable(unitType);
        return rows.stream()
                .filter(row -> !StringUtils.hasText(warehouseValue) || Objects.equals(row.warehouse(), warehouseValue))
                .filter(row -> !StringUtils.hasText(itemCategoryValue) || Objects.equals(row.itemCategory(), itemCategoryValue))
                .filter(row -> !StringUtils.hasText(itemCodeValue) || row.itemCode().contains(itemCodeValue))
                .filter(row -> !StringUtils.hasText(itemStatusValue) || Objects.equals(row.itemStatus(), itemStatusValue))
                .filter(row -> !StringUtils.hasText(warningStatusValue) || Objects.equals(row.warningStatus(), warningStatusValue))
                .filter(row -> !StringUtils.hasText(unitTypeValue) || Objects.equals(row.unit(), unitTypeValue))
                .toList();
    }

    private StockWarningReportRow toStockWarningRow(InventoryBalanceDO balance, Map<String, ItemProfileSnapshot> itemProfiles) {
        ItemProfileSnapshot item = itemProfiles.get(balance.getItemCode());
        BigDecimal currentStock = defaultQuantity(balance.getQuantity());
        BigDecimal lower = item == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : defaultQuantity(parseBigDecimal(item.stockMin()));
        BigDecimal upper = item == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : defaultQuantity(parseBigDecimal(item.stockMax()));
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
                resolveStockLimitWarning(currentStock, lower, upper),
                defaultIfBlank(item == null ? null : item.status(), "启用"),
                "",
                "",
                "",
                "",
                ""
        );
    }

    private List<StockWarningReportRow> aggregateStockWarningRows(List<StockWarningReportRow> rows) {
        Map<String, MutableStockWarningRow> grouped = new LinkedHashMap<>();
        for (StockWarningReportRow row : rows) {
            grouped.computeIfAbsent(row.itemCode(), ignored -> new MutableStockWarningRow()).merge(row);
        }
        return grouped.values().stream().map(MutableStockWarningRow::toRow).toList();
    }

    private List<StockWarningReportRow> loadBatchShelfLifeWarningRows(InventoryScope scope,
                                                                      Map<String, ItemProfileSnapshot> itemProfiles) {
        LocalDate today = LocalDate.now();
        List<StockWarningReportRow> rows = new ArrayList<>();
        for (InventoryBatchBalanceDO batch : inventoryBatchBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())) {
            if (defaultQuantity(batch.getQuantity()).compareTo(BigDecimal.ZERO) <= 0 || batch.getExpiryDate() == null) {
                continue;
            }
            ItemProfileSnapshot item = itemProfiles.get(batch.getItemCode());
            String shelfLifeStatus = resolveShelfLifeStatus(batch.getExpiryDate(), item == null ? null : item.warningDays(), today);
            if (Objects.equals(shelfLifeStatus, "正常")) {
                continue;
            }
            rows.add(toBatchShelfLifeWarningRow(batch, item, shelfLifeStatus));
        }
        return rows;
    }

    private StockWarningReportRow toBatchShelfLifeWarningRow(InventoryBatchBalanceDO batch,
                                                             ItemProfileSnapshot item,
                                                             String shelfLifeStatus) {
        return new StockWarningReportRow(
                "batch|" + batch.getWarehouseName() + "|" + batch.getItemCode() + "|" + batch.getBatchNo(),
                defaultIfBlank(batch.getItemCode(), ""),
                defaultIfBlank(batch.getItemName(), ""),
                defaultIfBlank(item == null ? null : item.stockUnit(), "库存单位"),
                defaultIfBlank(item == null ? null : item.category(), ""),
                defaultIfBlank(batch.getWarehouseName(), ""),
                defaultQuantity(batch.getQuantity()),
                BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                shelfLifeStatus,
                defaultIfBlank(item == null ? null : item.status(), "启用"),
                defaultIfBlank(batch.getBatchNo(), ""),
                formatDate(batch.getProductionDate()),
                formatDate(batch.getExpiryDate()),
                shelfLifeStatus,
                defaultIfBlank(batch.getManufacturer(), "")
        );
    }

    private boolean matchesRealtimeStockBalance(InventoryBalanceDO balance,
                                                ItemProfileSnapshot item,
                                                String warehouse,
                                                String itemCategory,
                                                String itemCode,
                                                String itemStatus) {
        return (!StringUtils.hasText(warehouse) || Objects.equals(balance.getWarehouseName(), warehouse))
                && (!StringUtils.hasText(itemCategory) || Objects.equals(item.category(), itemCategory))
                && (!StringUtils.hasText(itemCode) || containsIgnoreCase(balance.getItemCode(), itemCode))
                && (!StringUtils.hasText(itemStatus) || Objects.equals(item.status(), itemStatus));
    }

    private RealtimeStockReportRow toRealtimeStockRow(InventoryScope scope,
                                                      InventoryBalanceDO balance,
                                                      ItemProfileSnapshot item,
                                                      InventoryBatchBalanceDO batch) {
        BigDecimal stockQty = batch == null ? defaultQuantity(balance.getQuantity()) : defaultQuantity(batch.getQuantity());
        BigDecimal stockCost = batch == null ? defaultMoney(balance.getCostAmount()) : defaultMoney(batch.getCostAmount());
        BigDecimal avgCost = batch == null ? defaultMoney(balance.getAvgCost()) : defaultMoney(batch.getAvgCost());
        String shelfLifeStatus = batch == null ? "" : resolveShelfLifeStatus(batch.getExpiryDate(), item.warningDays(), LocalDate.now());
        return new RealtimeStockReportRow(
                summaryRowId(balance.getWarehouseName(), balance.getItemCode(), batch == null ? "" : batch.getBatchNo()),
                item.itemCode(),
                item.itemName(),
                item.category(),
                item.stockUnit(),
                stockQty,
                stockQty,
                stockCost,
                avgCost,
                defaultIfBlank(balance.getWarehouseName(), ""),
                resolveWarehouseType(scope, balance.getWarehouseName()),
                item.spec(),
                batch == null ? "" : defaultIfBlank(batch.getBatchNo(), ""),
                batch == null ? "" : defaultIfBlank(batch.getManufacturer(), ""),
                batch == null ? "" : formatDate(batch.getProductionDate()),
                batch == null ? "" : formatDate(batch.getExpiryDate()),
                shelfLifeStatus,
                item.status()
        );
    }

    private String resolveStockLimitWarning(BigDecimal currentStock, BigDecimal lower, BigDecimal upper) {
        if (currentStock.compareTo(lower) < 0) {
            return "库存不足";
        }
        if (currentStock.compareTo(upper) > 0) {
            return "库存超储";
        }
        return "正常";
    }

    private String resolveShelfLifeStatus(LocalDate expiryDate, Integer warningDays, LocalDate today) {
        if (expiryDate == null) {
            return "";
        }
        if (expiryDate.isBefore(today)) {
            return "过期";
        }
        int safeWarningDays = warningDays == null ? 0 : warningDays;
        if (safeWarningDays > 0 && !expiryDate.isAfter(today.plusDays(safeWarningDays))) {
            return "临期";
        }
        return "正常";
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
                defaultIfBlank(trimNullable(request.status()), "启用")
        );
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
        if (StringUtils.hasText(request.defaultStockUnit())) {
            return request.defaultStockUnit();
        }
        if (StringUtils.hasText(request.defaultCostUnit())) {
            return request.defaultCostUnit();
        }
        if (StringUtils.hasText(request.defaultPurchaseUnit())) {
            return request.defaultPurchaseUnit();
        }
        return "库存单位";
    }

    private String resolveWarehouseType(InventoryScope scope, String warehouseName) {
        if (!StringUtils.hasText(warehouseName)) {
            return "";
        }
        for (WarehouseDO warehouse : loadScopeWarehouses(scope)) {
            if (Objects.equals(warehouse.getWarehouseName(), warehouseName)) {
                return defaultIfBlank(warehouse.getWarehouseType(), "");
            }
        }
        return "";
    }

    private List<WarehouseDO> loadScopeWarehouses(InventoryScope scope) {
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE)) {
            return warehouseRepository.findByStoreId(scope.scopeId());
        }
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP)) {
            return warehouseRepository.findByGroupId(scope.scopeId());
        }
        return List.of();
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
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

    private String transactionKey(String warehouseName, String itemCode) {
        return defaultIfBlank(warehouseName, "") + "|" + defaultIfBlank(itemCode, "");
    }

    private String summaryRowId(String left, String right, String third) {
        return defaultIfBlank(left, "") + "|" + defaultIfBlank(right, "") + "|" + defaultIfBlank(third, "");
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && StringUtils.hasText(keyword) && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
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
            return BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(normalized).setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }

    private record ItemProfileSnapshot(String itemCode,
                                       String itemName,
                                       String category,
                                       String stockUnit,
                                       String spec,
                                       String stockMin,
                                       String stockMax,
                                       Integer warningDays,
                                       String status) {
    }

    /** 库存分页数据模型。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    /** 实时库存行。 */
    public record RealtimeStockReportRow(String id,
                                         String itemCode,
                                         String itemName,
                                         String itemCategory,
                                         String unit,
                                         BigDecimal currentStock,
                                         BigDecimal availableStock,
                                         BigDecimal costAmount,
                                         BigDecimal avgCost,
                                         String warehouse,
                                         String warehouseType,
                                         String spec,
                                         String batchNo,
                                         String manufacturer,
                                         String productionDate,
                                         String expiryDate,
                                         String shelfLifeStatus,
                                         String itemStatus) { }

    /** 库存预警行。 */
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
                                        String itemStatus,
                                        String batchNo,
                                        String productionDate,
                                        String expiryDate,
                                        String shelfLifeStatus,
                                        String manufacturer) { }
}
