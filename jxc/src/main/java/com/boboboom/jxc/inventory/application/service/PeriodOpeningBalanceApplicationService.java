package com.boboboom.jxc.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryPeriodOpeningRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningLineDO;
import com.boboboom.jxc.item.domain.repository.ItemProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemProfileDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 周期期初库存业务服务。
 */
@Service
public class PeriodOpeningBalanceApplicationService {

    public static final String BUSINESS_CODE = "PERIOD_OPENING_BALANCE";
    private static final String WORKFLOW_LABEL = "期初库存流程";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_GENERATED = "GENERATED";
    private static final String PENDING_NONE = "NONE";
    private static final String DOCUMENT_PREFIX = "QC";
    private static final int DOCUMENT_CODE_RANDOM_MIN = 1000;
    private static final int DOCUMENT_CODE_RANDOM_MAX = 10000;
    private static final int DOCUMENT_CODE_MAX_ATTEMPTS = 20;
    private static final int QUANTITY_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final int PAGE_SIZE_DEFAULT = 10;
    private static final int PAGE_SIZE_MAX = 200;

    private final InventoryPeriodOpeningRepository periodOpeningRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final InventoryDocumentWorkflowService inventoryDocumentWorkflowService;
    private final ItemProfileRepository itemProfileRepository;
    private final ObjectMapper objectMapper;
    private final OrgScopeService orgScopeService;
    private final DataScopeAccessService dataScopeAccessService;
    private final DictionaryLookupService dictionaryLookupService;

    /** 周期期初库存业务服务。 */
    public PeriodOpeningBalanceApplicationService(InventoryPeriodOpeningRepository periodOpeningRepositoryValue,
                                                  InventoryBalanceRepository inventoryBalanceRepositoryValue,
                                                  InventoryTransactionRepository inventoryTransactionRepositoryValue,
                                                  InventoryStockMutationService inventoryStockMutationServiceValue,
                                                  InventoryDocumentWorkflowService inventoryDocumentWorkflowServiceValue,
                                                  ItemProfileRepository itemProfileRepositoryValue,
                                                  ObjectMapper objectMapperValue,
                                                  OrgScopeService orgScopeServiceValue,
                                                  DataScopeAccessService dataScopeAccessServiceValue,
                                                  DictionaryLookupService dictionaryLookupServiceValue) {
        this.periodOpeningRepository = periodOpeningRepositoryValue;
        this.inventoryBalanceRepository = inventoryBalanceRepositoryValue;
        this.inventoryTransactionRepository = inventoryTransactionRepositoryValue;
        this.inventoryStockMutationService = inventoryStockMutationServiceValue;
        this.inventoryDocumentWorkflowService = inventoryDocumentWorkflowServiceValue;
        this.itemProfileRepository = itemProfileRepositoryValue;
        this.objectMapper = objectMapperValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /**
     * 分页查询期初库存。
     *
     * @param orgId           机构标识
     * @param pageNum         页码
     * @param pageSize        每页数量
     * @param warehouseName   仓库名称
     * @param periodType      周期类型
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param status          状态
     * @return 分页结果
     */
    public PeriodOpeningPage list(String orgId,
                                  Integer pageNum,
                                  Integer pageSize,
                                  String documentCode,
                                  String warehouseName,
                                  String periodType,
                                  String startDate,
                                  String endDate,
                                  String status) {
        InventoryScope scope = resolveScope(orgId);
        ensureViewPermission(scope);
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String documentCodeValue = toLower(trimNullable(documentCode));
        String warehouseValue = trimNullable(warehouseName);
        String periodTypeValue = normalizePeriodTypeNullable(periodType);
        String statusValue = trimNullable(status);
        List<PeriodOpeningRow> rows = periodOpeningRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> matchesHeader(header, documentCodeValue, warehouseValue, periodTypeValue, start, end, statusValue))
                .map(this::toRow)
                .toList();
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int startIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int endIndex = Math.min(startIndex + safePageSize, rows.size());
        return new PeriodOpeningPage(rows.subList(startIndex, endIndex), rows.size(), safePageNum, safePageSize);
    }

