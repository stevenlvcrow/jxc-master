package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 库存报表业务服务。
 */
@Service
public class InventoryReportApplicationService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int INVENTORY_QUANTITY_SCALE = 4;
    private static final String STORE_TRANSFER_SOURCE_WAREHOUSE = "sourceWarehouse";

    private final OrgScopeService orgScopeService;
    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundLineRepository purchaseInboundLineRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final DataScopeAccessService dataScopeAccessService;
    private final ObjectMapper objectMapper;

    /** 库存报表业务服务，负责库存查询、出入库统计和报表行组装。 */
    public InventoryReportApplicationService(OrgScopeService orgScopeServiceValue,
                                             GroupRepository groupRepositoryValue,
                                             StoreRepository storeRepositoryValue,
                                             WarehouseRepository warehouseRepositoryValue,
                                             InventoryDocumentRepository inventoryDocumentRepositoryValue,
                                             PurchaseInboundRepository purchaseInboundRepositoryValue,
                                             PurchaseInboundLineRepository purchaseInboundLineRepositoryValue,
                                             InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                             DataScopeAccessService dataScopeAccessServiceValue,
                                             ObjectMapper objectMapperValue) {
        this.orgScopeService = orgScopeServiceValue;
        this.groupRepository = groupRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.warehouseRepository = warehouseRepositoryValue;
        this.inventoryDocumentRepository = inventoryDocumentRepositoryValue;
        this.purchaseInboundRepository = purchaseInboundRepositoryValue;
        this.purchaseInboundLineRepository = purchaseInboundLineRepositoryValue;
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
        this.objectMapper = objectMapperValue;
    }

    /** 查询菜品消耗出库报表。 */
    public DishConsumptionOutboundReportPage dishConsumptionOutboundReport(Integer pageNo,
                                                                           Integer pageSize,
                                                                           String dimension,
                                                                           String startDate,
                                                                           String endDate,
                                                                           String warehouse,
                                                                           String dishName,
                                                                           String itemCode,
                                                                           String deductionType,
                                                                           String unitType,
                                                                           String queryScheme,
                                                                           String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String warehouseKeyword = trimNullable(warehouse);
        String dishKeyword = toLower(trimNullable(dishName));
        String itemCodeKeyword = toLower(trimNullable(itemCode));
        String deductionTypeValue = trimNullable(deductionType);
        String unitTypeValue = trimNullable(unitType);

        List<DishConsumptionOutboundReportRow> rows = new ArrayList<>();
        rows.addAll(loadDishRows(scope, InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND, "销售扣减"));
        rows.addAll(loadDishRows(scope, InventoryDocumentType.DAMAGE_OUTBOUND, "报损扣减"));
        rows = rows.stream()
                .filter(row -> matchDate(row.businessDate(), start, end))
                .filter(row -> !StringUtils.hasText(warehouseKeyword) || toLower(row.warehouse()).contains(warehouseKeyword))
                .filter(row -> !StringUtils.hasText(dishKeyword)
                        || toLower(row.dishName()).contains(dishKeyword)
                        || toLower(row.dishSpuCode()).contains(dishKeyword))
                .filter(row -> !StringUtils.hasText(itemCodeKeyword) || toLower(row.itemCode()).contains(itemCodeKeyword))
                .filter(row -> !StringUtils.hasText(deductionTypeValue) || Objects.equals(row.deductionType(), deductionTypeValue))
                .filter(row -> !StringUtils.hasText(unitTypeValue) || Objects.equals(row.itemUnit(), unitTypeValue))
                .sorted(Comparator.comparing(DishConsumptionOutboundReportRow::businessDate, Comparator.nullsLast(String::compareTo))
                        .reversed()
                        .thenComparing(DishConsumptionOutboundReportRow::consumptionNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(DishConsumptionOutboundReportRow::itemCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<DishConsumptionOutboundReportRow> normalizedRows = "菜品消耗汇总".equals(trimNullable(dimension))
                ? aggregateDishRows(rows)
                : rows;

        int fromIndex = Math.min((safePageNo - 1) * safePageSize, normalizedRows.size());
        int toIndex = Math.min(fromIndex + safePageSize, normalizedRows.size());
        List<DishConsumptionOutboundReportRow> pageRows = normalizedRows.subList(fromIndex, toIndex);
        DishConsumptionOutboundReportSummary summary = summarizeDishRows(rows);
        return new DishConsumptionOutboundReportPage(pageRows, normalizedRows.size(), safePageNo, safePageSize, summary);
    }

    /** 查询库存出入库明细报表。 */
    public PageData<InventoryInoutDetailReportRow> inventoryInoutDetailReport(Integer pageNo,
                                                                              Integer pageSize,
                                                                              String statisticDimension,
                                                                              String warehouse,
                                                                              String warehouseType,
                                                                              String startDate,
                                                                              String endDate,
                                                                              String dateText,
                                                                              String auditStartTime,
                                                                              String auditEndTime,
                                                                              String inoutType,
                                                                              String upstreamDocumentType,
                                                                              String itemCategory,
                                                                              String statisticType,
                                                                              String itemCode,
                                                                              String reasonType,
                                                                              String adjustmentDocument,
                                                                              String oppositeOrg,
                                                                              String documentNo,
                                                                              String crossMonthDocument,
                                                                              String inoutDirection,
                                                                              String gift,
                                                                              String unitType,
                                                                              String queryScheme,
                                                                              String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        LocalDateTime auditStart = parseDateTimeNullable(auditStartTime, "审核开始时间格式不正确");
        LocalDateTime auditEnd = parseDateTimeNullable(auditEndTime, "审核结束时间格式不正确");
        String warehouseKeyword = trimNullable(warehouse);
        String warehouseTypeValue = trimNullable(warehouseType);
        String inoutTypeValue = trimNullable(inoutType);
        String upstreamDocumentTypeValue = trimNullable(upstreamDocumentType);
        String itemCategoryValue = trimNullable(itemCategory);
        String statisticTypeValue = trimNullable(statisticType);
        String itemCodeValue = trimNullable(itemCode);
        String reasonTypeValue = trimNullable(reasonType);
        String adjustmentDocumentValue = trimNullable(adjustmentDocument);
        String oppositeOrgValue = trimNullable(oppositeOrg);
        String documentNoValue = trimNullable(documentNo);
        String crossMonthDocumentValue = trimNullable(crossMonthDocument);
        String inoutDirectionValue = trimNullable(inoutDirection);
        String giftValue = trimNullable(gift);
        String unitTypeValue = trimNullable(unitType);
        InventoryInoutDetailFilter filter = new InventoryInoutDetailFilter(start, end, auditStart, auditEnd,
                warehouseKeyword, warehouseTypeValue, inoutTypeValue, upstreamDocumentTypeValue, itemCategoryValue,
                statisticTypeValue, itemCodeValue, reasonTypeValue, adjustmentDocumentValue, oppositeOrgValue,
                documentNoValue, crossMonthDocumentValue, inoutDirectionValue, giftValue, unitTypeValue);

        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        rows.addAll(loadPurchaseInboundRows(scope));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.PURCHASE_RETURN_OUTBOUND, "采购退货出库", "出库", "采购入库", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.DEPARTMENT_PICKING, "部门领料", "出库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.DEPARTMENT_RETURN, "部门退料", "入库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STOCK_TRANSFER, "移库出库", "出库", "调拨单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STOCK_TRANSFER_INBOUND, "移库入库", "入库", "调拨单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.DEPARTMENT_TRANSFER, "部门调拨", "出库", "调拨单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.DAMAGE_OUTBOUND, "报损出库", "出库", "", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.OTHER_INBOUND, "其他入库", "入库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.OTHER_OUTBOUND, "其他出库", "出库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.PROFIT_INBOUND, "盘盈入库", "入库", "盘点单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.LOSS_OUTBOUND, "盘亏出库", "出库", "盘点单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.PRODUCTION_INBOUND, "生产入库", "入库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.CUSTOMER_SALES_OUTBOUND, "客户销售出库", "出库", "销售订单", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND, "菜品消耗出库", "出库", "销售订单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.CUSTOMER_RETURN_INBOUND, "客户退货入库", "入库", "销售订单", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STORE_TRANSFER, "调拨单", "出库", "调拨单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STOCK_TRANSFER_OUTBOUND, "移库出库", "出库", "调拨单", "是"));

        List<InventoryInoutDetailReportRow> filtered = rows.stream()
                .filter(row -> matchInventoryInoutDetailRow(row, filter))
                .sorted(Comparator.comparing(InventoryInoutDetailReportRow::documentDate, Comparator.nullsLast(String::compareTo))
                        .reversed()
                        .thenComparing(InventoryInoutDetailReportRow::documentNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(InventoryInoutDetailReportRow::itemCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<InventoryInoutDetailReportRow> normalizedRows = "单据".equals(trimNullable(statisticDimension))
                ? aggregateInoutRows(filtered)
                : filtered;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, normalizedRows.size());
        int toIndex = Math.min(fromIndex + safePageSize, normalizedRows.size());
        return new PageData<>(normalizedRows.subList(fromIndex, toIndex), normalizedRows.size(), safePageNo, safePageSize);
    }

    private boolean matchInventoryInoutDetailRow(InventoryInoutDetailReportRow row, InventoryInoutDetailFilter filter) {
        return matchDate(row.documentDate(), filter.start(), filter.end())
                && matchAuditTime(row.documentAuditTime(), filter.auditStart(), filter.auditEnd())
                && matchInventoryInoutWarehouse(row, filter.warehouse())
                && matchInventoryInoutTypeFields(row, filter)
                && matchInventoryInoutDocumentFields(row, filter)
                && matchInventoryInoutDirectionFields(row, filter);
    }

    private boolean matchInventoryInoutWarehouse(InventoryInoutDetailReportRow row, String warehouse) {
        return !StringUtils.hasText(warehouse) || toLower(row.warehouse()).contains(warehouse);
    }

    private boolean matchInventoryInoutTypeFields(InventoryInoutDetailReportRow row, InventoryInoutDetailFilter filter) {
        return (!StringUtils.hasText(filter.warehouseType()) || Objects.equals(row.warehouseType(), filter.warehouseType()))
                && (!StringUtils.hasText(filter.inoutType()) || Objects.equals(row.inoutType(), filter.inoutType()))
                && (!StringUtils.hasText(filter.upstreamDocumentType())
                || Objects.equals(row.upstreamDocumentType(), filter.upstreamDocumentType()))
                && (!StringUtils.hasText(filter.itemCategory()) || Objects.equals(row.itemCategory(), filter.itemCategory()))
                && (!StringUtils.hasText(filter.statisticType()) || Objects.equals(row.statisticType(), filter.statisticType()));
    }

    private boolean matchInventoryInoutDocumentFields(InventoryInoutDetailReportRow row,
                                                      InventoryInoutDetailFilter filter) {
        return (!StringUtils.hasText(filter.itemCode()) || toLower(row.itemCode()).contains(toLower(filter.itemCode())))
                && (!StringUtils.hasText(filter.reasonType()) || Objects.equals(row.reasonType(), filter.reasonType()))
                && (!StringUtils.hasText(filter.adjustmentDocument())
                || Objects.equals(row.adjustmentDocument(), filter.adjustmentDocument()))
                && (!StringUtils.hasText(filter.oppositeOrg())
                || toLower(row.oppositeOrg()).contains(toLower(filter.oppositeOrg())))
                && (!StringUtils.hasText(filter.documentNo())
                || toLower(row.documentNo()).contains(toLower(filter.documentNo())));
    }

    private boolean matchInventoryInoutDirectionFields(InventoryInoutDetailReportRow row,
                                                       InventoryInoutDetailFilter filter) {
        return (!StringUtils.hasText(filter.crossMonthDocument())
                || Objects.equals(row.crossMonthDocument(), filter.crossMonthDocument()))
                && (!StringUtils.hasText(filter.inoutDirection())
                || Objects.equals(row.inoutDirection(), filter.inoutDirection()))
                && (!StringUtils.hasText(filter.gift()) || Objects.equals(row.gift(), filter.gift()))
                && (!StringUtils.hasText(filter.unitType()) || Objects.equals(row.unit(), filter.unitType()));
    }

    /**
     * 查询物品批次全流程跟踪表。
     *
     * @param startDate 业务开始日期
     * @param endDate 业务结束日期
     * @param warehouses 仓库名称列表
     * @param itemCode 物品编码
     * @param batchNo 批次号
     * @param sourceOrg 来源机构
     * @param unitType 单位类型
     * @param orgId 当前机构作用域
     * @return 批次来源、本机构流转、外部流向数据
     */
    public ItemBatchTraceReport itemBatchTraceReport(String startDate,
                                                     String endDate,
                                                     List<String> warehouses,
                                                     String itemCode,
                                                     String batchNo,
                                                     String sourceOrg,
                                                     String unitType,
                                                     String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String itemCodeValue = requireText(itemCode, "物品不能为空");
        String batchNoValue = requireText(batchNo, "批次号不能为空");
        List<String> warehouseValues = normalizeValues(warehouses);
        String sourceOrgValue = trimNullable(sourceOrg);
        Map<String, BigDecimal> balanceMap = currentBalanceMap(scope);
        List<BatchTraceDocumentRow> rows = loadBatchTraceRows(scope, itemCodeValue, batchNoValue, balanceMap).stream()
                .filter(row -> matchDate(row.documentDate(), start, end))
                .filter(row -> warehouseValues.isEmpty() || warehouseValues.contains(row.warehouse()))
                .filter(row -> !StringUtils.hasText(sourceOrgValue) || "全部".equals(sourceOrgValue) || Objects.equals(row.sourceOrg(), sourceOrgValue))
                .toList();
        return new ItemBatchTraceReport(
                rows.stream().filter(BatchTraceDocumentRow::inbound).map(this::toBatchSourceRow).toList(),
                rows.stream().map(this::toBatchInternalFlowRow).toList(),
                rows.stream().filter(row -> !row.inbound()).map(this::toBatchExternalFlowRow).toList()
        );
    }

    private List<BatchTraceDocumentRow> loadBatchTraceRows(InventoryScope scope,
                                                           String itemCode,
                                                           String batchNo,
                                                           Map<String, BigDecimal> balanceMap) {
        List<BatchTraceDocumentRow> rows = new ArrayList<>();
        for (InventoryDocumentType type : InventoryDocumentType.managedTypes()) {
            rows.addAll(loadBatchTraceRowsByType(scope, type, itemCode, batchNo, balanceMap));
        }
        return rows.stream()
                .sorted(Comparator.comparing(BatchTraceDocumentRow::documentDate, Comparator.nullsLast(String::compareTo))
                        .thenComparing(BatchTraceDocumentRow::documentCreatedAt, Comparator.nullsLast(String::compareTo))
                        .thenComparing(BatchTraceDocumentRow::documentNo, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<BatchTraceDocumentRow> loadBatchTraceRowsByType(InventoryScope scope,
                                                                 InventoryDocumentType type,
                                                                 String itemCode,
                                                                 String batchNo,
                                                                 Map<String, BigDecimal> balanceMap) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, InventoryDocumentHeader> headerMap = headers.stream().collect(Collectors.toMap(InventoryDocumentHeader::getId, header -> header));
        List<InventoryDocumentLine> lines = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList());
        return lines.stream()
                .filter(line -> isApprovedDocument(headerMap.get(line.getHeaderId())))
                .filter(line -> Objects.equals(line.getItemCode(), itemCode))
                .filter(line -> Objects.equals(batchNoFromLine(line), batchNo))
                .map(line -> toBatchTraceDocumentRow(scope, type, headerMap.get(line.getHeaderId()), line, balanceMap))
                .filter(Objects::nonNull)
                .toList();
    }

    private BatchTraceDocumentRow toBatchTraceDocumentRow(InventoryScope scope,
                                                          InventoryDocumentType type,
                                                          InventoryDocumentHeader header,
                                                          InventoryDocumentLine line,
                                                          Map<String, BigDecimal> balanceMap) {
        if (header == null) {
            return null;
        }
        Map<String, String> lineExtra = parseExtraJson(line.getExtraJson());
        String warehouse = resolveWarehouseName(type, header);
        String batchNo = defaultIfBlank(lineExtra.get("batchNo"), "");
        boolean inbound = type.getStockDirection() == InventoryDocumentType.StockDirection.INBOUND;
        BigDecimal quantity = defaultQuantity(line.getQuantity());
        BigDecimal flowQty = inbound ? quantity : quantity.negate();
        return new BatchTraceDocumentRow(
                buildRowId("batch-trace", header.getId(), line.getId()),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultOrgName(scope),
                warehouse,
                resolveTargetOrg(type, header),
                type.getBusinessName(),
                header.getDocumentCode(),
                defaultIfBlank(header.getUpstreamCode(), ""),
                formatDate(header.getDocumentDate()),
                formatDateTime(header.getCreatedAt()),
                batchNo,
                defaultIfBlank(lineExtra.get("manufacturer"), ""),
                line.getItemCode(),
                line.getItemName(),
                defaultIfBlank(line.getSpec(), ""),
                defaultIfBlank(line.getUnitName(), ""),
                flowQty,
                balanceMap.getOrDefault(transactionKey(warehouse, line.getItemCode()), BigDecimal.ZERO),
                inbound
        );
    }

    private ItemBatchTraceSourceRow toBatchSourceRow(BatchTraceDocumentRow row) {
        return new ItemBatchTraceSourceRow(row.id(), row.sourceOrg(), row.orgName(), row.warehouse(), row.documentType(),
                row.documentNo(), row.upstreamDocumentNo(), row.documentDate(), row.documentCreatedAt(), row.batchNo(),
                row.manufacturer(), row.itemCode(), row.itemName(), row.specModel(), row.unit(), row.inoutQty());
    }

    private ItemBatchTraceInternalFlowRow toBatchInternalFlowRow(BatchTraceDocumentRow row) {
        return new ItemBatchTraceInternalFlowRow(row.id(), row.orgName(), row.warehouse(), row.documentType(), row.documentNo(),
                row.upstreamDocumentNo(), row.documentDate(), row.documentCreatedAt(), row.batchNo(), row.itemCode(),
                row.itemName(), row.specModel(), row.unit(), row.inoutQty(), row.currentBalanceQty());
    }

    private ItemBatchTraceExternalFlowRow toBatchExternalFlowRow(BatchTraceDocumentRow row) {
        return new ItemBatchTraceExternalFlowRow(row.id(), row.orgName(), row.warehouse(), row.targetOrg(), row.documentType(),
                row.documentNo(), row.upstreamDocumentNo(), row.documentDate(), row.documentCreatedAt(), row.batchNo(),
                row.itemCode(), row.itemName(), row.specModel(), row.unit(), row.inoutQty(), row.currentBalanceQty());
    }

    private List<DishConsumptionOutboundReportRow> loadDishRows(InventoryScope scope,
                                                                 InventoryDocumentType type,
                                                                 String deductionType) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        List<DishConsumptionOutboundReportRow> rows = new ArrayList<>();
        for (InventoryDocumentHeader header : headers) {
            if (!isApprovedDocument(header)) {
                continue;
            }
            List<InventoryDocumentLine> lines = lineMap.getOrDefault(header.getId(), List.of());
            for (InventoryDocumentLine line : lines) {
                BigDecimal quantity = defaultQuantity(line.getQuantity());
                BigDecimal amount = line.getAmount() == null ? quantity.multiply(defaultQuantity(line.getUnitPrice())).setScale(2, RoundingMode.HALF_UP) : line.getAmount();
                String warehouseName = resolveWarehouseName(type, header);
                rows.add(new DishConsumptionOutboundReportRow(
                        line.getId(),
                        defaultIfBlank(header.getDocumentCode(), ""),
                        header.getDocumentDate() == null ? "" : header.getDocumentDate().toString(),
                        defaultIfBlank(line.getItemCode(), ""),
                        defaultIfBlank(line.getItemCode(), ""),
                        defaultIfBlank(line.getItemName(), ""),
                        defaultIfBlank(line.getSpec(), ""),
                        deductionType,
                        defaultIfBlank(header.getReason(), defaultIfBlank(header.getCounterpartyName(), deductionType)),
                        defaultIfBlank(line.getCategory(), ""),
                        deductionType,
                        defaultIfBlank(line.getItemCode(), ""),
                        defaultIfBlank(line.getItemName(), ""),
                        defaultIfBlank(line.getSpec(), ""),
                        warehouseName,
                        "库存单位",
                        quantity,
                        quantity,
                        quantity,
                        BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                ));
            }
        }
        return rows;
    }

    private List<DishConsumptionOutboundReportRow> aggregateDishRows(List<DishConsumptionOutboundReportRow> rows) {
        Map<String, MutableDishRow> grouped = new LinkedHashMap<>();
        for (DishConsumptionOutboundReportRow row : rows) {
            String key = String.join("|",
                    defaultIfBlank(row.warehouse(), ""),
                    defaultIfBlank(row.dishSpuCode(), ""),
                    defaultIfBlank(row.itemCode(), ""),
                    defaultIfBlank(row.deductionType(), ""));
            grouped.computeIfAbsent(key, ignored -> new MutableDishRow()).merge(row);
        }
        return grouped.values().stream().map(MutableDishRow::toRow).toList();
    }

    private DishConsumptionOutboundReportSummary summarizeDishRows(List<DishConsumptionOutboundReportRow> rows) {
        return new DishConsumptionOutboundReportSummary(
                sum(rows.stream().map(DishConsumptionOutboundReportRow::dishQty).toList()),
                sum(rows.stream().map(DishConsumptionOutboundReportRow::theoreticalQty).toList()),
                sum(rows.stream().map(DishConsumptionOutboundReportRow::outboundQty).toList()),
                sum(rows.stream().map(DishConsumptionOutboundReportRow::pendingOutboundQty).toList()),
                sum(rows.stream().map(DishConsumptionOutboundReportRow::unlinkedWarehousePendingQty).toList()),
                sum(rows.stream().map(DishConsumptionOutboundReportRow::otherReasonPendingQty).toList())
        );
    }

    private List<InventoryInoutDetailReportRow> loadPurchaseInboundRows(InventoryScope scope) {
        List<PurchaseInboundDO> headers = loadVisiblePurchaseInboundHeaders(scope);
        List<PurchaseInboundLineDO> lines = purchaseInboundLineRepository.findByInboundIds(headers.stream().map(PurchaseInboundDO::getId).toList());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = lines.stream().collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));
        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        for (PurchaseInboundDO header : headers) {
            if (!isApprovedPurchaseInbound(header)) {
                continue;
            }
            for (PurchaseInboundLineDO line : lineMap.getOrDefault(header.getId(), List.of())) {
                BigDecimal quantity = defaultQuantity(line.getQuantity());
                BigDecimal amount = quantity.multiply(defaultQuantity(line.getUnitPrice())).setScale(2, RoundingMode.HALF_UP);
                rows.add(new InventoryInoutDetailReportRow(
                        buildRowId("purchase-inbound", header.getId(), line.getId()),
                        defaultIfBlank(line.getItemCode(), ""),
                        defaultIfBlank(line.getItemName(), ""),
                        defaultIfBlank(line.getSpec(), ""),
                        defaultIfBlank(line.getCategory(), ""),
                        defaultIfBlank(line.getCategory(), ""),
                        "库存单位",
                        "库存单位",
                        defaultOrgName(scope),
                        defaultOrgCode(scope),
                        defaultIfBlank(header.getWarehouseName(), ""),
                        resolveWarehouseType(scope, header.getWarehouseName()),
                        defaultIfBlank(header.getUpstreamCode(), ""),
                        "采购订单",
                        defaultIfBlank(header.getDocumentCode(), ""),
                        "采购入库",
                        defaultIfBlank(header.getRemark(), ""),
                        "否",
                        defaultIfBlank(header.getSupplierName(), ""),
                        "",
                        header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                        header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                        formatDateTime(header.getCreatedAt()),
                        defaultIfBlank(header.getCreatedBy() == null ? null : String.valueOf(header.getCreatedBy()), ""),
                        formatDateTime(header.getApprovedAt()),
                        quantity,
                        quantity,
                        BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP),
                        amount,
                        amount,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        crossMonthDocument(header.getInboundDate(), header.getCreatedAt()),
                        "入库",
                        detectGift(header.getRemark(), ""),
                        defaultIfBlank(header.getRemark(), "")
                ));
            }
        }
        return rows;
    }

    private List<InventoryInoutDetailReportRow> loadGenericDocumentRows(InventoryScope scope,
                                                                        InventoryDocumentType type,
                                                                        String inoutTypeLabel,
                                                                        String inoutDirection,
                                                                        String upstreamDocumentType,
                                                                        String adjustmentDocument) {
        List<InventoryDocumentHeader> headers = loadVisibleDocumentHeaders(type, scope);
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        for (InventoryDocumentHeader header : headers) {
            if (!isApprovedDocument(header)) {
                continue;
            }
            for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                rows.add(buildGenericDocumentRow(scope, type, header, line, inoutTypeLabel, inoutDirection,
                        upstreamDocumentType, adjustmentDocument));
            }
        }
        return rows;
    }

    private InventoryInoutDetailReportRow buildGenericDocumentRow(InventoryScope scope,
                                                                  InventoryDocumentType type,
                                                                  InventoryDocumentHeader header,
                                                                  InventoryDocumentLine line,
                                                                  String inoutTypeLabel,
                                                                  String inoutDirection,
                                                                  String upstreamDocumentType,
                                                                  String adjustmentDocument) {
        BigDecimal quantity = defaultQuantity(line.getQuantity());
        BigDecimal amount = line.getAmount() == null
                ? quantity.multiply(defaultQuantity(line.getUnitPrice())).setScale(2, RoundingMode.HALF_UP)
                : line.getAmount();
        String warehouseName = resolveWarehouseName(type, header);
        return new InventoryInoutDetailReportRow(
                buildRowId(type.getPathSegment(), header.getId(), line.getId()), defaultIfBlank(line.getItemCode(), ""),
                defaultIfBlank(line.getItemName(), ""), defaultIfBlank(line.getSpec(), ""),
                defaultIfBlank(line.getCategory(), ""), defaultIfBlank(line.getCategory(), ""),
                defaultIfBlank(line.getUnitName(), "库存单位"), defaultIfBlank(line.getUnitName(), "库存单位"),
                defaultOrgName(scope), defaultOrgCode(scope), warehouseName, resolveWarehouseType(scope, warehouseName),
                defaultIfBlank(header.getUpstreamCode(), ""), upstreamDocumentType,
                defaultIfBlank(header.getDocumentCode(), ""), inoutTypeLabel,
                defaultIfBlank(header.getReason(), defaultIfBlank(line.getLineReason(), "")), adjustmentDocument,
                defaultIfBlank(header.getCounterpartyName(), defaultIfBlank(header.getSecondaryName(), "")), "",
                formatDate(header.getDocumentDate()), formatDate(header.getDocumentDate()), formatDateTime(header.getCreatedAt()),
                defaultIfBlank(header.getCreatedBy() == null ? null : String.valueOf(header.getCreatedBy()), ""),
                formatDateTime(header.getApprovedAt()), quantityByDirection(inoutDirection, "入库", quantity),
                quantityByDirection(inoutDirection, "入库", quantity), quantityByDirection(inoutDirection, "出库", quantity),
                quantityByDirection(inoutDirection, "出库", quantity), moneyByDirection(inoutDirection, "入库", amount),
                moneyByDirection(inoutDirection, "入库", amount), moneyByDirection(inoutDirection, "出库", amount),
                moneyByDirection(inoutDirection, "出库", amount), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                crossMonthDocument(header.getDocumentDate(), header.getCreatedAt()), inoutDirection,
                detectGift(header.getRemark(), line.getLineReason()), defaultIfBlank(header.getRemark(), ""));
    }

    private BigDecimal quantityByDirection(String direction, String expectedDirection, BigDecimal quantity) {
        return expectedDirection.equals(direction) ? quantity
                : BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyByDirection(String direction, String expectedDirection, BigDecimal amount) {
        return expectedDirection.equals(direction) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private List<InventoryInoutDetailReportRow> aggregateInoutRows(List<InventoryInoutDetailReportRow> rows) {
        Map<String, MutableInoutRow> grouped = new LinkedHashMap<>();
        for (InventoryInoutDetailReportRow row : rows) {
            String key = String.join("|",
                    defaultIfBlank(row.documentNo(), ""),
                    defaultIfBlank(row.inoutType(), ""),
                    defaultIfBlank(row.warehouse(), ""),
                    defaultIfBlank(row.itemCode(), ""));
            grouped.computeIfAbsent(key, ignored -> new MutableInoutRow()).merge(row);
        }
        return grouped.values().stream().map(MutableInoutRow::toRow).toList();
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private List<PurchaseInboundDO> loadVisiblePurchaseInboundHeaders(InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        return purchaseInboundRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> viewAll || belongsToOperator(header.getCreatedBy(), header.getSalesmanUserId(), operatorId))
                .toList();
    }

    private List<InventoryDocumentHeader> loadVisibleDocumentHeaders(InventoryDocumentType type, InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        return inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> viewAll || belongsToOperator(header.getCreatedBy(), header.getSalesmanUserId(), operatorId))
                .toList();
    }

    private boolean belongsToOperator(Long createdBy, Long salesmanUserId, Long operatorId) {
        return Objects.equals(createdBy, operatorId) || Objects.equals(salesmanUserId, operatorId);
    }

    private boolean matchDate(String value, LocalDate start, LocalDate end) {
        LocalDate date = parseDateNullable(value, "业务日期格式不正确");
        if (date == null) {
            return start == null && end == null;
        }
        if (start != null && date.isBefore(start)) {
            return false;
        }
        return end == null || !date.isAfter(end);
    }

    private boolean matchAuditTime(String value, LocalDateTime start, LocalDateTime end) {
        LocalDateTime dateTime = parseDateTimeNullable(value, "审核时间格式不正确");
        if (dateTime == null) {
            return start == null && end == null;
        }
        if (start != null && dateTime.isBefore(start)) {
            return false;
        }
        return end == null || !dateTime.isAfter(end);
    }

    private String resolveWarehouseName(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.PURCHASE_INBOUND) {
            return defaultIfBlank(header.getPrimaryName(), "");
        }
        if (type == InventoryDocumentType.STORE_TRANSFER) {
            return defaultIfBlank(parseExtraJson(header.getExtraJson()).get(STORE_TRANSFER_SOURCE_WAREHOUSE), "");
        }
        if (type == InventoryDocumentType.CUSTOMER_SALES_OUTBOUND || type == InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND) {
            return defaultIfBlank(header.getPrimaryName(), defaultIfBlank(header.getCounterpartyName(), ""));
        }
        return defaultIfBlank(header.getPrimaryName(), defaultIfBlank(header.getSecondaryName(), defaultIfBlank(header.getCounterpartyName(), "")));
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

    private String defaultOrgName(InventoryScope scope) {
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP)) {
            GroupDO group = groupRepository.findById(scope.scopeId()).orElse(null);
            return group == null ? "" : defaultIfBlank(group.getGroupName(), "");
        }
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE)) {
            StoreDO store = storeRepository.findById(scope.scopeId()).orElse(null);
            return store == null ? "" : defaultIfBlank(store.getStoreName(), "");
        }
        return "平台";
    }

    private String defaultOrgCode(InventoryScope scope) {
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_GROUP)) {
            GroupDO group = groupRepository.findById(scope.scopeId()).orElse(null);
            return group == null ? "" : defaultIfBlank(group.getGroupCode(), "");
        }
        if (Objects.equals(scope.scopeType(), OrgScopeService.SCOPE_STORE)) {
            StoreDO store = storeRepository.findById(scope.scopeId()).orElse(null);
            return store == null ? "" : defaultIfBlank(store.getStoreCode(), "");
        }
        return "platform";
    }

    private String buildRowId(String prefix, Long headerId, Long lineId) {
        return prefix + "-" + headerId + "-" + lineId;
    }

    private Map<String, BigDecimal> currentBalanceMap(InventoryScope scope) {
        return inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .collect(Collectors.toMap(
                        balance -> transactionKey(balance.getWarehouseName(), balance.getItemCode()),
                        InventoryBalanceDO::getQuantity,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private String batchNoFromLine(InventoryDocumentLine line) {
        return defaultIfBlank(parseExtraJson(line.getExtraJson()).get("batchNo"), "");
    }

    private Map<String, String> parseExtraJson(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(extraJson, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            throw new BusinessException("报表扩展字段格式错误，请清理脏数据");
        }
    }

    private String resolveTargetOrg(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.STORE_TRANSFER || type == InventoryDocumentType.STOCK_TRANSFER_OUTBOUND) {
            return defaultIfBlank(header.getCounterpartyName(), defaultIfBlank(header.getCounterpartyName2(), ""));
        }
        return defaultIfBlank(header.getCounterpartyName(), defaultIfBlank(header.getSecondaryName(), ""));
    }

    private String transactionKey(String warehouseName, String itemCode) {
        return defaultIfBlank(warehouseName, "") + "|" + defaultIfBlank(itemCode, "");
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private boolean hasAnyText(String value) {
        return StringUtils.hasText(value);
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

    private LocalDate parseDateNullable(String value, String message) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(message);
        }
    }

    private LocalDateTime parseDateTimeNullable(String value, String message) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(message);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String requireText(String value, String message) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::trimNullable)
                .filter(Objects::nonNull)
                .flatMap(value -> List.of(value.split(",")).stream())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(value -> !"全部".equals(value))
                .toList();
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
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

    private String crossMonthDocument(LocalDate documentDate, LocalDateTime createdAt) {
        if (documentDate == null || createdAt == null) {
            return "否";
        }
        return documentDate.getYear() != createdAt.getYear() || documentDate.getMonthValue() != createdAt.getMonthValue() ? "是" : "否";
    }

    private String detectGift(String remark, String lineReason) {
        String text = (defaultIfBlank(remark, "") + defaultIfBlank(lineReason, "")).toLowerCase(Locale.ROOT);
        return text.contains("赠品") ? "是" : "否";
    }

    private boolean isApprovedDocument(InventoryDocumentHeader header) {
        return Objects.equals(defaultIfBlank(header.getStatus(), ""), "已审核");
    }

    private boolean isApprovedPurchaseInbound(PurchaseInboundDO header) {
        return header != null && Objects.equals(defaultIfBlank(header.getStatus(), ""), "已审核");
    }

    private static final class MutableDishRow {
        private String id = "";
        private String consumptionNo = "";
        private String businessDate = "";
        private String dishSpuCode = "";
        private String dishSkuCode = "";
        private String dishName = "";
        private String dishSpec = "";
        private String costCard = "";
        private String orderSource = "";
        private String dishCategory = "";
        private String deductionType = "";
        private String itemCode = "";
        private String itemName = "";
        private String specModel = "";
        private String warehouse = "";
        private String itemUnit = "";
        private BigDecimal dishQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal theoreticalQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal outboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal pendingOutboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal unlinkedWarehousePendingQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal otherReasonPendingQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        private MutableDishRow merge(DishConsumptionOutboundReportRow row) {
            if (!StringUtils.hasText(id)) {
                id = String.valueOf(row.id() == null ? 0 : row.id());
            }
            consumptionNo = row.consumptionNo();
            businessDate = row.businessDate();
            dishSpuCode = row.dishSpuCode();
            dishSkuCode = row.dishSkuCode();
            dishName = row.dishName();
            dishSpec = row.dishSpec();
            costCard = row.costCard();
            orderSource = row.orderSource();
            dishCategory = row.dishCategory();
            deductionType = row.deductionType();
            itemCode = row.itemCode();
            itemName = row.itemName();
            specModel = row.specModel();
            warehouse = row.warehouse();
            itemUnit = row.itemUnit();
            dishQty = dishQty.add(defaultQuantity(row.dishQty()));
            theoreticalQty = theoreticalQty.add(defaultQuantity(row.theoreticalQty()));
            outboundQty = outboundQty.add(defaultQuantity(row.outboundQty()));
            pendingOutboundQty = pendingOutboundQty.add(defaultQuantity(row.pendingOutboundQty()));
            unlinkedWarehousePendingQty = unlinkedWarehousePendingQty.add(defaultQuantity(row.unlinkedWarehousePendingQty()));
            otherReasonPendingQty = otherReasonPendingQty.add(defaultQuantity(row.otherReasonPendingQty()));
            return this;
        }

        private DishConsumptionOutboundReportRow toRow() {
            return new DishConsumptionOutboundReportRow(
                    id,
                    consumptionNo,
                    businessDate,
                    dishSpuCode,
                    dishSkuCode,
                    dishName,
                    dishSpec,
                    costCard,
                    orderSource,
                    dishCategory,
                    deductionType,
                    itemCode,
                    itemName,
                    specModel,
                    warehouse,
                    itemUnit,
                    dishQty,
                    theoreticalQty,
                    outboundQty,
                    pendingOutboundQty,
                    unlinkedWarehousePendingQty,
                    otherReasonPendingQty
            );
        }
    }

    private static final class MutableInoutRow {
        private String id = "";
        private String itemCode = "";
        private String itemName = "";
        private String specModel = "";
        private String itemCategory = "";
        private String statisticType = "";
        private String baseUnit = "";
        private String unit = "";
        private String orgName = "";
        private String orgCode = "";
        private String warehouse = "";
        private String warehouseType = "";
        private String upstreamDocumentNo = "";
        private String upstreamDocumentType = "";
        private String documentNo = "";
        private String inoutType = "";
        private String reasonType = "";
        private String adjustmentDocument = "";
        private String oppositeOrg = "";
        private String oppositeOrgCode = "";
        private String upstreamDocumentDate = "";
        private String documentDate = "";
        private String documentCreatedAt = "";
        private String documentCreator = "";
        private String documentAuditTime = "";
        private BigDecimal inboundBaseQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal inboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal outboundBaseQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal outboundQty = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal inboundCostAmountTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal inboundSettlementAmountTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal outboundCostAmountTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal outboundSettlementAmountTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal outboundDiscountSettlementAmountTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal outboundDiscountGrossProfitTaxIncluded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private String crossMonthDocument = "否";
        private String inoutDirection = "";
        private String gift = "否";
        private String remark = "";

        private MutableInoutRow merge(InventoryInoutDetailReportRow row) {
            if (!StringUtils.hasText(id)) {
                id = String.valueOf(row.id() == null ? "" : row.id());
            }
            itemCode = row.itemCode();
            itemName = row.itemName();
            specModel = row.specModel();
            itemCategory = row.itemCategory();
            statisticType = row.statisticType();
            baseUnit = row.baseUnit();
            unit = row.unit();
            orgName = row.orgName();
            orgCode = row.orgCode();
            warehouse = row.warehouse();
            warehouseType = row.warehouseType();
            upstreamDocumentNo = row.upstreamDocumentNo();
            upstreamDocumentType = row.upstreamDocumentType();
            documentNo = row.documentNo();
            inoutType = row.inoutType();
            reasonType = row.reasonType();
            adjustmentDocument = row.adjustmentDocument();
            oppositeOrg = row.oppositeOrg();
            oppositeOrgCode = row.oppositeOrgCode();
            upstreamDocumentDate = row.upstreamDocumentDate();
            documentDate = row.documentDate();
            documentCreatedAt = row.documentCreatedAt();
            documentCreator = row.documentCreator();
            documentAuditTime = row.documentAuditTime();
            inboundBaseQty = inboundBaseQty.add(defaultQuantity(row.inboundBaseQty()));
            inboundQty = inboundQty.add(defaultQuantity(row.inboundQty()));
            outboundBaseQty = outboundBaseQty.add(defaultQuantity(row.outboundBaseQty()));
            outboundQty = outboundQty.add(defaultQuantity(row.outboundQty()));
            inboundCostAmountTaxIncluded = inboundCostAmountTaxIncluded.add(defaultMoney(row.inboundCostAmountTaxIncluded()));
            inboundSettlementAmountTaxIncluded = inboundSettlementAmountTaxIncluded.add(defaultMoney(row.inboundSettlementAmountTaxIncluded()));
            outboundCostAmountTaxIncluded = outboundCostAmountTaxIncluded.add(defaultMoney(row.outboundCostAmountTaxIncluded()));
            outboundSettlementAmountTaxIncluded = outboundSettlementAmountTaxIncluded.add(defaultMoney(row.outboundSettlementAmountTaxIncluded()));
            outboundDiscountSettlementAmountTaxIncluded = outboundDiscountSettlementAmountTaxIncluded.add(defaultMoney(row.outboundDiscountSettlementAmountTaxIncluded()));
            outboundDiscountGrossProfitTaxIncluded = outboundDiscountGrossProfitTaxIncluded.add(defaultMoney(row.outboundDiscountGrossProfitTaxIncluded()));
            crossMonthDocument = row.crossMonthDocument();
            inoutDirection = row.inoutDirection();
            gift = row.gift();
            remark = row.remark();
            return this;
        }

        private InventoryInoutDetailReportRow toRow() {
            return new InventoryInoutDetailReportRow(
                    id,
                    itemCode,
                    itemName,
                    specModel,
                    itemCategory,
                    statisticType,
                    baseUnit,
                    unit,
                    orgName,
                    orgCode,
                    warehouse,
                    warehouseType,
                    upstreamDocumentNo,
                    upstreamDocumentType,
                    documentNo,
                    inoutType,
                    reasonType,
                    adjustmentDocument,
                    oppositeOrg,
                    oppositeOrgCode,
                    upstreamDocumentDate,
                    documentDate,
                    documentCreatedAt,
                    documentCreator,
                    documentAuditTime,
                    inboundBaseQty,
                    inboundQty,
                    outboundBaseQty,
                    outboundQty,
                    inboundCostAmountTaxIncluded,
                    inboundSettlementAmountTaxIncluded,
                    outboundCostAmountTaxIncluded,
                    outboundSettlementAmountTaxIncluded,
                    outboundDiscountSettlementAmountTaxIncluded,
                    outboundDiscountGrossProfitTaxIncluded,
                    crossMonthDocument,
                    inoutDirection,
                    gift,
                    remark
            );
        }
    }

    private static final class InventoryScope {
        private final String scopeType;
        private final Long scopeId;
        private final Long groupId;

        private InventoryScope(String scopeTypeValue, Long scopeIdValue, Long groupIdValue) {
            this.scopeType = scopeTypeValue;
            this.scopeId = scopeIdValue;
            this.groupId = groupIdValue;
        }

        private String scopeType() {
            return scopeType;
        }

        private Long scopeId() {
            return scopeId;
        }

        private Long groupId() {
            return groupId;
        }
    }

    /** 库存分页数据模型，承载列表数据和分页信息。 */
    public record DishConsumptionOutboundReportPage(List<DishConsumptionOutboundReportRow> list,
                                                    long total,
                                                    int pageNo,
                                                    int pageSize,
                                                    DishConsumptionOutboundReportSummary summary) {
    }

    /** 库存数据模型，承载菜品消耗出库报表汇总数据。 */
    public record DishConsumptionOutboundReportSummary(BigDecimal dishQty,
                                                       BigDecimal theoreticalQty,
                                                       BigDecimal outboundQty,
                                                       BigDecimal pendingOutboundQty,
                                                       BigDecimal unlinkedWarehousePendingQty,
                                                       BigDecimal otherReasonPendingQty) {
    }

    /** 库存行数据模型，承载列表或报表明细。 */
    public record DishConsumptionOutboundReportRow(Object id,
                                                   String consumptionNo,
                                                   String businessDate,
                                                   String dishSpuCode,
                                                   String dishSkuCode,
                                                   String dishName,
                                                   String dishSpec,
                                                   String costCard,
                                                   String orderSource,
                                                   String dishCategory,
                                                   String deductionType,
                                                   String itemCode,
                                                   String itemName,
                                                   String specModel,
                                                   String warehouse,
                                                   String itemUnit,
                                                   BigDecimal dishQty,
                                                   BigDecimal theoreticalQty,
                                                   BigDecimal outboundQty,
                                                   BigDecimal pendingOutboundQty,
                                                   BigDecimal unlinkedWarehousePendingQty,
                                                   BigDecimal otherReasonPendingQty) {
    }

    /** 库存行数据模型，承载列表或报表明细。 */
    public record InventoryInoutDetailReportRow(Object id,
                                                String itemCode,
                                                String itemName,
                                                String specModel,
                                                String itemCategory,
                                                String statisticType,
                                                String baseUnit,
                                                String unit,
                                                String orgName,
                                                String orgCode,
                                                String warehouse,
                                                String warehouseType,
                                                String upstreamDocumentNo,
                                                String upstreamDocumentType,
                                                String documentNo,
                                                String inoutType,
                                                String reasonType,
                                                String adjustmentDocument,
                                                String oppositeOrg,
                                                String oppositeOrgCode,
                                                String upstreamDocumentDate,
                                                String documentDate,
                                                String documentCreatedAt,
                                                String documentCreator,
                                                String documentAuditTime,
                                                BigDecimal inboundBaseQty,
                                                BigDecimal inboundQty,
                                                BigDecimal outboundBaseQty,
                                                BigDecimal outboundQty,
                                                BigDecimal inboundCostAmountTaxIncluded,
                                                BigDecimal inboundSettlementAmountTaxIncluded,
                                                BigDecimal outboundCostAmountTaxIncluded,
                                                BigDecimal outboundSettlementAmountTaxIncluded,
                                                BigDecimal outboundDiscountSettlementAmountTaxIncluded,
                                                BigDecimal outboundDiscountGrossProfitTaxIncluded,
                                                String crossMonthDocument,
                                                 String inoutDirection,
                                                 String gift,
                                                 String remark) {
    }

    /** 库存数据模型，承载物品批量追踪报表数据。 */
    public record ItemBatchTraceReport(List<ItemBatchTraceSourceRow> sourceRows,
                                       List<ItemBatchTraceInternalFlowRow> internalFlowRows,
                                       List<ItemBatchTraceExternalFlowRow> externalFlowRows) {
    }

    /** 库存行数据模型，承载列表或报表明细。 */
    public record ItemBatchTraceSourceRow(Object id,
                                          String sourceOrg,
                                          String inboundOrg,
                                          String inboundWarehouse,
                                          String inboundType,
                                          String inboundDocumentNo,
                                          String upstreamDocumentNo,
                                          String inboundDate,
                                          String inboundCreatedAt,
                                          String batchNo,
                                          String manufacturer,
                                          String itemCode,
                                          String itemName,
                                          String specModel,
                                          String unit,
                                          BigDecimal inboundQty) {
    }

    /** 库存行数据模型，承载列表或报表明细。 */
    public record ItemBatchTraceInternalFlowRow(Object id,
                                                String orgName,
                                                String warehouse,
                                                String inoutType,
                                                String documentNo,
                                                String upstreamDocumentNo,
                                                String documentDate,
                                                String documentCreatedAt,
                                                String batchNo,
                                                String itemCode,
                                                String itemName,
                                                String specModel,
                                                String unit,
                                                BigDecimal inoutQty,
                                                BigDecimal currentBalanceQty) {
    }

    /** 库存行数据模型，承载列表或报表明细。 */
    public record ItemBatchTraceExternalFlowRow(Object id,
                                                String orgName,
                                                String warehouse,
                                                String targetOrg,
                                                String outboundType,
                                                String outboundDocumentNo,
                                                String upstreamDocumentNo,
                                                String outboundDate,
                                                String outboundCreatedAt,
                                                String batchNo,
                                                String itemCode,
                                                String itemName,
                                                String specModel,
                                                String unit,
                                                BigDecimal inoutQty,
                                                BigDecimal targetBalanceQty) {
    }

    private record BatchTraceDocumentRow(Object id,
                                         String sourceOrg,
                                         String orgName,
                                         String warehouse,
                                         String targetOrg,
                                         String documentType,
                                         String documentNo,
                                         String upstreamDocumentNo,
                                         String documentDate,
                                         String documentCreatedAt,
                                         String batchNo,
                                         String manufacturer,
                                         String itemCode,
                                         String itemName,
                                         String specModel,
                                         String unit,
                                         BigDecimal inoutQty,
                                         BigDecimal currentBalanceQty,
                                         boolean inbound) {
    }

    /** 库存分页数据模型，承载列表数据和分页信息。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }
}
