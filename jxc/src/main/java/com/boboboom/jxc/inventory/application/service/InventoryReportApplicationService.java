package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存报表业务服务。
 */
@Service
public class InventoryReportApplicationService {

    private static final int MAX_PAGE_SIZE = 200;

    private final OrgScopeService orgScopeService;
    private final GroupRepository groupRepository;
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundLineRepository purchaseInboundLineRepository;
    private final InventoryCheckRepository inventoryCheckRepository;

    public InventoryReportApplicationService(OrgScopeService orgScopeService,
                                             GroupRepository groupRepository,
                                             StoreRepository storeRepository,
                                             WarehouseRepository warehouseRepository,
                                             InventoryDocumentRepository inventoryDocumentRepository,
                                             PurchaseInboundRepository purchaseInboundRepository,
                                             PurchaseInboundLineRepository purchaseInboundLineRepository,
                                             InventoryCheckRepository inventoryCheckRepository) {
        this.orgScopeService = orgScopeService;
        this.groupRepository = groupRepository;
        this.storeRepository = storeRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryDocumentRepository = inventoryDocumentRepository;
        this.purchaseInboundRepository = purchaseInboundRepository;
        this.purchaseInboundLineRepository = purchaseInboundLineRepository;
        this.inventoryCheckRepository = inventoryCheckRepository;
    }

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
        rows.addAll(loadDishRows(scope, InventoryDocumentType.CUSTOMER_SALES_OUTBOUND, "销售扣减"));
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
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.PRODUCTION_INBOUND, "生产入库", "入库", "", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.CUSTOMER_SALES_OUTBOUND, "菜品消耗出库", "出库", "销售订单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.CUSTOMER_RETURN_INBOUND, "客户退货入库", "入库", "销售订单", "否"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STORE_TRANSFER, "调拨单", "出库", "调拨单", "是"));
        rows.addAll(loadGenericDocumentRows(scope, InventoryDocumentType.STOCK_TRANSFER_OUTBOUND, "移库出库", "出库", "调拨单", "是"));
        rows.addAll(loadInventoryCheckRows(scope));

        List<InventoryInoutDetailReportRow> filtered = rows.stream()
                .filter(row -> matchDate(row.documentDate(), start, end))
                .filter(row -> matchAuditTime(row.documentAuditTime(), auditStart, auditEnd))
                .filter(row -> !StringUtils.hasText(warehouseKeyword) || toLower(row.warehouse()).contains(warehouseKeyword))
                .filter(row -> !StringUtils.hasText(warehouseTypeValue) || Objects.equals(row.warehouseType(), warehouseTypeValue))
                .filter(row -> !StringUtils.hasText(inoutTypeValue) || Objects.equals(row.inoutType(), inoutTypeValue))
                .filter(row -> !StringUtils.hasText(upstreamDocumentTypeValue) || Objects.equals(row.upstreamDocumentType(), upstreamDocumentTypeValue))
                .filter(row -> !StringUtils.hasText(itemCategoryValue) || Objects.equals(row.itemCategory(), itemCategoryValue))
                .filter(row -> !StringUtils.hasText(statisticTypeValue) || Objects.equals(row.statisticType(), statisticTypeValue))
                .filter(row -> !StringUtils.hasText(itemCodeValue) || toLower(row.itemCode()).contains(toLower(itemCodeValue)))
                .filter(row -> !StringUtils.hasText(reasonTypeValue) || Objects.equals(row.reasonType(), reasonTypeValue))
                .filter(row -> !StringUtils.hasText(adjustmentDocumentValue) || Objects.equals(row.adjustmentDocument(), adjustmentDocumentValue))
                .filter(row -> !StringUtils.hasText(oppositeOrgValue) || toLower(row.oppositeOrg()).contains(toLower(oppositeOrgValue)))
                .filter(row -> !StringUtils.hasText(documentNoValue) || toLower(row.documentNo()).contains(toLower(documentNoValue)))
                .filter(row -> !StringUtils.hasText(crossMonthDocumentValue) || Objects.equals(row.crossMonthDocument(), crossMonthDocumentValue))
                .filter(row -> !StringUtils.hasText(inoutDirectionValue) || Objects.equals(row.inoutDirection(), inoutDirectionValue))
                .filter(row -> !StringUtils.hasText(giftValue) || Objects.equals(row.gift(), giftValue))
                .filter(row -> !StringUtils.hasText(unitTypeValue) || Objects.equals(row.unit(), unitTypeValue))
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