    /**
     * 创建手工期初库存。
     *
     * @param orgId   机构标识
     * @param request 创建请求
     * @return 创建结果
     */
    @Transactional
    public IdPayload create(String orgId, PeriodOpeningSaveRequest request) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureOperationPermission(scope, operatorId, "CREATE");
        PeriodRange range = resolvePeriodRange(request.periodType(), request.periodStartDate());
        ensureManualOpeningAllowed(scope, request.warehouseName(), range);
        InventoryPeriodOpeningDO header = buildHeader(scope, request, range, SOURCE_MANUAL, operatorId);
        List<InventoryPeriodOpeningLineDO> lines = normalizeRequestLines(request.items());
        applyTotals(header, lines);
        periodOpeningRepository.saveHeader(header);
        saveLines(header.getId(), lines);
        return new IdPayload(header.getId(), header.getDocumentCode());
    }

    /**
     * 从上一周期结存生成期初库存。
     *
     * @param orgId   机构标识
     * @param request 生成请求
     * @return 创建结果
     */
    @Transactional
    public IdPayload generate(String orgId, PeriodOpeningGenerateRequest request) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureOperationPermission(scope, operatorId, "CREATE");
        PeriodRange range = resolvePeriodRange(request.periodType(), request.periodStartDate());
        String warehouseName = requiredTrim(request.warehouseName(), "仓库不能为空");
        requirePreviousApprovedOpening(scope, warehouseName, range);
        ensurePeriodNotExists(scope, request.warehouseName(), range);
        ensureNoLaterTransactions(scope, warehouseName, range.startDate());
        InventoryPeriodOpeningDO header = buildGeneratedHeader(scope, request, range, operatorId);
        List<InventoryPeriodOpeningLineDO> lines = generateLinesFromBalance(scope, warehouseName);
        applyTotals(header, lines);
        periodOpeningRepository.saveHeader(header);
        saveLines(header.getId(), lines);
        return new IdPayload(header.getId(), header.getDocumentCode());
    }

    /**
     * 查询期初库存详情。
     *
     * @param orgId 机构标识
     * @param id    主键
     * @return 详情
     */
    public PeriodOpeningDetail detail(String orgId, Long id) {
        InventoryScope scope = resolveScope(orgId);
        ensureViewPermission(scope);
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        return toDetail(header, periodOpeningRepository.findLinesByHeaderId(header.getId()));
    }

    /**
     * 更新期初库存。
     *
     * @param orgId   机构标识
     * @param id      主键
     * @param request 更新请求
     */
    @Transactional
    public void update(String orgId, Long id, PeriodOpeningSaveRequest request) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureOperationPermission(scope, operatorId, "UPDATE");
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        ensureEditable(header);
        if (!SOURCE_MANUAL.equals(header.getSourceType())) {
            throw new BusinessException("自动生成的期初不允许手工修改明细");
        }
        PeriodRange range = resolvePeriodRange(request.periodType(), request.periodStartDate());
        ensurePeriodUniqueForUpdate(scope, id, request.warehouseName(), range);
        applyHeaderFields(header, request, range);
        List<InventoryPeriodOpeningLineDO> lines = normalizeRequestLines(request.items());
        applyTotals(header, lines);
        periodOpeningRepository.updateHeader(header);
        periodOpeningRepository.deleteLinesByHeaderId(header.getId());
        saveLines(header.getId(), lines);
    }

    /**
     * 删除期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     */
    @Transactional
    public void delete(String orgId, Long id) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureOperationPermission(scope, operatorId, "DELETE");
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        ensureEditable(header);
        inventoryDocumentWorkflowService.cancelWorkflowInstanceIfRunning(toWorkflowHeader(header));
        periodOpeningRepository.deleteLinesByHeaderId(id);
        periodOpeningRepository.deleteHeaderById(id);
    }

    /**
     * 提交期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     */
    @Transactional
    public void submit(String orgId, Long id) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureOperationPermission(scope, operatorId, "CREATE");
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        ensureEditable(header);
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        workflowHeader.setStatus(submittedStatus());
        boolean workflowApplied = inventoryDocumentWorkflowService.syncBusinessOnAction(
                BUSINESS_CODE,
                WORKFLOW_LABEL,
                scope.scopeType(),
                scope.scopeId(),
                workflowGroupId(scope),
                workflowHeader,
                operatorId,
                "CREATE",
                () -> {
                    applyWorkflowHeader(header, workflowHeader);
                    header.setStatus(submittedStatus());
                    periodOpeningRepository.updateHeader(header);
                },
                "期初库存删除",
                true
        );
        if (!workflowApplied) {
            throw new BusinessException(WORKFLOW_LABEL + "未发布，请先发布审批流");
        }
    }

    /**
     * 审批通过期初库存。
     *
     * @param orgId 机构标识
     * @param id    主键
     */
    @Transactional
    public void approve(String orgId, Long id) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureReviewPermission(scope, operatorId);
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        if (!Objects.equals(header.getStatus(), submittedStatus())) {
            throw new BusinessException("只有已提交期初可以审批");
        }
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        InventoryDocumentWorkflowService.ApprovalResult result = inventoryDocumentWorkflowService.completeBusinessCurrentTask(
                BUSINESS_CODE,
                WORKFLOW_LABEL,
                workflowHeader,
                operatorId,
                workflowGroupId(scope),
                () -> {
                    applyWorkflowHeader(header, workflowHeader);
                    periodOpeningRepository.updateHeader(header);
                }
        );
        applyWorkflowHeader(header, workflowHeader);
        if (!result.workflowApplied()) {
            throw new BusinessException(WORKFLOW_LABEL + "未启动，请先提交并完成审批流配置");
        }
        if (result.workflowApplied() && !result.completed()) {
            periodOpeningRepository.updateHeader(header);
            return;
        }
        confirmOpening(scope, header, operatorId);
    }

    /**
     * 驳回期初库存。
     *
     * @param orgId   机构标识
     * @param id      主键
     * @param request 驳回请求
     */
    @Transactional
    public void reject(String orgId, Long id, PeriodOpeningRejectRequest request) {
        InventoryScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        ensureReviewPermission(scope, operatorId);
        InventoryPeriodOpeningDO header = requireHeader(scope, id);
        if (!Objects.equals(header.getStatus(), submittedStatus())) {
            throw new BusinessException("只有已提交期初可以驳回");
        }
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        workflowHeader.setStatus(draftStatus());
        inventoryDocumentWorkflowService.resetBusinessWorkflowState(workflowHeader, () -> {
            applyWorkflowHeader(header, workflowHeader);
            header.setStatus(draftStatus());
            header.setRejectionReason(requiredTrim(request.rejectionReason(), "驳回原因不能为空"));
            periodOpeningRepository.updateHeader(header);
        }, "期初库存驳回");
    }

    private void confirmOpening(InventoryScope scope, InventoryPeriodOpeningDO header, Long operatorId) {
        ensureNoLaterTransactions(scope, header);
        List<InventoryPeriodOpeningLineDO> lines = periodOpeningRepository.findLinesByHeaderId(header.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("期初明细不能为空");
        }
        for (InventoryPeriodOpeningLineDO line : lines) {
            inventoryStockMutationService.applyAbsolute(
                    scope.scopeType(),
                    scope.scopeId(),
                    header.getWarehouseName(),
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getOpeningQty(),
                    line.getOpeningAmount(),
                    header.getPeriodStartDate(),
                    BUSINESS_CODE + "_CONFIRM",
                    operatorId
            );
        }
        header.setStatus(approvedStatus());
        header.setApprovedBy(operatorId);
        header.setApprovedAt(LocalDateTime.now());
        header.setPendingOperation(PENDING_NONE);
        periodOpeningRepository.updateHeader(header);
    }

    private InventoryPeriodOpeningDO buildHeader(InventoryScope scope,
                                                 PeriodOpeningSaveRequest request,
                                                 PeriodRange range,
                                                 String sourceType,
                                                 Long operatorId) {
        InventoryPeriodOpeningDO header = new InventoryPeriodOpeningDO();
        header.setScopeType(scope.scopeType());
        header.setScopeId(scope.scopeId());
        header.setDocumentCode(generateDocumentCode(scope));
        header.setWarehouseName(requiredTrim(request.warehouseName(), "仓库不能为空"));
        header.setPeriodType(range.periodType());
        header.setPeriodStartDate(range.startDate());
        header.setPeriodEndDate(range.endDate());
        header.setSourceType(sourceType);
        header.setStatus(draftStatus());
        header.setWorkflowStatus(noneWorkflowStatus());
        header.setPendingOperation(PENDING_NONE);
        header.setRemark(trimNullable(request.remark()));
        header.setCreatedBy(operatorId);
        return header;
    }

    private InventoryPeriodOpeningDO buildGeneratedHeader(InventoryScope scope,
                                                          PeriodOpeningGenerateRequest request,
                                                          PeriodRange range,
                                                          Long operatorId) {
        PeriodOpeningSaveRequest saveRequest = new PeriodOpeningSaveRequest(
                request.warehouseName(),
                request.periodType(),
                request.periodStartDate(),
                request.remark(),
                List.of()
        );
        return buildHeader(scope, saveRequest, range, SOURCE_GENERATED, operatorId);
    }

    private void applyHeaderFields(InventoryPeriodOpeningDO header,
                                   PeriodOpeningSaveRequest request,
                                   PeriodRange range) {
        header.setWarehouseName(requiredTrim(request.warehouseName(), "仓库不能为空"));
        header.setPeriodType(range.periodType());
        header.setPeriodStartDate(range.startDate());
        header.setPeriodEndDate(range.endDate());
        header.setRemark(trimNullable(request.remark()));
        header.setRejectionReason(null);
    }

    private List<InventoryPeriodOpeningLineDO> normalizeRequestLines(List<PeriodOpeningLineRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("期初明细不能为空");
        }
        return items.stream().map(this::toLine).toList();
    }

    private InventoryPeriodOpeningLineDO toLine(PeriodOpeningLineRequest request) {
        InventoryPeriodOpeningLineDO line = new InventoryPeriodOpeningLineDO();
        line.setItemCode(requiredTrim(request.itemCode(), "物品编码不能为空"));
        line.setItemName(requiredTrim(request.itemName(), "物品名称不能为空"));
        line.setSpec(trimNullable(request.spec()));
        line.setCategory(trimNullable(request.category()));
        line.setUnitName(trimNullable(request.unitName()));
        line.setOpeningQty(normalizeNonNegative(request.openingQty(), "期初数量不能小于0"));
        line.setOpeningAmount(normalizeMoney(request.openingAmount(), line.getOpeningQty()));
        line.setOpeningAvgCost(averageCost(line.getOpeningAmount(), line.getOpeningQty()));
        line.setRemark(trimNullable(request.remark()));
        return line;
    }

    private List<InventoryPeriodOpeningLineDO> generateLinesFromBalance(InventoryScope scope, String warehouseName) {
        List<InventoryBalanceDO> balances = inventoryBalanceRepository
                .findByScopeAndWarehouseOrdered(scope.scopeType(), scope.scopeId(), warehouseName);
        Map<String, ItemProfileDO> itemMap = itemProfileRepository.findByScopeAndItemCodes(
                        scope.scopeType(),
                        scope.scopeId(),
                        balances.stream().map(InventoryBalanceDO::getItemCode).toList()
                ).stream()
                .collect(Collectors.toMap(ItemProfileDO::getItemCode, Function.identity(), (left, right) -> left));
        List<InventoryPeriodOpeningLineDO> lines = balances.stream()
                .map(balance -> balanceToOpeningLine(balance, itemMap.get(balance.getItemCode())))
                .toList();
        if (lines.isEmpty()) {
            throw new BusinessException("上一周期结存为空，无法生成期初");
        }
        return lines;
    }

    private InventoryPeriodOpeningLineDO balanceToOpeningLine(InventoryBalanceDO balance, ItemProfileDO item) {
        InventoryPeriodOpeningLineDO line = new InventoryPeriodOpeningLineDO();
        BigDecimal quantity = normalizeGeneratedQuantity(balance.getQuantity());
        BigDecimal amount = normalizeGeneratedAmount(balance.getCostAmount(), quantity);
        line.setItemCode(balance.getItemCode());
        line.setItemName(balance.getItemName());
        applyItemSnapshot(line, item);
        line.setOpeningQty(quantity);
        line.setOpeningAmount(amount);
        line.setOpeningAvgCost(averageCost(amount, quantity));
        return line;
    }

    private void applyItemSnapshot(InventoryPeriodOpeningLineDO line, ItemProfileDO item) {
        if (item == null || !StringUtils.hasText(item.getDetailJson())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(item.getDetailJson());
            line.setSpec(textOf(root, "spec"));
            line.setCategory(textOf(root, "category"));
            line.setUnitName(textOf(root, "defaultStockUnit"));
        } catch (Exception ex) {
            throw new BusinessException("物品档案快照解析失败");
        }
    }

    private void saveLines(Long headerId, List<InventoryPeriodOpeningLineDO> lines) {
        for (InventoryPeriodOpeningLineDO line : lines) {
            line.setHeaderId(headerId);
            periodOpeningRepository.saveLine(line);
        }
    }

    private void applyTotals(InventoryPeriodOpeningDO header, List<InventoryPeriodOpeningLineDO> lines) {
        header.setTotalQuantity(lines.stream()
                .map(InventoryPeriodOpeningLineDO::getOpeningQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        header.setTotalAmount(lines.stream()
                .map(InventoryPeriodOpeningLineDO::getOpeningAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private boolean matchesHeader(InventoryPeriodOpeningDO header,
                                  String documentCode,
                                  String warehouseName,
                                  String periodType,
                                  LocalDate start,
                                  LocalDate end,
                                  String status) {
        return matchesOptionalText(toLower(header.getDocumentCode()), documentCode)
                && matchesOptionalEquals(header.getWarehouseName(), warehouseName)
                && matchesOptionalEquals(header.getPeriodType(), periodType)
                && matchesDateRange(header.getPeriodStartDate(), start, end)
                && matchesOptionalEquals(header.getStatus(), status);
    }

    private boolean matchesOptionalText(String actual, String keyword) {
        return !StringUtils.hasText(keyword) || defaultIfBlank(actual, "").contains(keyword);
    }

    private boolean matchesOptionalEquals(String actual, String expected) {
        return !StringUtils.hasText(expected) || Objects.equals(actual, expected);
    }

    private boolean matchesDateRange(LocalDate actual, LocalDate start, LocalDate end) {
        return (start == null || !actual.isBefore(start))
                && (end == null || !actual.isAfter(end));
    }

    private PeriodOpeningRow toRow(InventoryPeriodOpeningDO header) {
        return new PeriodOpeningRow(
                header.getId(),
                header.getDocumentCode(),
                header.getWarehouseName(),
                header.getPeriodType(),
                header.getPeriodStartDate().toString(),
                header.getPeriodEndDate().toString(),
                header.getSourceType(),
                header.getStatus(),
                defaultQuantity(header.getTotalQuantity()),
                defaultMoney(header.getTotalAmount()),
                defaultIfBlank(header.getRemark(), ""),
                formatDateTime(header.getCreatedAt()),
                formatDateTime(header.getApprovedAt())
        );
    }

    private Long workflowGroupId(InventoryScope scope) {
        return OrgScopeService.SCOPE_GROUP.equals(scope.scopeType()) ? scope.scopeId() : scope.groupId();
    }

    private PeriodOpeningDetail toDetail(InventoryPeriodOpeningDO header, List<InventoryPeriodOpeningLineDO> lines) {
        return new PeriodOpeningDetail(
                header.getId(),
                header.getDocumentCode(),
                header.getWarehouseName(),
                header.getPeriodType(),
                header.getPeriodStartDate().toString(),
                header.getPeriodEndDate().toString(),
                header.getSourceType(),
                header.getStatus(),
                defaultQuantity(header.getTotalQuantity()),
                defaultMoney(header.getTotalAmount()),
                defaultIfBlank(header.getRemark(), ""),
                defaultIfBlank(header.getRejectionReason(), ""),
                userDisplay(header.getCreatedBy()),
                formatDateTime(header.getCreatedAt()),
                userDisplay(header.getApprovedBy()),
                formatDateTime(header.getApprovedAt()),
                lines.stream().map(this::toLineView).toList()
        );
    }

    private PeriodOpeningLineView toLineView(InventoryPeriodOpeningLineDO line) {
        return new PeriodOpeningLineView(
                line.getId(),
                line.getItemCode(),
                line.getItemName(),
                defaultIfBlank(line.getSpec(), ""),
                defaultIfBlank(line.getCategory(), ""),
                defaultIfBlank(line.getUnitName(), ""),
                defaultQuantity(line.getOpeningQty()),
                defaultMoney(line.getOpeningAmount()),
                defaultMoney(line.getOpeningAvgCost()),
                defaultIfBlank(line.getRemark(), "")
        );
    }

    private void ensureManualOpeningAllowed(InventoryScope scope, String warehouseName, PeriodRange range) {
        ensurePeriodNotExists(scope, warehouseName, range);
        String warehouseValue = requiredTrim(warehouseName, "仓库不能为空");
        boolean hasEarlierOpening = periodOpeningRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(header -> Objects.equals(header.getWarehouseName(), warehouseValue)
                        && Objects.equals(header.getPeriodType(), range.periodType())
                        && header.getPeriodStartDate().isBefore(range.startDate()));
        if (hasEarlierOpening) {
            throw new BusinessException("后续周期只能从上一周期结存生成期初");
        }
    }

    private void ensurePeriodNotExists(InventoryScope scope, String warehouseName, PeriodRange range) {
        periodOpeningRepository.findByPeriod(
                scope.scopeType(),
                scope.scopeId(),
                requiredTrim(warehouseName, "仓库不能为空"),
                range.periodType(),
                range.startDate()
        ).ifPresent(existing -> {
            throw new BusinessException("该仓库当前周期已存在期初库存");
        });
    }

    private void ensurePeriodUniqueForUpdate(InventoryScope scope, Long currentId, String warehouseName, PeriodRange range) {
        periodOpeningRepository.findByPeriod(
                scope.scopeType(),
                scope.scopeId(),
                requiredTrim(warehouseName, "仓库不能为空"),
                range.periodType(),
                range.startDate()
        ).filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new BusinessException("该仓库当前周期已存在期初库存");
                });
    }

    private InventoryPeriodOpeningDO requirePreviousApprovedOpening(InventoryScope scope, String warehouseName, PeriodRange range) {
        return periodOpeningRepository.findByPeriod(
                scope.scopeType(),
                scope.scopeId(),
                requiredTrim(warehouseName, "仓库不能为空"),
                range.periodType(),
                previousPeriodStart(range)
        ).filter(header -> Objects.equals(header.getStatus(), approvedStatus()))
                .orElseThrow(() -> new BusinessException("上一周期没有已确认期初，无法生成"));
    }

    private void ensureNoLaterTransactions(InventoryScope scope, InventoryPeriodOpeningDO header) {
        ensureNoLaterTransactions(scope, header.getWarehouseName(), header.getPeriodStartDate());
    }

    private void ensureNoLaterTransactions(InventoryScope scope, String warehouseName, LocalDate businessDate) {
        boolean exists = inventoryTransactionRepository.existsByScopeWarehouseAndBusinessDateOnOrAfter(
                scope.scopeType(),
                scope.scopeId(),
                warehouseName,
                businessDate
        );
        if (exists) {
            throw new BusinessException("当前仓库已有本周期或之后库存流水，请清理后重做期初");
        }
    }

    private void ensureEditable(InventoryPeriodOpeningDO header) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
            throw new BusinessException("已确认期初不支持修改或删除");
        }
    }

    private InventoryPeriodOpeningDO requireHeader(InventoryScope scope, Long id) {
        if (id == null) {
            throw new BusinessException("期初库存ID不能为空");
        }
        return periodOpeningRepository.findByScopeAndId(scope.scopeType(), scope.scopeId(), id)
                .orElseThrow(() -> new BusinessException("期初库存不存在"));
    }

    private InventoryDocumentHeader toWorkflowHeader(InventoryPeriodOpeningDO header) {
        InventoryDocumentHeader workflowHeader = new InventoryDocumentHeader();
        workflowHeader.setId(header.getId());
        workflowHeader.setScopeType(header.getScopeType());
        workflowHeader.setScopeId(header.getScopeId());
        workflowHeader.setDocumentCode(header.getDocumentCode());
        workflowHeader.setDocumentDate(header.getPeriodStartDate());
        workflowHeader.setPrimaryName(header.getWarehouseName());
        workflowHeader.setTotalAmount(header.getTotalAmount());
        workflowHeader.setStatus(header.getStatus());
        workflowHeader.setWorkflowProcessCode(header.getWorkflowProcessCode());
        workflowHeader.setWorkflowDefinitionKey(header.getWorkflowDefinitionKey());
        workflowHeader.setWorkflowDefinitionId(header.getWorkflowDefinitionId());
        workflowHeader.setWorkflowInstanceId(header.getWorkflowInstanceId());
        workflowHeader.setWorkflowTaskId(header.getWorkflowTaskId());
        workflowHeader.setWorkflowTaskName(header.getWorkflowTaskName());
        workflowHeader.setWorkflowStatus(header.getWorkflowStatus());
        workflowHeader.setPendingOperation(header.getPendingOperation());
        workflowHeader.setRemark(header.getRemark());
        workflowHeader.setRejectionReason(header.getRejectionReason());
        workflowHeader.setCreatedBy(header.getCreatedBy());
        workflowHeader.setApprovedBy(header.getApprovedBy());
        workflowHeader.setApprovedAt(header.getApprovedAt());
        workflowHeader.setCreatedAt(header.getCreatedAt());
        workflowHeader.setUpdatedAt(header.getUpdatedAt());
        return workflowHeader;
    }

    private void applyWorkflowHeader(InventoryPeriodOpeningDO header, InventoryDocumentHeader workflowHeader) {
        header.setWorkflowProcessCode(workflowHeader.getWorkflowProcessCode());
        header.setWorkflowDefinitionKey(workflowHeader.getWorkflowDefinitionKey());
        header.setWorkflowDefinitionId(workflowHeader.getWorkflowDefinitionId());
        header.setWorkflowInstanceId(workflowHeader.getWorkflowInstanceId());
        header.setWorkflowTaskId(workflowHeader.getWorkflowTaskId());
        header.setWorkflowTaskName(workflowHeader.getWorkflowTaskName());
        header.setWorkflowStatus(workflowHeader.getWorkflowStatus());
        header.setPendingOperation(workflowHeader.getPendingOperation());
    }

    private void ensureOperationPermission(InventoryScope scope, Long operatorId, String action) {
        if (dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)) {
            return;
        }
        if (!inventoryDocumentWorkflowService.hasBusinessOperationPermission(
                BUSINESS_CODE, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, action)) {
            throw new BusinessException("当前账号无期初库存操作权限");
        }
    }

    private void ensureReviewPermission(InventoryScope scope, Long operatorId) {
        if (dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)) {
            return;
        }
        if (!inventoryDocumentWorkflowService.hasBusinessReviewPermission(BUSINESS_CODE, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)) {
            throw new BusinessException("当前账号无期初库存审核权限");
        }
    }

    private void ensureViewPermission(InventoryScope scope) {
        AuthContextHolder.requireUserId("登录已失效，请重新登录");
        if (!isScopeSupported(scope.scopeType())) {
            throw new BusinessException("期初库存仅支持集团或门店作用域");
        }
    }

    private InventoryScope resolveScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(
                AuthContextHolder.requireUserId("登录已失效，请重新登录"),
                orgId
        );
        if (!isScopeSupported(scope.scopeType())) {
            throw new BusinessException("期初库存仅支持集团或门店作用域");
        }
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private boolean isScopeSupported(String scopeType) {
        return OrgScopeService.SCOPE_GROUP.equals(scopeType) || OrgScopeService.SCOPE_STORE.equals(scopeType);
    }

    private String normalizePeriodTypeNullable(String periodType) {
        String normalized = trimNullable(periodType);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalizePeriodType(normalized);
    }

    private String normalizePeriodType(String periodType) {
        String normalized = requiredTrim(periodType, "周期类型不能为空").toUpperCase(Locale.ROOT);
        dictionaryLookupService.requireEnabledCode(DictionaryCodes.INVENTORY_PERIOD_TYPE, normalized);
        return normalized;
    }

    private PeriodRange resolvePeriodRange(String periodType, String periodStartDate) {
        String normalizedType = normalizePeriodType(periodType);
        LocalDate input = parseRequiredDate(periodStartDate, "周期开始日期格式不正确");
        LocalDate start;
        LocalDate end;
        if ("DAY".equals(normalizedType)) {
            start = input;
            end = input;
        } else if ("MONTH".equals(normalizedType)) {
            start = input.withDayOfMonth(1);
            end = input.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("YEAR".equals(normalizedType)) {
            start = input.withDayOfYear(1);
            end = input.with(TemporalAdjusters.lastDayOfYear());
        } else {
            throw new BusinessException("不支持的周期类型");
        }
        return new PeriodRange(normalizedType, start, end);
    }

    private LocalDate previousPeriodStart(PeriodRange range) {
        if ("DAY".equals(range.periodType())) {
            return range.startDate().minusDays(1);
        }
        if ("MONTH".equals(range.periodType())) {
            return range.startDate().minusMonths(1);
        }
        return range.startDate().minusYears(1);
    }

    private String generateDocumentCode(InventoryScope scope) {
        String datePart = LocalDate.now().toString().replace("-", "");
        for (int i = 0; i < DOCUMENT_CODE_MAX_ATTEMPTS; i++) {
            int rand = ThreadLocalRandom.current().nextInt(DOCUMENT_CODE_RANDOM_MIN, DOCUMENT_CODE_RANDOM_MAX);
            String code = DOCUMENT_PREFIX + "-" + datePart + "-" + rand;
            boolean exists = periodOpeningRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                    .anyMatch(header -> Objects.equals(header.getDocumentCode(), code));
            if (!exists) {
                return code;
            }
        }
        throw new BusinessException("生成期初单号失败，请重试");
    }

    private BigDecimal normalizeNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeGeneratedQuantity(BigDecimal value) {
        return normalizeNonNegative(value, "结存数量不能小于0");
    }

    private BigDecimal normalizeGeneratedAmount(BigDecimal amount, BigDecimal quantity) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("结存金额不能小于0");
        }
        if (quantity.compareTo(BigDecimal.ZERO) == 0 && value.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("结存数量为0时金额必须为0");
        }
        if (quantity.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("结存数量大于0时金额必须大于0");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal amount, BigDecimal quantity) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("期初金额不能小于0");
        }
        if (quantity.compareTo(BigDecimal.ZERO) == 0 && value.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("期初数量为0时金额必须为0");
        }
        if (quantity.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("期初数量大于0时金额必须大于0");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal averageCost(BigDecimal amount, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return amount.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private LocalDate parseRequiredDate(String value, String message) {
        String normalized = requiredTrim(value, message);
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(message);
        }
    }

    private LocalDate parseDateNullable(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(message);
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? PAGE_SIZE_DEFAULT : Math.min(pageSize, PAGE_SIZE_MAX);
    }

    private String draftStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.DRAFT);
    }

    private String submittedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.SUBMITTED);
    }

    private String approvedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.APPROVED);
    }

    private String noneWorkflowStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.NONE);
    }

    private String userDisplay(Long userId) {
        if (userId == null) {
            return "";
        }
        return String.valueOf(userId);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String textOf(JsonNode root, String fieldName) {
        JsonNode node = root == null ? null : root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return trimNullable(node.asText());
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP) : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : value;
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }

    private record PeriodRange(String periodType, LocalDate startDate, LocalDate endDate) {
    }

    /** 周期期初库存保存请求。 */
    public record PeriodOpeningSaveRequest(String warehouseName,
                                           String periodType,
                                           String periodStartDate,
                                           String remark,
                                           List<PeriodOpeningLineRequest> items) {
    }

    /** 周期期初库存生成请求。 */
    public record PeriodOpeningGenerateRequest(String warehouseName,
                                               String periodType,
                                               String periodStartDate,
                                               String remark) {
    }

    /** 周期期初库存驳回请求。 */
    public record PeriodOpeningRejectRequest(String rejectionReason) {
    }

    /** 周期期初库存明细请求。 */
    public record PeriodOpeningLineRequest(String itemCode,
                                           String itemName,
                                           String spec,
                                           String category,
                                           String unitName,
                                           BigDecimal openingQty,
                                           BigDecimal openingAmount,
                                           String remark) {
    }

    /** 期初库存创建结果。 */
    public record IdPayload(Long id, String documentCode) {
    }

    /** 期初库存分页结果。 */
    public record PeriodOpeningPage(List<PeriodOpeningRow> list, long total, int pageNum, int pageSize) {
    }

    /** 期初库存列表行。 */
    public record PeriodOpeningRow(Long id,
                                   String documentCode,
                                   String warehouseName,
                                   String periodType,
                                   String periodStartDate,
                                   String periodEndDate,
                                   String sourceType,
                                   String status,
                                   BigDecimal totalQuantity,
                                   BigDecimal totalAmount,
                                   String remark,
                                   String createdAt,
                                   String approvedAt) {
    }

    /** 期初库存详情。 */
    public record PeriodOpeningDetail(Long id,
                                      String documentCode,
                                      String warehouseName,
                                      String periodType,
                                      String periodStartDate,
                                      String periodEndDate,
                                      String sourceType,
                                      String status,
                                      BigDecimal totalQuantity,
                                      BigDecimal totalAmount,
                                      String remark,
                                      String rejectionReason,
                                      String creator,
                                      String createdAt,
                                      String auditor,
                                      String approvedAt,
                                      List<PeriodOpeningLineView> items) {
    }

    /** 期初库存详情明细。 */
    public record PeriodOpeningLineView(Long id,
                                        String itemCode,
                                        String itemName,
                                        String spec,
                                        String category,
                                        String unitName,
                                        BigDecimal openingQty,
                                        BigDecimal openingAmount,
                                        BigDecimal openingAvgCost,
                                        String remark) {
    }
}
