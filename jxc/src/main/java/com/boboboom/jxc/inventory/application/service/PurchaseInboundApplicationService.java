package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundCreateRequest;
import com.boboboom.jxc.workflow.application.service.PurchaseInboundWorkflowService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PurchaseInboundApplicationService {

    private static final String STATUS_DRAFT = "草稿";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_APPROVED = "已审核";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundLineRepository purchaseInboundLineRepository;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final PurchaseInboundPermissionService purchaseInboundPermissionService;
    private final PurchaseInboundNotificationService purchaseInboundNotificationService;
    private final PurchaseInboundUnapproveService purchaseInboundUnapproveService;
    private final PurchaseInboundWorkflowService purchaseInboundWorkflowService;
    private final OrgScopeService orgScopeService;

    public PurchaseInboundApplicationService(InventoryBalanceRepository inventoryBalanceRepository,
                                             PurchaseInboundRepository purchaseInboundRepository,
                                             PurchaseInboundLineRepository purchaseInboundLineRepository,
                                             InventoryStockMutationService inventoryStockMutationService,
                                             PurchaseInboundPermissionService purchaseInboundPermissionService,
                                             PurchaseInboundNotificationService purchaseInboundNotificationService,
                                             PurchaseInboundUnapproveService purchaseInboundUnapproveService,
                                             PurchaseInboundWorkflowService purchaseInboundWorkflowService,
                                             OrgScopeService orgScopeService) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.purchaseInboundRepository = purchaseInboundRepository;
        this.purchaseInboundLineRepository = purchaseInboundLineRepository;
        this.inventoryStockMutationService = inventoryStockMutationService;
        this.purchaseInboundPermissionService = purchaseInboundPermissionService;
        this.purchaseInboundNotificationService = purchaseInboundNotificationService;
        this.purchaseInboundUnapproveService = purchaseInboundUnapproveService;
        this.purchaseInboundWorkflowService = purchaseInboundWorkflowService;
        this.orgScopeService = orgScopeService;
    }

    public PageData<PurchaseInboundRow> listPurchaseInbound(Integer pageNo,
                                                            Integer pageSize,
                                                            String timeType,
                                                            String startDate,
                                                            String endDate,
                                                            String warehouse,
                                                            String documentCode,
                                                            String supplier,
                                                            String itemName,
                                                            String documentStatus,
                                                            String reviewStatus,
                                                            String reconciliationStatus,
                                                            String splitStatus,
                                                            String upstreamCode,
                                                            String invoiceStatus,
                                                            String inspectionCount,
                                                            String printStatus,
                                                            String remark,
                                                            String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long currentUserId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        PurchaseInboundListAccess access = resolveListAccess(scope, currentUserId);
        PurchaseInboundListQuery query = buildListQuery(
                pageNo, pageSize, timeType, startDate, endDate, warehouse, documentCode, supplier,
                itemName, documentStatus, reviewStatus, reconciliationStatus, splitStatus, upstreamCode,
                invoiceStatus, inspectionCount, printStatus, remark
        );
        List<PurchaseInboundDO> headers = loadVisibleHeaders(scope, access, currentUserId);
        if (headers.isEmpty()) {
            return emptyPurchaseInboundPage(query);
        }
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(headers.stream().map(PurchaseInboundDO::getId).toList());
        String currentUserName = AuthContextHolder.userNameOr("未知用户");
        List<PurchaseInboundRow> filtered = headers.stream()
                .filter(header -> matchListQuery(header, lineMap.getOrDefault(header.getId(), List.of()), query))
                .map(header -> toPurchaseInboundRow(
                        header,
                        lineMap.getOrDefault(header.getId(), List.of()),
                        currentUserId,
                        currentUserName
                ))
                .toList();
        return paginatePurchaseInboundRows(filtered, query.pageNo(), query.pageSize());
    }

    public PurchaseInboundPermissionView purchaseInboundPermissions(String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        PurchaseInboundPermissionService.PermissionSnapshot permissionSnapshot =
                purchaseInboundPermissionService.resolvePermissions(
                        scope.scopeType(),
                        scope.scopeId(),
                        scope.groupId(),
                        operatorId
                );
        return new PurchaseInboundPermissionView(
                permissionSnapshot.canCreate(),
                permissionSnapshot.canUpdate(),
                permissionSnapshot.canDelete(),
                permissionSnapshot.canApprove(),
                permissionSnapshot.canUnapprove()
        );
    }

    @Transactional
    public IdPayload createPurchaseInbound(String orgId, PurchaseInboundCreateRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundOperationPermission(scope, "CREATE");
        PurchaseInboundDO header = savePurchaseInbound(scope, null, request, true, "CREATE");
        return new IdPayload(header.getId(), header.getDocumentCode());
    }

    public PurchaseInboundDetail detailPurchaseInbound(Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        PurchaseInboundDO header = requireHeader(scope, id, operatorId, true);
        List<PurchaseInboundLineDO> lines = purchaseInboundLineRepository.findByInboundId(header.getId());
        return new PurchaseInboundDetail(
                header.getId(),
                header.getDocumentCode(),
                defaultIfBlank(header.getStatus(), STATUS_DRAFT),
                header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                defaultIfBlank(header.getWarehouseName(), ""),
                defaultIfBlank(header.getSupplierName(), ""),
                header.getSalesmanUserId(),
                defaultIfBlank(header.getSalesmanName(), ""),
                defaultIfBlank(header.getUpstreamCode(), ""),
                defaultIfBlank(header.getRemark(), ""),
                defaultIfBlank(header.getRejectionReason(), ""),
                defaultIfBlank(header.getWorkflowProcessCode(), ""),
                defaultIfBlank(header.getWorkflowDefinitionKey(), ""),
                defaultIfBlank(header.getWorkflowDefinitionId(), ""),
                defaultIfBlank(header.getWorkflowInstanceId(), ""),
                defaultIfBlank(header.getWorkflowTaskId(), ""),
                defaultIfBlank(header.getWorkflowTaskName(), ""),
                defaultIfBlank(header.getWorkflowStatus(), "NONE"),
                lines.stream()
                        .map(line -> new PurchaseInboundDetailLine(
                                line.getItemCode(),
                                line.getItemName(),
                                line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity(),
                                line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice(),
                                line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate()
                        ))
                        .toList()
        );
    }

    @Transactional
    public void updatePurchaseInbound(Long id, String orgId, PurchaseInboundCreateRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundOperationPermission(scope, "UPDATE");
        savePurchaseInbound(scope, id, request, false, "UPDATE");
    }

    @Transactional
    public void deletePurchaseInbound(Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundOperationPermission(scope, "DELETE");
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        PurchaseInboundDO header = requireHeader(scope, id, operatorId);
        if (purchaseInboundWorkflowService.shouldTriggerAction(scope.scopeType(), scope.scopeId(), scope.groupId(), "DELETE")) {
            if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                throw new BusinessException("已审核单据请先反审核后再删除");
            }
            saveDeleteWorkflow(scope, header, operatorId);
            return;
        }
        deletePurchaseInboundInternal(header);
    }

    @Transactional
    public void batchDeletePurchaseInbound(String orgId, PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundOperationPermission(scope, "DELETE");
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids(), operatorId);
        for (PurchaseInboundDO header : headers) {
            if (purchaseInboundWorkflowService.shouldTriggerAction(scope.scopeType(), scope.scopeId(), scope.groupId(), "DELETE")) {
                if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                    throw new BusinessException("已审核单据请先反审核后再删除");
                }
                saveDeleteWorkflow(scope, header, operatorId);
                continue;
            }
            deletePurchaseInboundInternal(header);
        }
    }

    @Transactional
    public void batchApprove(String orgId, PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundReviewPermission(scope);
        BatchApproveContext context = buildBatchApproveContext(scope, request);
        for (PurchaseInboundDO header : context.headers()) {
            approveHeader(scope, header, context.lineMap().getOrDefault(header.getId(), List.of()), context);
        }
    }

    @Transactional
    public void batchUnapprove(String orgId, PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundReviewPermission(scope);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String rejectionReason = requiredTrim(request.rejectionReason(), "拒审原因不能为空");
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids(), operatorId, true);
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(request.ids());

        for (PurchaseInboundDO header : headers) {
            purchaseInboundUnapproveService.execute(
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.groupId(),
                    header,
                    lineMap.getOrDefault(header.getId(), List.of()),
                    operatorId,
                    rejectionReason
            );
        }
    }

    public PageData<InventoryBalanceRow> listBalances(Integer pageNum, Integer pageSize, String warehouse, String itemName, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        String warehouseValue = trimNullable(warehouse);
        String itemKeyword = toLower(trimNullable(itemName));
        List<InventoryBalanceRow> filtered = inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())
                .stream()
                .filter(row -> !StringUtils.hasText(warehouseValue) || Objects.equals(row.getWarehouseName(), warehouseValue))
                .filter(row -> !StringUtils.hasText(itemKeyword)
                        || toLower(row.getItemName()).contains(itemKeyword)
                        || toLower(row.getItemCode()).contains(itemKeyword))
                .map(row -> new InventoryBalanceRow(
                        row.getWarehouseName(),
                        row.getItemCode(),
                        row.getItemName(),
                        row.getQuantity() == null ? "0" : row.getQuantity().stripTrailingZeros().toPlainString(),
                        formatDateTime(row.getUpdatedAt())
                ))
                .toList();
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        return new PageData<>(filtered.subList(fromIndex, toIndex), filtered.size(), safePageNum, safePageSize);
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope, List<Long> ids) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return requireHeaders(scope, ids, operatorId);
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope, List<Long> ids, Long operatorId) {
        return requireHeaders(scope, ids, operatorId, false);
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope,
                                                     List<Long> ids,
                                                     Long operatorId,
                                                     boolean allowReviewAccess) {
        Set<Long> idSet = new LinkedHashSet<>(ids);
        if (idSet.isEmpty()) {
            throw new BusinessException("单据ID不能为空");
        }
        boolean viewAll = purchaseInboundPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)
                || allowReviewAccess && purchaseInboundPermissionService.canReview(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<PurchaseInboundDO> headers = purchaseInboundRepository.findByScopeAndIds(scope.scopeType(), scope.scopeId(), operatorId, viewAll, new java.util.ArrayList<>(idSet));
        if (headers.size() != idSet.size()) {
            throw new BusinessException("存在无效单据");
        }
        return headers;
    }

    private PurchaseInboundDO requireHeader(InventoryScope scope, Long id) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return requireHeader(scope, id, operatorId);
    }

    private PurchaseInboundDO requireHeader(InventoryScope scope, Long id, Long operatorId) {
        return requireHeader(scope, id, operatorId, false);
    }

    private PurchaseInboundDO requireHeader(InventoryScope scope, Long id, Long operatorId, boolean allowReviewAccess) {
        boolean viewAll = purchaseInboundPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)
                || allowReviewAccess && purchaseInboundPermissionService.canReview(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        PurchaseInboundDO header = purchaseInboundRepository.findByScopeAndId(scope.scopeType(), scope.scopeId(), operatorId, viewAll, id)
                .orElse(null);
        if (header == null) {
            throw new BusinessException("单据不存在");
        }
        return header;
    }

    private PurchaseInboundDO savePurchaseInbound(InventoryScope scope,
                                                  Long id,
                                                  PurchaseInboundCreateRequest request,
                                                  boolean createMode,
                                                  String action) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        PurchaseInboundDO header = buildPurchaseInboundHeader(scope, id, request, createMode, operatorId);
        persistPurchaseInbound(scope, header, request, createMode);
        boolean workflowApplied = purchaseInboundWorkflowService.syncOnAction(scope.scopeType(), scope.scopeId(), scope.groupId(), header, operatorId, action);
        if (!workflowApplied) {
            header.setPendingOperation("NONE");
            purchaseInboundRepository.update(header);
            return header;
        }
        purchaseInboundNotificationService.recordSubmit(scope.scopeType(), scope.scopeId(), scope.groupId(), header);
        return header;
    }

    private PurchaseInboundDO buildPurchaseInboundHeader(InventoryScope scope,
                                                         Long id,
                                                         PurchaseInboundCreateRequest request,
                                                         boolean createMode,
                                                         Long operatorId) {
        PurchaseInboundDO header = createMode
                ? new PurchaseInboundDO()
                : requireHeader(scope, id, operatorId);
        if (!createMode && Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException("已审核单据不允许编辑，请先反审核");
        }
        initializeCreatedHeader(scope, header, createMode, operatorId);
        validateSalesmanSelection(scope, request.salesmanUserId(), createMode, operatorId);
        applyPurchaseInboundFields(header, request, createMode);
        return header;
    }

    private void initializeCreatedHeader(InventoryScope scope,
                                         PurchaseInboundDO header,
                                         boolean createMode,
                                         Long operatorId) {
        if (!createMode) {
            return;
        }
        header.setScopeType(scope.scopeType());
        header.setScopeId(scope.scopeId());
        header.setDocumentCode(generateDocumentCode(scope));
        header.setCreatedBy(operatorId);
    }

    private void validateSalesmanSelection(InventoryScope scope,
                                           Long salesmanUserId,
                                           boolean createMode,
                                           Long operatorId) {
        if (createMode && salesmanUserId == null) {
            throw new BusinessException("业务员不能为空");
        }
        if (purchaseInboundPermissionService.isStoreSalesmanOperator(scope.scopeType(), scope.scopeId(), operatorId)
                && !Objects.equals(salesmanUserId, operatorId)) {
            throw new BusinessException("业务员只能选择本人");
        }
    }

    private void applyPurchaseInboundFields(PurchaseInboundDO header,
                                            PurchaseInboundCreateRequest request,
                                            boolean createMode) {
        header.setInboundDate(parseRequiredDate(request.inboundDate(), "入库日期格式不正确"));
        header.setWarehouseName(requiredTrim(request.warehouse(), "仓库不能为空"));
        header.setSupplierName(requiredTrim(request.supplier(), "供应商不能为空"));
        header.setSalesmanUserId(request.salesmanUserId());
        header.setSalesmanName(trimNullable(request.salesmanName()));
        header.setUpstreamCode(trimNullable(request.upstreamCode()));
        header.setRemark(trimNullable(request.remark()));
        header.setStatus(createMode || !StringUtils.hasText(header.getStatus()) ? STATUS_SUBMITTED : header.getStatus());
    }

    private void persistPurchaseInbound(InventoryScope scope,
                                        PurchaseInboundDO header,
                                        PurchaseInboundCreateRequest request,
                                        boolean createMode) {
        persistPurchaseInboundHeader(header, createMode);
        replacePurchaseInboundLines(header, request.items());
    }

    private void persistPurchaseInboundHeader(PurchaseInboundDO header, boolean createMode) {
        if (createMode) {
            purchaseInboundRepository.save(header);
            return;
        }
        purchaseInboundRepository.update(header);
        purchaseInboundLineRepository.deleteByInboundId(header.getId());
    }

    private void replacePurchaseInboundLines(PurchaseInboundDO header,
                                             List<PurchaseInboundCreateRequest.LineItem> items) {
        for (PurchaseInboundCreateRequest.LineItem item : items) {
            purchaseInboundLineRepository.save(buildPurchaseInboundLine(header.getId(), item));
        }
    }

    private PurchaseInboundLineDO buildPurchaseInboundLine(Long headerId,
                                                           PurchaseInboundCreateRequest.LineItem item) {
        PurchaseInboundLineDO line = new PurchaseInboundLineDO();
        line.setInboundId(headerId);
        line.setItemCode(requiredTrim(item.itemCode(), "物品编码不能为空"));
        line.setItemName(requiredTrim(item.itemName(), "物品名称不能为空"));
        line.setQuantity(normalizePositive(item.quantity(), "数量必须大于0"));
        line.setUnitPrice(normalizeNonNegative(item.unitPrice(), "单价不能小于0"));
        line.setTaxRate(normalizeNonNegative(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate(), "税率不能小于0"));
        return line;
    }

    private Map<Long, List<PurchaseInboundLineDO>> loadLineMap(List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Map.of();
        }
        return purchaseInboundLineRepository.findByInboundIds(headerIds)
                .stream()
                .collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));
    }

    private PurchaseInboundListAccess resolveListAccess(InventoryScope scope, Long currentUserId) {
        return new PurchaseInboundListAccess(
                purchaseInboundPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), currentUserId),
                purchaseInboundPermissionService.canReview(scope.scopeType(), scope.scopeId(), scope.groupId(), currentUserId)
        );
    }

    private PurchaseInboundListQuery buildListQuery(Integer pageNo,
                                                    Integer pageSize,
                                                    String timeType,
                                                    String startDate,
                                                    String endDate,
                                                    String warehouse,
                                                    String documentCode,
                                                    String supplier,
                                                    String itemName,
                                                    String documentStatus,
                                                    String reviewStatus,
                                                    String reconciliationStatus,
                                                    String splitStatus,
                                                    String upstreamCode,
                                                    String invoiceStatus,
                                                    String inspectionCount,
                                                    String printStatus,
                                                    String remark) {
        return new PurchaseInboundListQuery(
                pageNo == null || pageNo < 1 ? 1 : pageNo,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200),
                trimNullable(timeType),
                parseDateNullable(startDate, "开始日期格式不正确"),
                parseDateNullable(endDate, "结束日期格式不正确"),
                trimNullable(warehouse),
                toLower(trimNullable(documentCode)),
                trimNullable(supplier),
                toLower(trimNullable(itemName)),
                trimNullable(documentStatus),
                trimNullable(reviewStatus),
                trimNullable(reconciliationStatus),
                trimNullable(splitStatus),
                toLower(trimNullable(upstreamCode)),
                trimNullable(invoiceStatus),
                trimNullable(inspectionCount),
                trimNullable(printStatus),
                toLower(trimNullable(remark))
        );
    }

    private List<PurchaseInboundDO> loadVisibleHeaders(InventoryScope scope, PurchaseInboundListAccess access, Long currentUserId) {
        return purchaseInboundRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> access.viewAll() || access.canReview() || Objects.equals(header.getCreatedBy(), currentUserId))
                .toList();
    }

    private PageData<PurchaseInboundRow> emptyPurchaseInboundPage(PurchaseInboundListQuery query) {
        return new PageData<>(List.of(), 0, query.pageNo(), query.pageSize());
    }

    private boolean matchListQuery(PurchaseInboundDO header, List<PurchaseInboundLineDO> lines, PurchaseInboundListQuery query) {
        return matchDate(header, query.timeType(), query.startDate(), query.endDate())
                && (!StringUtils.hasText(query.warehouse()) || Objects.equals(header.getWarehouseName(), query.warehouse()))
                && (!StringUtils.hasText(query.documentCode()) || toLower(header.getDocumentCode()).contains(query.documentCode()))
                && (!StringUtils.hasText(query.supplier()) || Objects.equals(header.getSupplierName(), query.supplier()))
                && (!StringUtils.hasText(query.documentStatus()) || Objects.equals(header.getStatus(), query.documentStatus()))
                && (!StringUtils.hasText(query.reviewStatus()) || Objects.equals(deriveReviewStatus(header), query.reviewStatus()))
                && (!StringUtils.hasText(query.reconciliationStatus()) || Objects.equals(deriveReconciliationStatus(), query.reconciliationStatus()))
                && (!StringUtils.hasText(query.splitStatus()) || Objects.equals(deriveSplitStatus(), query.splitStatus()))
                && (!StringUtils.hasText(query.upstreamCode()) || toLower(header.getUpstreamCode()).contains(query.upstreamCode()))
                && (!StringUtils.hasText(query.invoiceStatus()) || Objects.equals(deriveInvoiceStatus(), query.invoiceStatus()))
                && (!StringUtils.hasText(query.inspectionCount()) || Objects.equals(deriveInspectionCount(), query.inspectionCount()))
                && (!StringUtils.hasText(query.printStatus()) || Objects.equals(derivePrintStatus(), query.printStatus()))
                && (!StringUtils.hasText(query.remark()) || toLower(header.getRemark()).contains(query.remark()))
                && matchItem(lines, query.itemName());
    }

    private PurchaseInboundRow toPurchaseInboundRow(PurchaseInboundDO header,
                                                    List<PurchaseInboundLineDO> lines,
                                                    Long currentUserId,
                                                    String currentUserName) {
        String creator = Objects.equals(header.getCreatedBy(), currentUserId)
                ? currentUserName
                : "用户#" + header.getCreatedBy();
        return new PurchaseInboundRow(
                header.getId(),
                header.getDocumentCode(),
                header.getInboundDate() == null ? "" : header.getInboundDate().toString(),
                defaultIfBlank(header.getUpstreamCode(), "-"),
                defaultIfBlank(header.getWarehouseName(), "-"),
                defaultIfBlank(header.getSupplierName(), "-"),
                formatInboundAmount(lines),
                defaultIfBlank(header.getStatus(), STATUS_DRAFT),
                deriveReviewStatus(header),
                deriveReconciliationStatus(),
                deriveInvoiceStatus(),
                derivePrintStatus(),
                deriveInspectionCount(),
                formatDateTime(header.getCreatedAt()),
                creator,
                defaultIfBlank(header.getRemark(), ""),
                defaultIfBlank(header.getWorkflowStatus(), "NONE"),
                defaultIfBlank(header.getWorkflowTaskName(), "")
        );
    }

    private String formatInboundAmount(List<PurchaseInboundLineDO> lines) {
        BigDecimal amount = lines.stream()
                .map(line -> {
                    BigDecimal unitPrice = line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice();
                    BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
                    BigDecimal taxRate = line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate();
                    return unitPrice
                            .multiply(quantity)
                            .multiply(BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new DecimalFormat("#,##0.00").format(amount);
    }

    private PageData<PurchaseInboundRow> paginatePurchaseInboundRows(List<PurchaseInboundRow> rows, int pageNo, int pageSize) {
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), pageNo, pageSize);
    }

    private BatchApproveContext buildBatchApproveContext(InventoryScope scope, PurchaseInboundBatchRequest request) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return new BatchApproveContext(
                operatorId,
                requireHeaders(scope, request.ids(), operatorId, true),
                loadLineMap(request.ids())
        );
    }

    private void approveHeader(InventoryScope scope,
                               PurchaseInboundDO header,
                               List<PurchaseInboundLineDO> lines,
                               BatchApproveContext context) {
        if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            return;
        }
        String approverRole = purchaseInboundWorkflowService.resolveApprovalRoleLabel(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                context.operatorId(),
                header.getWorkflowTaskName()
        );
        PurchaseInboundWorkflowService.ApprovalResult workflowResult =
                purchaseInboundWorkflowService.completeCurrentTask(header, context.operatorId());
        if (!workflowResult.workflowApplied()) {
            approveWithoutWorkflow(scope, header, lines, context, approverRole);
            return;
        }
        approveWithWorkflow(scope, header, lines, context, approverRole, workflowResult);
    }

    private void approveWithoutWorkflow(InventoryScope scope,
                                        PurchaseInboundDO header,
                                        List<PurchaseInboundLineDO> lines,
                                        BatchApproveContext context,
                                        String approverRole) {
        if (deletePendingHeaderIfNecessary(header)) {
            return;
        }
        markHeaderApproved(scope, header, lines, context.operatorId());
        purchaseInboundRepository.update(header);
        purchaseInboundNotificationService.recordApproved(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                header,
                approverRole,
                header.getApprovedAt()
        );
    }

    private void approveWithWorkflow(InventoryScope scope,
                                     PurchaseInboundDO header,
                                     List<PurchaseInboundLineDO> lines,
                                     BatchApproveContext context,
                                     String approverRole,
                                     PurchaseInboundWorkflowService.ApprovalResult workflowResult) {
        if (workflowResult.completed()) {
            if (deletePendingHeaderIfNecessary(header)) {
                return;
            }
            markHeaderApproved(scope, header, lines, context.operatorId());
        } else {
            markHeaderSubmitted(header);
        }
        purchaseInboundRepository.update(header);
        purchaseInboundNotificationService.recordApproved(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                header,
                approverRole,
                LocalDateTime.now()
        );
    }

    private boolean deletePendingHeaderIfNecessary(PurchaseInboundDO header) {
        if (!"DELETE".equals(header.getPendingOperation())) {
            return false;
        }
        deletePurchaseInboundInternal(header);
        return true;
    }

    private void markHeaderApproved(InventoryScope scope,
                                    PurchaseInboundDO header,
                                    List<PurchaseInboundLineDO> lines,
                                    Long operatorId) {
        for (PurchaseInboundLineDO line : lines) {
            inventoryStockMutationService.applyDelta(
                    scope.scopeType(),
                    scope.scopeId(),
                    header.getWarehouseName(),
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getQuantity(),
                    "PURCHASE_INBOUND_APPROVE",
                    operatorId
            );
        }
        header.setStatus(STATUS_APPROVED);
        header.setApprovedBy(operatorId);
        header.setApprovedAt(LocalDateTime.now());
        header.setRejectionReason(null);
        header.setPendingOperation("NONE");
    }

    private void markHeaderSubmitted(PurchaseInboundDO header) {
        header.setStatus(STATUS_SUBMITTED);
        header.setApprovedBy(null);
        header.setApprovedAt(null);
    }

    private boolean matchDate(PurchaseInboundDO header, String timeType, LocalDate start, LocalDate end) {
        LocalDate value = Objects.equals(timeType, "创建时间")
                ? (header.getCreatedAt() == null ? null : header.getCreatedAt().toLocalDate())
                : header.getInboundDate();
        if (value == null) {
            return false;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        return end == null || !value.isAfter(end);
    }

    private boolean matchItem(List<PurchaseInboundLineDO> lines, String itemKeyword) {
        if (!StringUtils.hasText(itemKeyword)) {
            return true;
        }
        return lines.stream().anyMatch(line ->
                toLower(line.getItemName()).contains(itemKeyword) || toLower(line.getItemCode()).contains(itemKeyword));
    }

    private String deriveReviewStatus(PurchaseInboundDO header) {
        return Objects.equals(header.getStatus(), STATUS_APPROVED) ? "已复审" : "未复审";
    }

    private String deriveReconciliationStatus() {
        return "未对账";
    }

    private String deriveSplitStatus() {
        return "未分账";
    }

    private String deriveInvoiceStatus() {
        return "未开票";
    }

    private String derivePrintStatus() {
        return "未打印";
    }

    private String deriveInspectionCount() {
        return "0";
    }

    private String generateDocumentCode(InventoryScope scope) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
            String code = "RK-" + datePart + "-" + rand;
            Long count = purchaseInboundRepository.countByScopeAndDocumentCode(scope.scopeType(), scope.scopeId(), code);
            if (count == null || count == 0L) {
                return code;
            }
        }
        throw new BusinessException("生成单据号失败，请重试");
    }

    private void ensurePurchaseInboundOperationPermission(InventoryScope scope, String action) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        purchaseInboundPermissionService.ensureOperationPermission(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                action
        );
    }

    private void ensurePurchaseInboundReviewPermission(InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        purchaseInboundPermissionService.ensureReviewPermission(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
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
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(message);
        }
    }

    private BigDecimal normalizePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
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

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATETIME_FORMATTER.format(value);
    }

    private void deletePurchaseInboundInternal(PurchaseInboundDO header) {
        if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException("已审核单据请先反审核后再删除");
        }
        if (StringUtils.hasText(header.getWorkflowInstanceId())) {
            purchaseInboundWorkflowService.cancelWorkflowInstanceIfRunning(header);
        }
        purchaseInboundLineRepository.deleteByInboundId(header.getId());
        purchaseInboundRepository.deleteById(header.getId());
    }

    private void saveDeleteWorkflow(InventoryScope scope, PurchaseInboundDO header, Long operatorId) {
        header.setPendingOperation("DELETE");
        purchaseInboundWorkflowService.syncOnAction(scope.scopeType(), scope.scopeId(), scope.groupId(), header, operatorId, "DELETE");
        header.setStatus(STATUS_SUBMITTED);
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        purchaseInboundRepository.update(header);
    }

    public record IdPayload(Long id, String documentCode) {
    }

    public record PurchaseInboundDetail(Long id,
                                        String documentCode,
                                        String status,
                                        String inboundDate,
                                        String warehouse,
                                        String supplier,
                                        Long salesmanUserId,
                                        String salesmanName,
                                        String upstreamCode,
                                        String remark,
                                        String rejectionReason,
                                        String workflowProcessCode,
                                        String workflowDefinitionKey,
                                        String workflowDefinitionId,
                                        String workflowInstanceId,
                                        String workflowTaskId,
                                        String workflowTaskName,
                                        String workflowStatus,
                                        List<PurchaseInboundDetailLine> items) {
    }

    public record PurchaseInboundDetailLine(String itemCode,
                                            String itemName,
                                            BigDecimal quantity,
                                            BigDecimal unitPrice,
                                            BigDecimal taxRate) {
    }

    public record PurchaseInboundRow(Long id,
                                     String documentCode,
                                     String inboundDate,
                                     String upstreamCode,
                                     String warehouse,
                                     String supplier,
                                     String amountTaxIncluded,
                                     String status,
                                     String reviewStatus,
                                     String reconciliationStatus,
                                     String invoiceStatus,
                                     String printStatus,
                                     String inspectionCount,
                                     String createdAt,
                                     String creator,
                                     String remark,
                                     String workflowStatus,
                                     String workflowTaskName) {
    }

    public record InventoryBalanceRow(String warehouse,
                                      String itemCode,
                                      String itemName,
                                      String quantity,
                                      String updatedAt) {
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    public record PurchaseInboundPermissionView(boolean canCreate,
                                                boolean canUpdate,
                                                boolean canDelete,
                                                boolean canApprove,
                                                boolean canUnapprove) {
    }

    private record PurchaseInboundListAccess(boolean viewAll, boolean canReview) {
    }

    private record PurchaseInboundListQuery(int pageNo,
                                            int pageSize,
                                            String timeType,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            String warehouse,
                                            String documentCode,
                                            String supplier,
                                            String itemName,
                                            String documentStatus,
                                            String reviewStatus,
                                            String reconciliationStatus,
                                            String splitStatus,
                                            String upstreamCode,
                                            String invoiceStatus,
                                            String inspectionCount,
                                            String printStatus,
                                            String remark) {
    }

    private record BatchApproveContext(Long operatorId,
                                       List<PurchaseInboundDO> headers,
                                       Map<Long, List<PurchaseInboundLineDO>> lineMap) {
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }
}
