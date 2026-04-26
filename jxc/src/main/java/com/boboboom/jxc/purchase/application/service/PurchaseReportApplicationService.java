package com.boboboom.jxc.purchase.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PageData;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchaseOrderStatusTrackingRow;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchasePriceAnalysisPeriod;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchasePriceAnalysisReport;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchasePriceAnalysisRow;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchasePriceAnalysisValue;
import com.boboboom.jxc.purchase.application.service.PurchaseReportRows.PurchaseReturnStatusTrackingRow;
import com.boboboom.jxc.purchase.domain.repository.PurchaseReportRepository;

/** 采购报表业务服务，负责采购订单、退货和价格分析查询。 */
@Service
public class PurchaseReportApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DOCUMENT_ORDER = "ORDER";
    private static final String DOCUMENT_RETURN = "RETURN";
    private static final String DATE_TYPE_ORDER = "订货日期";
    private static final String DATE_TYPE_EXPECTED_ARRIVAL = "期望到货日期";
    private static final String DATE_TYPE_RECEIPT = "收货日期";
    private static final String GIFT_ALL = "全部";
    private static final String YES = "是";
    private static final String NO = "否";
    private static final String PERIOD_DAY = "按日";
    private static final String PERIOD_MONTH = "按月";
    private static final String SORT_ASC = "按时间正序";
    private static final DateTimeFormatter PERIOD_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MONEY_SCALE = 2;
    private static final int QTY_SCALE = 4;
    private static final int RATE_PERCENT = 100;
    private static final int DEFAULT_ANALYSIS_WEEKS = 7;

    private final PurchaseReportRepository purchaseReportRepository;
    private final OrgScopeService orgScopeService;
    private final DataScopeAccessService dataScopeAccessService;

    /** 采购报表业务服务，负责采购订单、退货和价格分析查询。 */
    public PurchaseReportApplicationService(PurchaseReportRepository purchaseReportRepositoryValue,
                                            OrgScopeService orgScopeServiceValue,
                                            DataScopeAccessService dataScopeAccessServiceValue) {
        this.purchaseReportRepository = purchaseReportRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
    }

    /** 查询采购订单状态跟踪报表。 */
    public PageData<PurchaseOrderStatusTrackingRow> purchaseOrderStatusTrackingReport(Map<String, String> params) {
        PageRequest pageRequest = resolvePageRequest(params);
        List<PurchaseOrderStatusTrackingRow> rows = loadReportLines(params, DOCUMENT_ORDER).stream()
                .filter(line -> matchesOrderLine(line, params))
                .map(this::toOrderRow)
                .toList();
        return paginate(rows, pageRequest);
    }

    /** 查询采购退货状态跟踪报表。 */
    public PageData<PurchaseReturnStatusTrackingRow> purchaseReturnStatusTrackingReport(Map<String, String> params) {
        PageRequest pageRequest = resolvePageRequest(params);
        List<PurchaseReturnStatusTrackingRow> rows = loadReportLines(params, DOCUMENT_RETURN).stream()
                .filter(line -> matchesReturnLine(line, params))
                .map(this::toReturnRow)
                .toList();
        return paginate(rows, pageRequest);
    }

    /** 查询采购物品价格分析报表。 */
    public PurchasePriceAnalysisReport purchaseItemPriceAnalysisReport(Map<String, String> params) {
        PageRequest pageRequest = resolvePageRequest(params);
        LocalDate[] range = resolveAnalysisDateRange(params);
        List<PurchasePriceAnalysisPeriod> periods = buildPeriods(range[0], range[1], params.get("statisticPeriod"), params.get("timeSort"));
        List<PurchaseReportLineSnapshot> sourceLines = loadReportLines(params, DOCUMENT_ORDER).stream()
                .filter(line -> matchesPriceAnalysisLine(line, params, range[0], range[1]))
                .toList();
        List<PurchasePriceAnalysisRow> rows = buildPriceRows(sourceLines, periods, params.get("detailGranularity"));
        return new PurchasePriceAnalysisReport(periods, paginate(rows, pageRequest));
    }

    private List<PurchaseReportLineSnapshot> loadReportLines(Map<String, String> params, String documentType) {
        OrgScopeService.AccessibleScope scope = resolveScope(params.get("orgId"));
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        return purchaseReportRepository.findLines(scope.scopeType(), scope.scopeId(), documentType, operatorId, viewAll);
    }

    private boolean matchesOrderLine(PurchaseReportLineSnapshot line, Map<String, String> params) {
        LocalDate[] range = parseRange(params);
        LocalDate date = resolveOrderDate(line, trimToDefault(params.get("dateType"), DATE_TYPE_ORDER));
        return matchDate(date, range[0], range[1])
                && equalsIfPresent(resolveLineWarehouse(line), params.get("purchaseWarehouse"))
                && equalsIfPresent(resolveLineWarehouse(line), params.get("receiptWarehouse"))
                && equalsIfPresent(line.itemCode(), params.get("itemCode"))
                && equalsIfPresent(line.supplierName(), params.get("supplier"))
                && contains(line.documentCode(), params.get("purchaseOrderCode"))
                && equalsIfPresent(line.documentStatus(), params.get("documentStatus"))
                && equalsIfPresent(resolveReceiveStatus(line), params.get("receiveStatus"))
                && matchesGift(line, params.get("isGift"))
                && matchesCrossMonth(line, params.get("crossMonth"));
    }

    private boolean matchesReturnLine(PurchaseReportLineSnapshot line, Map<String, String> params) {
        LocalDate[] range = parseRange(params);
        return matchDate(line.documentDate(), range[0], range[1])
                && equalsIfPresent(resolveLineWarehouse(line), params.get("shippingWarehouse"))
                && equalsIfPresent(line.supplierName(), params.get("supplier"))
                && contains(line.documentCode(), params.get("returnCode"))
                && equalsIfPresent(line.itemCode(), params.get("itemCode"))
                && equalsIfPresent(line.documentStatus(), normalizeAll(params.get("documentStatus")))
                && matchesGift(line, params.get("isGift"));
    }

    private boolean matchesPriceAnalysisLine(PurchaseReportLineSnapshot line, Map<String, String> params, LocalDate start, LocalDate end) {
        return matchDate(line.documentDate(), start, end)
                && equalsIfPresent(line.supplierName(), params.get("supplier"))
                && equalsIfPresent(line.itemCode(), params.get("itemCode"))
                && equalsIfPresent(line.itemCategory(), params.get("itemCategory"));
    }

    private PurchaseOrderStatusTrackingRow toOrderRow(PurchaseReportLineSnapshot line) {
        BigDecimal purchaseQty = amount(line.quantity());
        BigDecimal auditQty = amount(line.reviewQty());
        BigDecimal unitPrice = amount(line.unitPrice());
        BigDecimal purchaseAmount = amount(line.amount());
        BigDecimal auditAmount = auditQty.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal receivedQty = amount(line.receivedQty());
        BigDecimal receiptAmount = receivedQty.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal unreceivedQty = purchaseQty.subtract(receivedQty).max(BigDecimal.ZERO).setScale(QTY_SCALE, RoundingMode.HALF_UP);
        return new PurchaseOrderStatusTrackingRow(
                String.valueOf(line.lineId()),
                text(line.documentCode()),
                text(line.documentStatus()),
                resolveReceiveStatus(line),
                formatDate(line.documentDate()),
                formatDate(resolveExpectedArrivalDate(line)),
                text(line.supplierName()),
                text(line.itemName()),
                text(line.spec()),
                text(line.itemCategory()),
                giftLabel(line),
                text(line.purchaseUnit()),
                unitPrice,
                purchaseQty,
                auditQty,
                purchaseAmount,
                auditAmount,
                resolveLineWarehouse(line),
                "-",
                resolveLineWarehouse(line),
                receivedQty,
                receiptAmount,
                unreceivedQty,
                BigDecimal.ZERO.setScale(QTY_SCALE, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        );
    }

    private PurchaseReturnStatusTrackingRow toReturnRow(PurchaseReportLineSnapshot line) {
        BigDecimal returnQty = amount(line.quantity());
        BigDecimal returnBaseQty = toBaseQty(returnQty, line.baseConversion());
        BigDecimal auditQty = amount(line.reviewQty());
        BigDecimal shippedQty = isApproved(line.documentStatus()) ? returnQty : BigDecimal.ZERO.setScale(QTY_SCALE, RoundingMode.HALF_UP);
        BigDecimal shippedBaseQty = isApproved(line.documentStatus())
                ? returnBaseQty
                : BigDecimal.ZERO.setScale(QTY_SCALE, RoundingMode.HALF_UP);
        BigDecimal returnAmount = amount(line.amount());
        BigDecimal shippedAmount = isApproved(line.documentStatus())
                ? returnAmount
                : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new PurchaseReturnStatusTrackingRow(
                String.valueOf(line.lineId()),
                text(line.documentCode()),
                text(line.documentStatus()),
                formatDate(line.documentDate()),
                defaultIfBlank(line.sourceDocumentCode(), "-"),
                text(line.supplierCode()),
                text(line.supplierName()),
                text(line.itemName()),
                text(line.spec()),
                text(line.itemCategory()),
                text(line.purchaseUnit()),
                text(line.baseUnit()),
                giftLabel(line),
                returnQty,
                returnBaseQty,
                returnAmount,
                auditQty,
                shippedQty,
                shippedBaseQty,
                shippedAmount,
                resolveLineWarehouse(line)
        );
    }

    private List<PurchasePriceAnalysisRow> buildPriceRows(List<PurchaseReportLineSnapshot> sourceLines,
                                                          List<PurchasePriceAnalysisPeriod> periods,
                                                          String detailGranularity) {
        Map<String, List<PurchaseReportLineSnapshot>> grouped = new LinkedHashMap<>();
        for (PurchaseReportLineSnapshot line : sourceLines) {
            grouped.computeIfAbsent(priceGroupKey(line, detailGranularity), key -> new ArrayList<>()).add(line);
        }
        return grouped.values().stream()
                .map(lines -> toPriceAnalysisRow(lines.get(0), lines, periods, detailGranularity))
                .sorted(Comparator.comparing(PurchasePriceAnalysisRow::itemCode).thenComparing(PurchasePriceAnalysisRow::supplier))
                .toList();
    }

    private PurchasePriceAnalysisRow toPriceAnalysisRow(PurchaseReportLineSnapshot sample,
                                                        List<PurchaseReportLineSnapshot> lines,
                                                        List<PurchasePriceAnalysisPeriod> periods,
                                                        String detailGranularity) {
        List<PurchasePriceAnalysisValue> values = new ArrayList<>();
        BigDecimal previousAvg = null;
        for (PurchasePriceAnalysisPeriod period : periods) {
            List<PurchaseReportLineSnapshot> periodLines = lines.stream()
                    .filter(line -> Objects.equals(period.key(), periodKey(line.documentDate(), period)))
                    .toList();
            BigDecimal total = periodLines.stream().map(line -> amount(line.amount())).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal qty = periodLines.stream().map(line -> amount(line.quantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = qty.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(QTY_SCALE, RoundingMode.HALF_UP)
                    : total.divide(qty, QTY_SCALE, RoundingMode.HALF_UP);
            BigDecimal fluctuation = calculateFluctuation(previousAvg, avg);
            values.add(new PurchasePriceAnalysisValue(period.key(), total, avg, fluctuation));
            if (avg.compareTo(BigDecimal.ZERO) > 0) {
                previousAvg = avg;
            }
        }
        String supplier = "到供应商粒度".equals(detailGranularity) ? text(sample.supplierName()) : "";
        return new PurchasePriceAnalysisRow(
                priceGroupKey(sample, detailGranularity),
                text(sample.itemName()),
                text(sample.itemCode()),
                text(sample.spec()),
                text(sample.itemCategory()),
                text(sample.baseUnit()),
                text(sample.purchaseUnit()),
                supplier,
                values
        );
    }

    private BigDecimal calculateFluctuation(BigDecimal previousAvg, BigDecimal avg) {
        if (previousAvg == null || previousAvg.compareTo(BigDecimal.ZERO) == 0 || avg.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return avg.subtract(previousAvg)
                .multiply(BigDecimal.valueOf(RATE_PERCENT))
                .divide(previousAvg, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private List<PurchasePriceAnalysisPeriod> buildPeriods(LocalDate start, LocalDate end, String statisticPeriod, String timeSort) {
        List<PurchasePriceAnalysisPeriod> periods = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDate periodStart = cursor;
            LocalDate periodEnd = resolvePeriodEnd(cursor, end, statisticPeriod);
            periods.add(toPeriod(periodStart, periodEnd));
            cursor = periodEnd.plusDays(1);
        }
        if (!SORT_ASC.equals(timeSort)) {
            List<PurchasePriceAnalysisPeriod> reversedPeriods = new ArrayList<>(periods);
            java.util.Collections.reverse(reversedPeriods);
            return reversedPeriods;
        }
        return periods;
    }

    private LocalDate resolvePeriodEnd(LocalDate cursor, LocalDate end, String statisticPeriod) {
        if (PERIOD_DAY.equals(statisticPeriod)) {
            return cursor;
        }
        if (PERIOD_MONTH.equals(statisticPeriod)) {
            return cursor.withDayOfMonth(cursor.lengthOfMonth()).isAfter(end)
                    ? end
                    : cursor.withDayOfMonth(cursor.lengthOfMonth());
        }
        LocalDate weekEnd = cursor.with(DayOfWeek.SUNDAY);
        return weekEnd.isAfter(end) ? end : weekEnd;
    }

    private PurchasePriceAnalysisPeriod toPeriod(LocalDate start, LocalDate end) {
        String key = start.format(PERIOD_KEY_FORMATTER) + "_" + end.format(PERIOD_KEY_FORMATTER);
        String label = formatShortDate(start) + " - " + formatShortDate(end);
        String compactLabel = formatShortDate(start) + "-" + formatShortDate(end);
        return new PurchasePriceAnalysisPeriod(key, label, compactLabel);
    }

    private String periodKey(LocalDate date, PurchasePriceAnalysisPeriod period) {
        if (date == null) {
            return "";
        }
        String[] parts = period.key().split("_");
        LocalDate start = LocalDate.parse(parts[0], PERIOD_KEY_FORMATTER);
        LocalDate end = LocalDate.parse(parts[1], PERIOD_KEY_FORMATTER);
        return !date.isBefore(start) && !date.isAfter(end) ? period.key() : "";
    }

    private String priceGroupKey(PurchaseReportLineSnapshot line, String detailGranularity) {
        String supplier = "到供应商粒度".equals(detailGranularity) ? text(line.supplierName()) : "";
        return text(line.itemCode()) + "|" + supplier;
    }

    private LocalDate[] resolveAnalysisDateRange(Map<String, String> params) {
        LocalDate[] parsed = parseRange(params);
        LocalDate end = parsed[1] == null ? LocalDate.now() : parsed[1];
        LocalDate start = parsed[0] == null ? end.minusWeeks(DEFAULT_ANALYSIS_WEEKS).plusDays(1) : parsed[0];
        if (start.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new LocalDate[] {start, end};
    }

    private LocalDate[] parseRange(Map<String, String> params) {
        LocalDate start = parseDateNullable(firstParam(params, "startDate", "dateRangeStart"));
        LocalDate end = parseDateNullable(firstParam(params, "endDate", "dateRangeEnd"));
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new LocalDate[] {start, end};
    }

    private LocalDate resolveOrderDate(PurchaseReportLineSnapshot line, String dateType) {
        if (DATE_TYPE_EXPECTED_ARRIVAL.equals(dateType)) {
            return resolveExpectedArrivalDate(line);
        }
        if (DATE_TYPE_RECEIPT.equals(dateType)) {
            return line.receivedQty() != null && line.receivedQty().compareTo(BigDecimal.ZERO) > 0 ? line.documentDate() : null;
        }
        return line.documentDate();
    }

    private LocalDate resolveExpectedArrivalDate(PurchaseReportLineSnapshot line) {
        return line.lineExpectedArrivalDate() == null ? line.expectedArrivalDate() : line.lineExpectedArrivalDate();
    }

    private String resolveReceiveStatus(PurchaseReportLineSnapshot line) {
        BigDecimal qty = amount(line.quantity());
        BigDecimal received = amount(line.receivedQty());
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            return "未收货";
        }
        if (received.compareTo(qty) >= 0) {
            return "已收货";
        }
        return "部分收货";
    }

    private boolean matchesCrossMonth(PurchaseReportLineSnapshot line, String crossMonth) {
        String normalized = normalizeAll(crossMonth);
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        LocalDate expected = resolveExpectedArrivalDate(line);
        boolean crossed = line.documentDate() != null && expected != null && line.documentDate().getMonth() != expected.getMonth();
        return YES.equals(normalized) == crossed;
    }

    private boolean matchesGift(PurchaseReportLineSnapshot line, String isGift) {
        String normalized = normalizeAll(isGift);
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        return Objects.equals(giftLabel(line), normalized);
    }

    private String giftLabel(PurchaseReportLineSnapshot line) {
        return Boolean.TRUE.equals(line.isGift()) ? YES : NO;
    }

    private boolean isApproved(String documentStatus) {
        return "已审核".equals(documentStatus) || "已收货".equals(documentStatus);
    }

    private BigDecimal toBaseQty(BigDecimal qty, String baseConversion) {
        BigDecimal rate = parseBaseConversionRate(baseConversion);
        return qty.multiply(rate).setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal parseBaseConversionRate(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ONE;
        }
        String text = value.trim();
        int index = text.indexOf('=');
        if (index < 0 || index >= text.length() - 1) {
            return BigDecimal.ONE;
        }
        String right = text.substring(index + 1).replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(right)) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(right);
        } catch (NumberFormatException ex) {
            return BigDecimal.ONE;
        }
    }

    private String resolveLineWarehouse(PurchaseReportLineSnapshot line) {
        return defaultIfBlank(line.lineWarehouseName(), defaultIfBlank(line.warehouseName(), ""));
    }

    private <T> PageData<T> paginate(List<T> rows, PageRequest pageRequest) {
        int start = Math.min((pageRequest.pageNo() - 1) * pageRequest.pageSize(), rows.size());
        int end = Math.min(start + pageRequest.pageSize(), rows.size());
        return new PageData<>(rows.subList(start, end), rows.size(), pageRequest.pageNo(), pageRequest.pageSize());
    }

    private PageRequest resolvePageRequest(Map<String, String> params) {
        int pageNo = parsePositiveInt(params.get("pageNo"), 1);
        int pageSize = Math.min(parsePositiveInt(params.get("pageSize"), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        return new PageRequest(pageNo, pageSize);
    }

    private OrgScopeService.AccessibleScope resolveScope(String orgId) {
        Long userId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return orgScopeService.resolveAccessibleScope(userId, orgId);
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

    private boolean equalsIfPresent(String value, String expected) {
        return !StringUtils.hasText(expected) || Objects.equals(value, expected);
    }

    private boolean contains(String value, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return defaultIfBlank(value, "").toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private String normalizeAll(String value) {
        String text = trimNullable(value);
        if (!StringUtils.hasText(text) || GIFT_ALL.equals(text)) {
            return null;
        }
        return text;
    }

    private String firstParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private LocalDate parseDateNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式不正确");
        }
    }

    private int parsePositiveInt(String value, int fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String formatShortDate(LocalDate value) {
        return value.getMonthValue() + "." + value.getDayOfMonth();
    }

    private String text(String value) {
        return defaultIfBlank(value, "");
    }

    private String trimToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record PageRequest(int pageNo, int pageSize) {
    }
}