    private List<DishConsumptionOutboundReportRow> loadDishRows(InventoryScope scope,
                                                                 InventoryDocumentType type,
                                                                 String deductionType) {
        List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        List<DishConsumptionOutboundReportRow> rows = new ArrayList<>();
        for (InventoryDocumentHeader header : headers) {
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
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
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
        List<PurchaseInboundDO> headers = purchaseInboundRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        List<PurchaseInboundLineDO> lines = purchaseInboundLineRepository.findByInboundIds(headers.stream().map(PurchaseInboundDO::getId).toList());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = lines.stream().collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));
        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        for (PurchaseInboundDO header : headers) {
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
                        "",
                        header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                        header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                        formatDateTime(header.getCreatedAt()),
                        defaultIfBlank(header.getCreatedBy() == null ? null : String.valueOf(header.getCreatedBy()), ""),
                        formatDateTime(header.getApprovedAt()),
                        quantity,
                        quantity,
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
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
        List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
        Map<Long, List<InventoryDocumentLine>> lineMap = inventoryDocumentRepository.findLinesByHeaderIds(type, headers.stream().map(InventoryDocumentHeader::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        for (InventoryDocumentHeader header : headers) {
            for (InventoryDocumentLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                BigDecimal quantity = defaultQuantity(line.getQuantity());
                BigDecimal amount = line.getAmount() == null ? quantity.multiply(defaultQuantity(line.getUnitPrice())).setScale(2, RoundingMode.HALF_UP) : line.getAmount();
                BigDecimal inboundBaseQty = "入库".equals(inoutDirection) ? quantity : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                BigDecimal inboundQty = inboundBaseQty;
                BigDecimal outboundBaseQty = "出库".equals(inoutDirection) ? quantity : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                BigDecimal outboundQty = outboundBaseQty;
                BigDecimal inboundAmount = "入库".equals(inoutDirection) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal outboundAmount = "出库".equals(inoutDirection) ? amount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                rows.add(new InventoryInoutDetailReportRow(
                        buildRowId(type.getPathSegment(), header.getId(), line.getId()),
                        defaultIfBlank(line.getItemCode(), ""),
                        defaultIfBlank(line.getItemName(), ""),
                        defaultIfBlank(line.getSpec(), ""),
                        defaultIfBlank(line.getCategory(), ""),
                        defaultIfBlank(line.getCategory(), ""),
                        defaultIfBlank(line.getUnitName(), "库存单位"),
                        defaultOrgName(scope),
                        defaultOrgCode(scope),
                        resolveWarehouseName(type, header),
                        resolveWarehouseType(scope, resolveWarehouseName(type, header)),
                        defaultIfBlank(header.getUpstreamCode(), ""),
                        upstreamDocumentType,
                        defaultIfBlank(header.getDocumentCode(), ""),
                        inoutTypeLabel,
                        defaultIfBlank(header.getReason(), defaultIfBlank(line.getLineReason(), "")),
                        adjustmentDocument,
                        defaultIfBlank(header.getCounterpartyName(), defaultIfBlank(header.getSecondaryName(), "")),
                        "",
                        "",
                        header.getDocumentDate() == null ? "" : header.getDocumentDate().toString(),
                        header.getDocumentDate() == null ? "" : header.getDocumentDate().toString(),
                        formatDateTime(header.getCreatedAt()),
                        defaultIfBlank(header.getCreatedBy() == null ? null : String.valueOf(header.getCreatedBy()), ""),
                        formatDateTime(header.getApprovedAt()),
                        inboundBaseQty,
                        inboundQty,
                        outboundBaseQty,
                        outboundQty,
                        inboundAmount,
                        inboundAmount,
                        outboundAmount,
                        outboundAmount,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        crossMonthDocument(header.getDocumentDate(), header.getCreatedAt()),
                        inoutDirection,
                        detectGift(header.getRemark(), line.getLineReason()),
                        defaultIfBlank(header.getRemark(), "")
                ));
            }
        }
        return rows;
    }

    private List<InventoryInoutDetailReportRow> loadInventoryCheckRows(InventoryScope scope) {
        List<InventoryInoutDetailReportRow> rows = new ArrayList<>();
        for (InventoryCheckKind kind : List.of(InventoryCheckKind.INVENTORY_CHECK, InventoryCheckKind.MULTI_INVENTORY_CHECK)) {
            List<InventoryCheckHeader> headers = inventoryCheckRepository.findHeadersByScopeAndKindOrdered(kind, scope.scopeType(), scope.scopeId());
            Map<Long, List<InventoryCheckLine>> lineMap = inventoryCheckRepository.findLinesByHeaderIds(kind, headers.stream().map(InventoryCheckHeader::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(InventoryCheckLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
            for (InventoryCheckHeader header : headers) {
                for (InventoryCheckLine line : lineMap.getOrDefault(header.getId(), List.of())) {
                    BigDecimal profitQty = defaultQuantity(line.getProfitQty());
                    BigDecimal lossQty = defaultQuantity(line.getLossQty());
                    BigDecimal inboundAmount = defaultQuantity(line.getProfitAmount());
                    BigDecimal outboundAmount = defaultQuantity(line.getLossAmount());
                    BigDecimal inboundBaseQty = profitQty;
                    BigDecimal outboundBaseQty = lossQty;
                    String inoutTypeLabel = profitQty.compareTo(BigDecimal.ZERO) > 0 ? "盘盈入库" : "盘亏出库";
                    String inoutDirection = profitQty.compareTo(BigDecimal.ZERO) > 0 ? "入库" : "出库";
                    rows.add(new InventoryInoutDetailReportRow(
                            buildRowId(kind.getHeaderTable(), header.getId(), line.getId()),
                            defaultIfBlank(line.getItemCode(), ""),
                            defaultIfBlank(line.getItemName(), ""),
                            defaultIfBlank(line.getSpec(), ""),
                            defaultIfBlank(line.getCategory(), ""),
                            defaultIfBlank(line.getCategory(), ""),
                            defaultIfBlank(line.getUnitName(), "库存单位"),
                            defaultOrgName(scope),
                            defaultOrgCode(scope),
                            defaultIfBlank(header.getWarehouseName(), ""),
                            resolveWarehouseType(scope, header.getWarehouseName()),
                            defaultIfBlank(header.getThirdPartyDocument(), ""),
                            "盘点单",
                            defaultIfBlank(header.getDocumentCode(), ""),
                            inoutTypeLabel,
                            defaultIfBlank(line.getProfitLossReason(), ""),
                            "是",
                            defaultIfBlank(header.getPlanName(), defaultIfBlank(header.getRemark(), "")),
                            "",
                            "",
                            header.getCheckDate() == null ? "" : header.getCheckDate().toString(),
                            header.getCheckDate() == null ? "" : header.getCheckDate().toString(),
                            formatDateTime(header.getCreatedAt()),
                            defaultIfBlank(header.getCreatedBy() == null ? null : String.valueOf(header.getCreatedBy()), ""),
                            formatDateTime(header.getApprovedAt()),
                            "入库".equals(inoutDirection) ? inboundBaseQty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            "入库".equals(inoutDirection) ? profitQty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            "出库".equals(inoutDirection) ? outboundBaseQty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            "出库".equals(inoutDirection) ? lossQty : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                            "入库".equals(inoutDirection) ? inboundAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            "入库".equals(inoutDirection) ? inboundAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            "出库".equals(inoutDirection) ? outboundAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            "出库".equals(inoutDirection) ? outboundAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            "否",
                            inoutDirection,
                            "否",
                            defaultIfBlank(header.getRemark(), "")
                    ));
                }
            }
        }
        return rows;
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
        if (type == InventoryDocumentType.CUSTOMER_SALES_OUTBOUND) {
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

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean hasAnyText(String value) {
        return StringUtils.hasText(value);
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

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
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

        private InventoryScope(String scopeType, Long scopeId, Long groupId) {
            this.scopeType = scopeType;
            this.scopeId = scopeId;
            this.groupId = groupId;
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

    public record DishConsumptionOutboundReportPage(List<DishConsumptionOutboundReportRow> list,
                                                    long total,
                                                    int pageNo,
                                                    int pageSize,
                                                    DishConsumptionOutboundReportSummary summary) {
    }

    public record DishConsumptionOutboundReportSummary(BigDecimal dishQty,
                                                       BigDecimal theoreticalQty,
                                                       BigDecimal outboundQty,
                                                       BigDecimal pendingOutboundQty,
                                                       BigDecimal unlinkedWarehousePendingQty,
                                                       BigDecimal otherReasonPendingQty) {
    }

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

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }
}
