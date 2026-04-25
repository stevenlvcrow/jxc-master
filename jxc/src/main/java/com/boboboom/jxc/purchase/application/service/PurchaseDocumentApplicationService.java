package com.boboboom.jxc.purchase.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentWorkflowService;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

@Service
public class PurchaseDocumentApplicationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OrgScopeService orgScopeService;
    private final InventoryDocumentWorkflowService workflowService;
    private final WorkflowApprovalNotificationApplicationService notificationService;

    public PurchaseDocumentApplicationService(NamedParameterJdbcTemplate jdbcTemplate,
                                              OrgScopeService orgScopeService,
                                              InventoryDocumentWorkflowService workflowService,
                                              WorkflowApprovalNotificationApplicationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.orgScopeService = orgScopeService;
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    public PageData<PurchaseDocumentView> page(String rawDocumentType, Map<String, String> params) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(params.get("orgId"));
        int pageNo = parsePositiveInt(params.get("pageNo"), 1);
        int pageSize = Math.min(parsePositiveInt(params.get("pageSize"), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        List<Header> headers = loadHeaders(scope, kind.storageType());
        Map<Long, List<Line>> lineMap = loadLineMap(headers.stream().map(item -> item.id).toList());
        List<PurchaseDocumentView> filtered = headers.stream()
                .map(header -> toView(header, lineMap.getOrDefault(header.id, List.of()), kind))
                .filter(row -> matches(row, params))
                .sorted(Comparator.comparing(PurchaseDocumentView::documentDate).reversed()
                        .thenComparing(PurchaseDocumentView::id, Comparator.reverseOrder()))
                .toList();
        int start = Math.min((pageNo - 1) * pageSize, filtered.size());
        int end = Math.min(start + pageSize, filtered.size());
        return new PageData<>(filtered.subList(start, end), filtered.size(), pageNo, pageSize);
    }

    public PurchaseDocumentView detail(String rawDocumentType, Long id, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Header header = requireHeader(scope, kind.storageType(), id);
        return toView(header, loadLines(header.id), kind);
    }

    @Transactional
    public IdPayload create(String rawDocumentType, SavePurchaseDocumentRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        Header header = new Header();
        header.scopeType = scope.scopeType();
        header.scopeId = scope.scopeId();
        header.documentType = kind.storageType();
        header.documentCode = generateDocumentCode(scope, kind);
        header.documentStatus = "草稿";
        header.reviewStatus = "待审核";
        header.workflowStatus = "NONE";
        header.pendingOperation = "NONE";
        header.createdBy = operatorId;
        header.creatorName = AuthContextHolder.userNameOr("system");
        applyRequest(header, request, kind, true);
        header.id = insertHeader(header);
        replaceLines(header.id, request.items());
        return new IdPayload(header.id, header.documentCode);
    }

    @Transactional
    public void update(String rawDocumentType, Long id, SavePurchaseDocumentRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Header header = requireHeader(scope, kind.storageType(), id);
        ensureEditable(header);
        applyRequest(header, request, kind, false);
        header.updatedAt = LocalDateTime.now();
        updateHeader(header);
        replaceLines(header.id, request.items());
    }

    @Transactional
    public void delete(String rawDocumentType, Long id, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Header header = requireHeader(scope, kind.storageType(), id);
        ensureEditable(header);
        jdbcTemplate.update("DELETE FROM purchase_document WHERE id = :id", new MapSqlParameterSource("id", header.id));
    }

    @Transactional
    public void batchDelete(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            ensureEditable(header);
            jdbcTemplate.update("DELETE FROM purchase_document WHERE id = :id", new MapSqlParameterSource("id", header.id));
        }
    }

    @Transactional
    public void batchSubmit(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            if ("已审核".equals(header.documentStatus)) {
                continue;
            }
            header.documentStatus = "已提交";
            header.reviewStatus = "待审核";
            header.submitterName = AuthContextHolder.userNameOr("system");
            header.lastOperatorName = header.submitterName;
            header.lastOperatedAt = LocalDateTime.now();
            header.updatedAt = LocalDateTime.now();
            boolean workflowApplied = syncWorkflow(scope, kind, header, operatorId, "CREATE", true);
            if (!workflowApplied) {
                throw new BusinessException(kind.businessName() + "未发起审批流，不能提交");
            }
            recordPending(scope, kind, header);
        }
    }

    @Transactional
    public void batchApprove(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            approveHeader(scope, kind, header, operatorId);
        }
    }

    @Transactional
    public void batchReject(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String reason = requiredTrim(request.rejectionReason(), "驳回原因不能为空");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            rejectHeader(scope, kind, header, operatorId, reason);
        }
    }

    @Transactional
    public void batchUnapprove(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            header.documentStatus = "已提交";
            header.reviewStatus = "待审核";
            header.approvedBy = null;
            header.approvedAt = null;
            header.lastOperatorName = AuthContextHolder.userNameOr("system");
            header.lastOperatedAt = LocalDateTime.now();
            header.updatedAt = LocalDateTime.now();
            InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
            workflowService.resetBusinessWorkflowState(
                    workflowHeader,
                    () -> {
                        applyWorkflowHeader(header, workflowHeader);
                        updateHeader(header);
                    },
                    kind.businessName() + "反审核"
            );
        }
    }

    @Transactional
    public void batchPrint(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.printStatus = "已打印");
    }

    @Transactional
    public void batchClose(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.documentStatus = "已关闭");
    }

    @Transactional
    public void batchCancelClose(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.documentStatus = "已提交");
    }

    @Transactional
    public void batchReceive(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> {
            header.receiveStatus = "已收货";
            header.shipStatus = "已发货";
            header.documentStatus = "已收货";
        });
    }

    @Transactional
    public void batchCancelReceive(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> {
            header.receiveStatus = "未收货";
            header.documentStatus = "待收货";
        });
    }

    @Transactional
    public void reviewApplicationLines(LineReviewBatchRequest request, String orgId) {
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        if (request.lineReviews() == null || request.lineReviews().isEmpty()) {
            throw new BusinessException("请选择需要审核的明细");
        }
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        List<Long> lineIds = request.lineReviews().stream().map(LineReviewRequest::lineId).filter(Objects::nonNull).distinct().toList();
        if (lineIds.isEmpty()) {
            throw new BusinessException("请选择需要审核的明细");
        }
        Map<Long, LineReviewRequest> reviewMap = request.lineReviews().stream()
                .filter(item -> item.lineId() != null)
                .collect(Collectors.toMap(LineReviewRequest::lineId, item -> item, (left, right) -> right, LinkedHashMap::new));
        List<Line> lines = loadLinesByIds(lineIds);
        if (lines.size() != lineIds.size()) {
            throw new BusinessException("存在无效明细");
        }
        Set<Long> documentIds = lines.stream().map(item -> item.documentId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Header> headers = requireHeaders(scope, "APPLICATION", new ArrayList<>(documentIds));
        Map<Long, Header> headerMap = headers.stream().collect(Collectors.toMap(item -> item.id, item -> item));
        boolean approved = Boolean.TRUE.equals(request.approved());
        for (Line line : lines) {
            Header header = headerMap.get(line.documentId);
            if (header == null) {
                throw new BusinessException("存在无效明细");
            }
            LineReviewRequest review = reviewMap.get(line.id);
            BigDecimal reviewQty = review.reviewQty() == null ? line.quantity : review.reviewQty();
            updateLineReview(line.id, approved ? "已审核" : "已驳回", reviewQty, trimNullable(review.remark()));
        }
        if (!approved) {
            String reason = requiredTrim(request.rejectionReason(), "驳回原因不能为空");
            for (Header header : headers) {
                rejectHeader(scope, DocumentKind.APPLICATION, header, operatorId, reason);
            }
            return;
        }
        for (Header header : headers) {
            List<Line> currentLines = loadLines(header.id);
            boolean allApproved = currentLines.stream().allMatch(line -> "已审核".equals(line.reviewStatus));
            if (allApproved) {
                approveHeader(scope, DocumentKind.APPLICATION, header, operatorId);
            }
        }
    }

    @Transactional
    public void updateApplicationLines(LineUpdateBatchRequest request, String orgId) {
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        if (request.lineUpdates() == null || request.lineUpdates().isEmpty()) {
            throw new BusinessException("请选择需要保存的明细");
        }
        List<Long> lineIds = request.lineUpdates().stream().map(LineUpdateRequest::lineId).filter(Objects::nonNull).distinct().toList();
        if (lineIds.isEmpty()) {
            throw new BusinessException("请选择需要保存的明细");
        }
        Map<Long, LineUpdateRequest> updateMap = request.lineUpdates().stream()
                .filter(item -> item.lineId() != null)
                .collect(Collectors.toMap(LineUpdateRequest::lineId, item -> item, (left, right) -> right, LinkedHashMap::new));
        List<Line> lines = loadLinesByIds(lineIds);
        if (lines.size() != lineIds.size()) {
            throw new BusinessException("存在无效明细");
        }
        Set<Long> documentIds = lines.stream().map(item -> item.documentId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Header> headers = requireHeaders(scope, "APPLICATION", new ArrayList<>(documentIds));
        Map<Long, Header> headerMap = headers.stream().collect(Collectors.toMap(item -> item.id, item -> item));
        for (Line line : lines) {
            if (!headerMap.containsKey(line.documentId)) {
                throw new BusinessException("存在无效明细");
            }
            LineUpdateRequest update = updateMap.get(line.id);
            BigDecimal reviewQty = update.reviewQty() == null ? line.reviewQty : nonNegative(update.reviewQty(), "审核数量不能小于0");
            updateLinePatch(
                    line.id,
                    trimNullable(update.supplier()),
                    parseDateNullable(update.expectedArrivalDate()),
                    reviewQty,
                    trimNullable(update.remark())
            );
        }
        jdbcTemplate.update("""
                UPDATE purchase_document
                SET last_operator_name = :operator,
                    last_operated_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id IN (:ids)
                """, new MapSqlParameterSource()
                .addValue("operator", AuthContextHolder.userNameOr("system"))
                .addValue("ids", new ArrayList<>(documentIds)));
    }

    private void approveHeader(OrgScopeService.AccessibleScope scope, DocumentKind kind, Header header, Long operatorId) {
        String role = workflowService.resolveApprovalRoleLabel(
                kind.businessCode(),
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                header.workflowTaskName
        );
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        InventoryDocumentWorkflowService.ApprovalResult result = workflowService.completeBusinessCurrentTask(
                kind.businessCode(),
                kind.businessName() + "流程",
                workflowHeader,
                operatorId,
                scope.groupId(),
                () -> {
                    applyWorkflowHeader(header, workflowHeader);
                    updateHeader(header);
                }
        );
        if (!result.workflowApplied()) {
            throw new BusinessException(kind.businessName() + "未发起审批流，不能审核");
        }
        applyWorkflowHeader(header, workflowHeader);
        if (result.completed()) {
            header.documentStatus = terminalApprovedStatus(kind);
            header.reviewStatus = "已审核";
            header.approvedBy = operatorId;
            header.approvedAt = LocalDateTime.now();
        } else {
            header.documentStatus = "已提交";
            header.reviewStatus = "待审核";
        }
        header.lastOperatorName = AuthContextHolder.userNameOr("system");
        header.lastOperatedAt = LocalDateTime.now();
        header.updatedAt = LocalDateTime.now();
        updateHeader(header);
        recordAudit(scope, kind, header, role, result.completed() ? "通过" : "通过", "");
        if (!result.completed()) {
            recordPending(scope, kind, header);
        }
    }

    private void rejectHeader(OrgScopeService.AccessibleScope scope, DocumentKind kind, Header header, Long operatorId, String reason) {
        if (!StringUtils.hasText(header.workflowInstanceId) && !"RUNNING".equals(header.workflowStatus)) {
            throw new BusinessException(kind.businessName() + "未发起审批流，不能驳回");
        }
        String role = workflowService.resolveApprovalRoleLabel(
                kind.businessCode(),
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                header.workflowTaskName
        );
        header.documentStatus = "已驳回";
        header.reviewStatus = "已驳回";
        header.rejectionReason = reason;
        header.lastOperatorName = AuthContextHolder.userNameOr("system");
        header.lastOperatedAt = LocalDateTime.now();
        header.updatedAt = LocalDateTime.now();
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        workflowService.resetBusinessWorkflowState(
                workflowHeader,
                () -> {
                    applyWorkflowHeader(header, workflowHeader);
                    updateHeader(header);
                },
                kind.businessName() + "驳回"
        );
        recordAudit(scope, kind, header, role, "拒绝", reason);
    }

    private boolean syncWorkflow(OrgScopeService.AccessibleScope scope,
                                 DocumentKind kind,
                                 Header header,
                                 Long operatorId,
                                 String action,
                                 boolean requireWorkflow) {
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        boolean applied = workflowService.syncBusinessOnAction(
                kind.businessCode(),
                kind.businessName() + "流程",
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                workflowHeader,
                operatorId,
                action,
                () -> {
                    applyWorkflowHeader(header, workflowHeader);
                    updateHeader(header);
                },
                kind.businessName() + "删除",
                requireWorkflow
        );
        applyWorkflowHeader(header, workflowHeader);
        return applied;
    }

    private void recordPending(OrgScopeService.AccessibleScope scope, DocumentKind kind, Header header) {
        WorkflowActionService.ApprovalTarget target = workflowService
                .resolveApprovalTarget(kind.businessCode(), scope.scopeType(), scope.scopeId(), scope.groupId(), header.workflowTaskName)
                .orElse(null);
        notificationService.record(
                scope.scopeType(),
                scope.scopeId(),
                kind.businessCode(),
                kind.businessName() + "流程",
                header.id,
                header.documentCode,
                AuthContextHolder.userNameOr("system"),
                "发起人",
                target == null ? null : target.userId(),
                target == null ? null : target.roleCode(),
                target == null ? null : target.roleName(),
                LocalDateTime.now(),
                "待审核",
                "",
                kind.routePath()
        );
    }

    private void recordAudit(OrgScopeService.AccessibleScope scope,
                             DocumentKind kind,
                             Header header,
                             String role,
                             String result,
                             String remark) {
        notificationService.record(
                scope.scopeType(),
                scope.scopeId(),
                kind.businessCode(),
                kind.businessName() + "流程",
                header.id,
                header.documentCode,
                AuthContextHolder.userNameOr("system"),
                StringUtils.hasText(role) ? role : "普通审核",
                null,
                null,
                null,
                LocalDateTime.now(),
                result,
                remark,
                kind.routePath()
        );
    }

    private void updateSimpleStatus(String rawDocumentType,
                                    BatchActionRequest request,
                                    String orgId,
                                    HeaderMutator mutator) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids())) {
            mutator.apply(header);
            header.lastOperatorName = AuthContextHolder.userNameOr("system");
            header.lastOperatedAt = LocalDateTime.now();
            header.updatedAt = LocalDateTime.now();
            updateHeader(header);
        }
    }

    private void applyRequest(Header header, SavePurchaseDocumentRequest request, DocumentKind kind, boolean createMode) {
        header.documentDate = parseRequiredDate(request.documentDate(), "单据日期不能为空");
        header.expectedArrivalDate = parseDateNullable(request.expectedArrivalDate());
        header.purchaseOrg = trimNullable(request.purchaseOrg());
        header.warehouseName = requiredTrim(request.warehouse(), "仓库不能为空");
        header.supplierName = trimNullable(request.supplier());
        header.sourceDocumentCode = trimNullable(request.sourceDocumentCode());
        header.downstreamDocumentCode = trimNullable(request.downstreamDocumentCode());
        header.documentBizType = StringUtils.hasText(request.documentBizType()) ? request.documentBizType() : defaultBizType(kind);
        header.shipStatus = StringUtils.hasText(request.shipStatus()) ? request.shipStatus() : defaultShipStatus(kind);
        header.receiveStatus = StringUtils.hasText(request.receiveStatus()) ? request.receiveStatus() : defaultReceiveStatus(kind);
        header.reconciliationStatus = StringUtils.hasText(request.reconciliationStatus()) ? request.reconciliationStatus() : "未对账";
        header.supplierSplitStatus = StringUtils.hasText(request.supplierSplit()) ? request.supplierSplit() : "未分账";
        header.splitReceiptStatus = StringUtils.hasText(request.splitReceipt()) ? request.splitReceipt() : "不拆分";
        header.printStatus = StringUtils.hasText(request.printStatus()) ? request.printStatus() : "未打印";
        header.returnReason = trimNullable(request.returnReason());
        header.inspectionStatus = StringUtils.hasText(request.inspectionStatus()) ? request.inspectionStatus() : "无需质检";
        header.adjustedPrice = Boolean.TRUE.equals(request.adjustedPrice());
        header.applicantName = StringUtils.hasText(request.applicant()) ? request.applicant() : AuthContextHolder.userNameOr("system");
        header.submitterName = trimNullable(request.submitter());
        header.remark = trimNullable(request.remark());
        header.lastOperatorName = AuthContextHolder.userNameOr("system");
        header.lastOperatedAt = LocalDateTime.now();
        if (!createMode) {
            header.updatedAt = LocalDateTime.now();
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("物品明细不能为空");
        }
    }

    private Long insertHeader(Header header) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO purchase_document (
                    scope_type, scope_id, document_type, document_code, document_date, expected_arrival_date,
                    purchase_org, warehouse_name, supplier_name, source_document_code, downstream_document_code,
                    document_status, review_status, ship_status, receive_status, reconciliation_status,
                    supplier_split_status, document_biz_type, split_receipt_status, print_status, return_reason,
                    inspection_status, inspection_count, adjusted_price, workflow_status, pending_operation,
                    creator_name, submitter_name, applicant_name, last_operator_name, last_operated_at,
                    remark, created_by, created_at, updated_at
                ) VALUES (
                    :scopeType, :scopeId, :documentType, :documentCode, :documentDate, :expectedArrivalDate,
                    :purchaseOrg, :warehouseName, :supplierName, :sourceDocumentCode, :downstreamDocumentCode,
                    :documentStatus, :reviewStatus, :shipStatus, :receiveStatus, :reconciliationStatus,
                    :supplierSplitStatus, :documentBizType, :splitReceiptStatus, :printStatus, :returnReason,
                    :inspectionStatus, :inspectionCount, :adjustedPrice, :workflowStatus, :pendingOperation,
                    :creatorName, :submitterName, :applicantName, :lastOperatorName, :lastOperatedAt,
                    :remark, :createdBy, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, toHeaderParams(header), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("采购单据保存失败");
        }
        return key.longValue();
    }

    private void updateHeader(Header header) {
        jdbcTemplate.update("""
                UPDATE purchase_document
                SET document_date = :documentDate,
                    expected_arrival_date = :expectedArrivalDate,
                    purchase_org = :purchaseOrg,
                    warehouse_name = :warehouseName,
                    supplier_name = :supplierName,
                    source_document_code = :sourceDocumentCode,
                    downstream_document_code = :downstreamDocumentCode,
                    document_status = :documentStatus,
                    review_status = :reviewStatus,
                    ship_status = :shipStatus,
                    receive_status = :receiveStatus,
                    reconciliation_status = :reconciliationStatus,
                    supplier_split_status = :supplierSplitStatus,
                    document_biz_type = :documentBizType,
                    split_receipt_status = :splitReceiptStatus,
                    print_status = :printStatus,
                    return_reason = :returnReason,
                    inspection_status = :inspectionStatus,
                    inspection_count = :inspectionCount,
                    adjusted_price = :adjustedPrice,
                    workflow_process_code = :workflowProcessCode,
                    workflow_definition_key = :workflowDefinitionKey,
                    workflow_definition_id = :workflowDefinitionId,
                    workflow_instance_id = :workflowInstanceId,
                    workflow_task_id = :workflowTaskId,
                    workflow_task_name = :workflowTaskName,
                    workflow_status = :workflowStatus,
                    pending_operation = :pendingOperation,
                    rejection_reason = :rejectionReason,
                    creator_name = :creatorName,
                    submitter_name = :submitterName,
                    applicant_name = :applicantName,
                    last_operator_name = :lastOperatorName,
                    last_operated_at = :lastOperatedAt,
                    remark = :remark,
                    approved_by = :approvedBy,
                    approved_at = :approvedAt,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, toHeaderParams(header).addValue("id", header.id));
    }

    private void replaceLines(Long documentId, List<LineRequest> items) {
        jdbcTemplate.update("DELETE FROM purchase_document_line WHERE document_id = :documentId",
                new MapSqlParameterSource("documentId", documentId));
        for (LineRequest item : items) {
            BigDecimal quantity = positive(item.quantity(), "数量必须大于0");
            BigDecimal price = nonNegative(item.unitPrice(), "单价不能小于0");
            BigDecimal amount = item.amount() == null ? quantity.multiply(price) : nonNegative(item.amount(), "金额不能小于0");
            jdbcTemplate.update("""
                    INSERT INTO purchase_document_line (
                        document_id, item_code, item_name, spec, item_category, supplier_name, purchase_unit,
                        base_unit, base_conversion, quantity, review_qty, received_qty, unit_price, tax_rate,
                        amount, is_gift, warehouse_name, expected_arrival_date, review_status, remark, created_at
                    ) VALUES (
                        :documentId, :itemCode, :itemName, :spec, :itemCategory, :supplierName, :purchaseUnit,
                        :baseUnit, :baseConversion, :quantity, :reviewQty, :receivedQty, :unitPrice, :taxRate,
                        :amount, :isGift, :warehouseName, :expectedArrivalDate, :reviewStatus, :remark, CURRENT_TIMESTAMP
                    )
                    """, new MapSqlParameterSource()
                    .addValue("documentId", documentId)
                    .addValue("itemCode", requiredTrim(item.itemCode(), "物品编码不能为空"))
                    .addValue("itemName", requiredTrim(item.itemName(), "物品名称不能为空"))
                    .addValue("spec", trimNullable(item.spec()))
                    .addValue("itemCategory", trimNullable(item.itemCategory()))
                    .addValue("supplierName", trimNullable(item.supplier()))
                    .addValue("purchaseUnit", trimNullable(item.purchaseUnit()))
                    .addValue("baseUnit", trimNullable(item.baseUnit()))
                    .addValue("baseConversion", trimNullable(item.baseConversion()))
                    .addValue("quantity", quantity)
                    .addValue("reviewQty", item.reviewQty() == null ? quantity : nonNegative(item.reviewQty(), "审核数量不能小于0"))
                    .addValue("receivedQty", item.receivedQty() == null ? BigDecimal.ZERO : nonNegative(item.receivedQty(), "收货数量不能小于0"))
                    .addValue("unitPrice", price)
                    .addValue("taxRate", item.taxRate() == null ? BigDecimal.ZERO : nonNegative(item.taxRate(), "税率不能小于0"))
                    .addValue("amount", amount)
                    .addValue("isGift", Boolean.TRUE.equals(item.isGift()))
                    .addValue("warehouseName", trimNullable(item.warehouse()))
                    .addValue("expectedArrivalDate", parseDateNullable(item.expectedArrivalDate()))
                    .addValue("reviewStatus", StringUtils.hasText(item.reviewStatus()) ? item.reviewStatus() : "待审核")
                    .addValue("remark", trimNullable(item.remark())));
        }
    }

    private MapSqlParameterSource toHeaderParams(Header header) {
        return new MapSqlParameterSource()
                .addValue("scopeType", header.scopeType)
                .addValue("scopeId", header.scopeId)
                .addValue("documentType", header.documentType)
                .addValue("documentCode", header.documentCode)
                .addValue("documentDate", header.documentDate)
                .addValue("expectedArrivalDate", header.expectedArrivalDate)
                .addValue("purchaseOrg", header.purchaseOrg)
                .addValue("warehouseName", header.warehouseName)
                .addValue("supplierName", header.supplierName)
                .addValue("sourceDocumentCode", header.sourceDocumentCode)
                .addValue("downstreamDocumentCode", header.downstreamDocumentCode)
                .addValue("documentStatus", header.documentStatus)
                .addValue("reviewStatus", header.reviewStatus)
                .addValue("shipStatus", header.shipStatus)
                .addValue("receiveStatus", header.receiveStatus)
                .addValue("reconciliationStatus", header.reconciliationStatus)
                .addValue("supplierSplitStatus", header.supplierSplitStatus)
                .addValue("documentBizType", header.documentBizType)
                .addValue("splitReceiptStatus", header.splitReceiptStatus)
                .addValue("printStatus", header.printStatus)
                .addValue("returnReason", header.returnReason)
                .addValue("inspectionStatus", header.inspectionStatus)
                .addValue("inspectionCount", header.inspectionCount)
                .addValue("adjustedPrice", header.adjustedPrice)
                .addValue("workflowProcessCode", header.workflowProcessCode)
                .addValue("workflowDefinitionKey", header.workflowDefinitionKey)
                .addValue("workflowDefinitionId", header.workflowDefinitionId)
                .addValue("workflowInstanceId", header.workflowInstanceId)
                .addValue("workflowTaskId", header.workflowTaskId)
                .addValue("workflowTaskName", header.workflowTaskName)
                .addValue("workflowStatus", StringUtils.hasText(header.workflowStatus) ? header.workflowStatus : "NONE")
                .addValue("pendingOperation", StringUtils.hasText(header.pendingOperation) ? header.pendingOperation : "NONE")
                .addValue("rejectionReason", header.rejectionReason)
                .addValue("creatorName", header.creatorName)
                .addValue("submitterName", header.submitterName)
                .addValue("applicantName", header.applicantName)
                .addValue("lastOperatorName", header.lastOperatorName)
                .addValue("lastOperatedAt", header.lastOperatedAt)
                .addValue("remark", header.remark)
                .addValue("createdBy", header.createdBy)
                .addValue("approvedBy", header.approvedBy)
                .addValue("approvedAt", header.approvedAt);
    }

    private List<Header> loadHeaders(OrgScopeService.AccessibleScope scope, String documentType) {
        return jdbcTemplate.query("""
                SELECT *
                FROM purchase_document
                WHERE scope_type = :scopeType
                  AND scope_id = :scopeId
                  AND document_type = :documentType
                ORDER BY document_date DESC, id DESC
                """, new MapSqlParameterSource()
                .addValue("scopeType", scope.scopeType())
                .addValue("scopeId", scope.scopeId())
                .addValue("documentType", documentType), headerMapper());
    }

    private Header requireHeader(OrgScopeService.AccessibleScope scope, String documentType, Long id) {
        if (id == null) {
            throw new BusinessException("单据ID不能为空");
        }
        List<Header> rows = jdbcTemplate.query("""
                SELECT *
                FROM purchase_document
                WHERE id = :id
                  AND scope_type = :scopeType
                  AND scope_id = :scopeId
                  AND document_type = :documentType
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("scopeType", scope.scopeType())
                .addValue("scopeId", scope.scopeId())
                .addValue("documentType", documentType), headerMapper());
        if (rows.isEmpty()) {
            throw new BusinessException("单据不存在");
        }
        return rows.get(0);
    }

    private List<Header> requireHeaders(OrgScopeService.AccessibleScope scope, String documentType, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择单据");
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException("请选择单据");
        }
        List<Header> rows = jdbcTemplate.query("""
                SELECT *
                FROM purchase_document
                WHERE scope_type = :scopeType
                  AND scope_id = :scopeId
                  AND document_type = :documentType
                  AND id IN (:ids)
                """, new MapSqlParameterSource()
                .addValue("scopeType", scope.scopeType())
                .addValue("scopeId", scope.scopeId())
                .addValue("documentType", documentType)
                .addValue("ids", distinctIds), headerMapper());
        if (rows.size() != distinctIds.size()) {
            throw new BusinessException("存在无效单据");
        }
        return rows;
    }

    private Map<Long, List<Line>> loadLineMap(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                SELECT *
                FROM purchase_document_line
                WHERE document_id IN (:documentIds)
                ORDER BY id ASC
                """, new MapSqlParameterSource("documentIds", documentIds), lineMapper())
                .stream()
                .collect(Collectors.groupingBy(item -> item.documentId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<Line> loadLines(Long documentId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM purchase_document_line
                WHERE document_id = :documentId
                ORDER BY id ASC
                """, new MapSqlParameterSource("documentId", documentId), lineMapper());
    }

    private List<Line> loadLinesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT *
                FROM purchase_document_line
                WHERE id IN (:ids)
                """, new MapSqlParameterSource("ids", ids), lineMapper());
    }

    private void updateLineReview(Long lineId, String reviewStatus, BigDecimal reviewQty, String remark) {
        jdbcTemplate.update("""
                UPDATE purchase_document_line
                SET review_status = :reviewStatus,
                    review_qty = :reviewQty,
                    remark = COALESCE(:remark, remark)
                WHERE id = :lineId
                """, new MapSqlParameterSource()
                .addValue("lineId", lineId)
                .addValue("reviewStatus", reviewStatus)
                .addValue("reviewQty", reviewQty)
                .addValue("remark", remark));
    }

    private void updateLinePatch(Long lineId, String supplier, LocalDate expectedArrivalDate, BigDecimal reviewQty, String remark) {
        jdbcTemplate.update("""
                UPDATE purchase_document_line
                SET supplier_name = COALESCE(:supplierName, supplier_name),
                    expected_arrival_date = COALESCE(:expectedArrivalDate, expected_arrival_date),
                    review_qty = :reviewQty,
                    remark = COALESCE(:remark, remark)
                WHERE id = :lineId
                """, new MapSqlParameterSource()
                .addValue("lineId", lineId)
                .addValue("supplierName", supplier)
                .addValue("expectedArrivalDate", expectedArrivalDate == null ? null : Date.valueOf(expectedArrivalDate))
                .addValue("reviewQty", reviewQty)
                .addValue("remark", remark));
    }

    private PurchaseDocumentView toView(Header header, List<Line> lines, DocumentKind kind) {
        BigDecimal amount = lines.stream().map(line -> line.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseDocumentView(
                header.id,
                kind.routeKey(),
                header.documentCode,
                formatDate(header.documentDate),
                formatDate(header.expectedArrivalDate),
                defaultIfBlank(header.purchaseOrg, "采购中心"),
                defaultIfBlank(header.warehouseName, ""),
                defaultIfBlank(header.supplierName, ""),
                defaultIfBlank(header.sourceDocumentCode, "-"),
                defaultIfBlank(header.downstreamDocumentCode, "-"),
                defaultIfBlank(header.documentStatus, "草稿"),
                defaultIfBlank(header.reviewStatus, "待审核"),
                defaultIfBlank(header.shipStatus, "未发货"),
                defaultIfBlank(header.receiveStatus, "未收货"),
                defaultIfBlank(header.reconciliationStatus, "未对账"),
                defaultIfBlank(header.supplierSplitStatus, "未分账"),
                defaultIfBlank(header.documentBizType, defaultBizType(kind)),
                defaultIfBlank(header.splitReceiptStatus, "不拆分"),
                defaultIfBlank(header.printStatus, "未打印"),
                defaultIfBlank(header.returnReason, ""),
                defaultIfBlank(header.inspectionStatus, "无需质检"),
                header.inspectionCount == null ? 0 : header.inspectionCount,
                Boolean.TRUE.equals(header.adjustedPrice),
                defaultIfBlank(header.creatorName, ""),
                defaultIfBlank(header.submitterName, ""),
                defaultIfBlank(header.applicantName, ""),
                defaultIfBlank(header.lastOperatorName, ""),
                formatDateTime(header.lastOperatedAt),
                defaultIfBlank(header.remark, ""),
                defaultIfBlank(header.rejectionReason, ""),
                amount.setScale(2, RoundingMode.HALF_UP),
                lines.size(),
                defaultIfBlank(header.workflowStatus, "NONE"),
                defaultIfBlank(header.workflowTaskName, ""),
                lines.stream().map(this::toLineView).toList(),
                formatDateTime(header.createdAt)
        );
    }

    private PurchaseDocumentLineView toLineView(Line line) {
        return new PurchaseDocumentLineView(
                line.id,
                line.itemCode,
                line.itemName,
                defaultIfBlank(line.spec, ""),
                defaultIfBlank(line.itemCategory, ""),
                defaultIfBlank(line.supplierName, ""),
                defaultIfBlank(line.purchaseUnit, ""),
                defaultIfBlank(line.baseUnit, ""),
                defaultIfBlank(line.baseConversion, ""),
                line.quantity,
                line.reviewQty,
                line.receivedQty,
                line.unitPrice,
                line.taxRate,
                line.amount,
                line.isGift,
                defaultIfBlank(line.warehouseName, ""),
                formatDate(line.expectedArrivalDate),
                defaultIfBlank(line.reviewStatus, "待审核"),
                defaultIfBlank(line.remark, "")
        );
    }

    private boolean matches(PurchaseDocumentView row, Map<String, String> params) {
        LocalDate start = parseDateNullable(firstParam(params, "startDate", "dateStart"));
        LocalDate end = parseDateNullable(firstParam(params, "endDate", "dateEnd"));
        LocalDate date = parseDateNullable(row.documentDate());
        return matchDate(date, start, end)
                && contains(row.documentCode(), firstParam(params, "documentCode", "applicationCode", "receiptCode"))
                && equalsIfPresent(row.warehouse(), firstParam(params, "warehouse", "receiptWarehouse", "returnWarehouse"))
                && equalsIfPresent(row.supplier(), params.get("supplier"))
                && equalsIfPresent(row.documentStatus(), params.get("documentStatus"))
                && equalsIfPresent(row.reviewStatus(), params.get("reviewStatus"))
                && equalsIfPresent(row.shipStatus(), params.get("shipStatus"))
                && equalsIfPresent(row.receiveStatus(), params.get("receiveStatus"))
                && equalsIfPresent(row.reconciliationStatus(), params.get("reconciliationStatus"))
                && equalsIfPresent(row.supplierSplit(), params.get("supplierSplit"))
                && equalsIfPresent(row.documentBizType(), params.get("documentType"))
                && equalsIfPresent(row.returnReason(), params.get("returnReason"))
                && equalsIfPresent(row.inspectionStatus(), params.get("inspectionStatus"))
                && ("全部".equals(defaultIfBlank(params.get("printStatus"), "全部")) || equalsIfPresent(row.printStatus(), params.get("printStatus")))
                && contains(row.sourceDocumentCode(), firstParam(params, "sourceDocumentCode", "sourceCode", "purchaseOrderCode"))
                && contains(row.remark(), params.get("remark"))
                && matchItem(row, params.get("itemCode"));
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

    private boolean matchItem(PurchaseDocumentView row, String itemCode) {
        if (!StringUtils.hasText(itemCode)) {
            return true;
        }
        return row.items().stream().anyMatch(item -> itemCode.equals(item.itemCode()));
    }

    private InventoryDocumentHeader toWorkflowHeader(Header header) {
        InventoryDocumentHeader workflowHeader = new InventoryDocumentHeader();
        workflowHeader.setId(header.id);
        workflowHeader.setScopeType(header.scopeType);
        workflowHeader.setScopeId(header.scopeId);
        workflowHeader.setDocumentCode(header.documentCode);
        workflowHeader.setDocumentDate(header.documentDate);
        workflowHeader.setStatus(header.documentStatus);
        workflowHeader.setWorkflowProcessCode(header.workflowProcessCode);
        workflowHeader.setWorkflowDefinitionKey(header.workflowDefinitionKey);
        workflowHeader.setWorkflowDefinitionId(header.workflowDefinitionId);
        workflowHeader.setWorkflowInstanceId(header.workflowInstanceId);
        workflowHeader.setWorkflowTaskId(header.workflowTaskId);
        workflowHeader.setWorkflowTaskName(header.workflowTaskName);
        workflowHeader.setWorkflowStatus(header.workflowStatus);
        workflowHeader.setPendingOperation(header.pendingOperation);
        workflowHeader.setRemark(header.remark);
        workflowHeader.setRejectionReason(header.rejectionReason);
        workflowHeader.setCreatedBy(header.createdBy);
        workflowHeader.setApprovedBy(header.approvedBy);
        workflowHeader.setApprovedAt(header.approvedAt);
        return workflowHeader;
    }

    private void applyWorkflowHeader(Header header, InventoryDocumentHeader workflowHeader) {
        header.workflowProcessCode = workflowHeader.getWorkflowProcessCode();
        header.workflowDefinitionKey = workflowHeader.getWorkflowDefinitionKey();
        header.workflowDefinitionId = workflowHeader.getWorkflowDefinitionId();
        header.workflowInstanceId = workflowHeader.getWorkflowInstanceId();
        header.workflowTaskId = workflowHeader.getWorkflowTaskId();
        header.workflowTaskName = workflowHeader.getWorkflowTaskName();
        header.workflowStatus = workflowHeader.getWorkflowStatus();
        header.pendingOperation = workflowHeader.getPendingOperation();
        header.rejectionReason = workflowHeader.getRejectionReason();
        header.approvedBy = workflowHeader.getApprovedBy();
        header.approvedAt = workflowHeader.getApprovedAt();
    }

    private RowMapper<Header> headerMapper() {
        return (rs, rowNum) -> {
            Header row = new Header();
            row.id = rs.getLong("id");
            row.scopeType = rs.getString("scope_type");
            row.scopeId = rs.getLong("scope_id");
            row.documentType = rs.getString("document_type");
            row.documentCode = rs.getString("document_code");
            row.documentDate = toLocalDate(rs, "document_date");
            row.expectedArrivalDate = toLocalDate(rs, "expected_arrival_date");
            row.purchaseOrg = rs.getString("purchase_org");
            row.warehouseName = rs.getString("warehouse_name");
            row.supplierName = rs.getString("supplier_name");
            row.sourceDocumentCode = rs.getString("source_document_code");
            row.downstreamDocumentCode = rs.getString("downstream_document_code");
            row.documentStatus = rs.getString("document_status");
            row.reviewStatus = rs.getString("review_status");
            row.shipStatus = rs.getString("ship_status");
            row.receiveStatus = rs.getString("receive_status");
            row.reconciliationStatus = rs.getString("reconciliation_status");
            row.supplierSplitStatus = rs.getString("supplier_split_status");
            row.documentBizType = rs.getString("document_biz_type");
            row.splitReceiptStatus = rs.getString("split_receipt_status");
            row.printStatus = rs.getString("print_status");
            row.returnReason = rs.getString("return_reason");
            row.inspectionStatus = rs.getString("inspection_status");
            row.inspectionCount = rs.getObject("inspection_count", Integer.class);
            row.adjustedPrice = rs.getBoolean("adjusted_price");
            row.workflowProcessCode = rs.getString("workflow_process_code");
            row.workflowDefinitionKey = rs.getString("workflow_definition_key");
            row.workflowDefinitionId = rs.getString("workflow_definition_id");
            row.workflowInstanceId = rs.getString("workflow_instance_id");
            row.workflowTaskId = rs.getString("workflow_task_id");
            row.workflowTaskName = rs.getString("workflow_task_name");
            row.workflowStatus = rs.getString("workflow_status");
            row.pendingOperation = rs.getString("pending_operation");
            row.rejectionReason = rs.getString("rejection_reason");
            row.creatorName = rs.getString("creator_name");
            row.submitterName = rs.getString("submitter_name");
            row.applicantName = rs.getString("applicant_name");
            row.lastOperatorName = rs.getString("last_operator_name");
            row.lastOperatedAt = toLocalDateTime(rs, "last_operated_at");
            row.remark = rs.getString("remark");
            row.createdBy = rs.getObject("created_by", Long.class);
            row.approvedBy = rs.getObject("approved_by", Long.class);
            row.approvedAt = toLocalDateTime(rs, "approved_at");
            row.createdAt = toLocalDateTime(rs, "created_at");
            row.updatedAt = toLocalDateTime(rs, "updated_at");
            return row;
        };
    }

    private RowMapper<Line> lineMapper() {
        return (rs, rowNum) -> {
            Line row = new Line();
            row.id = rs.getLong("id");
            row.documentId = rs.getLong("document_id");
            row.itemCode = rs.getString("item_code");
            row.itemName = rs.getString("item_name");
            row.spec = rs.getString("spec");
            row.itemCategory = rs.getString("item_category");
            row.supplierName = rs.getString("supplier_name");
            row.purchaseUnit = rs.getString("purchase_unit");
            row.baseUnit = rs.getString("base_unit");
            row.baseConversion = rs.getString("base_conversion");
            row.quantity = decimal(rs, "quantity");
            row.reviewQty = decimal(rs, "review_qty");
            row.receivedQty = decimal(rs, "received_qty");
            row.unitPrice = decimal(rs, "unit_price");
            row.taxRate = decimal(rs, "tax_rate");
            row.amount = decimal(rs, "amount");
            row.isGift = rs.getBoolean("is_gift");
            row.warehouseName = rs.getString("warehouse_name");
            row.expectedArrivalDate = toLocalDate(rs, "expected_arrival_date");
            row.reviewStatus = rs.getString("review_status");
            row.remark = rs.getString("remark");
            return row;
        };
    }

    private DocumentKind resolveKind(String value) {
        String normalized = trimNullable(value);
        if ("applications".equals(normalized) || "application-reviews".equals(normalized) || "APPLICATION".equals(normalized)) {
            return DocumentKind.APPLICATION;
        }
        if ("orders".equals(normalized) || "ORDER".equals(normalized)) {
            return DocumentKind.ORDER;
        }
        if ("receipts".equals(normalized) || "RECEIPT".equals(normalized)) {
            return DocumentKind.RECEIPT;
        }
        if ("returns".equals(normalized) || "RETURN".equals(normalized)) {
            return DocumentKind.RETURN;
        }
        throw new BusinessException("采购单据类型非法");
    }

    private OrgScopeService.AccessibleScope resolveScope(String orgId) {
        Long userId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return orgScopeService.resolveAccessibleScope(userId, orgId);
    }

    private String generateDocumentCode(OrgScopeService.AccessibleScope scope, DocumentKind kind) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = kind.codePrefix() + "-" + datePart + "-";
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM purchase_document
                WHERE scope_type = :scopeType
                  AND scope_id = :scopeId
                  AND document_type = :documentType
                  AND document_code LIKE :prefix
                """, new MapSqlParameterSource()
                .addValue("scopeType", scope.scopeType())
                .addValue("scopeId", scope.scopeId())
                .addValue("documentType", kind.storageType())
                .addValue("prefix", prefix + "%"), Integer.class);
        return prefix + String.format(Locale.ROOT, "%03d", (count == null ? 0 : count) + 1);
    }

    private void ensureEditable(Header header) {
        if ("已审核".equals(header.documentStatus)) {
            throw new BusinessException("已审核单据不允许编辑，请先反审核");
        }
    }

    private String terminalApprovedStatus(DocumentKind kind) {
        if (kind == DocumentKind.RECEIPT) {
            return "已收货";
        }
        return "已审核";
    }

    private String defaultBizType(DocumentKind kind) {
        return switch (kind) {
            case APPLICATION -> "手工创建";
            case ORDER -> "普通采购";
            case RECEIPT -> "采购订单收货";
            case RETURN -> "普通退货";
        };
    }

    private String defaultShipStatus(DocumentKind kind) {
        return kind == DocumentKind.RECEIPT ? "未发货" : "";
    }

    private String defaultReceiveStatus(DocumentKind kind) {
        return kind == DocumentKind.ORDER || kind == DocumentKind.RECEIPT ? "未收货" : "";
    }

    private LocalDate parseRequiredDate(String value, String message) {
        LocalDate parsed = parseDateNullable(value);
        if (parsed == null) {
            throw new BusinessException(message);
        }
        return parsed;
    }

    private LocalDate parseDateNullable(String value) {
        String text = trimNullable(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式不正确");
        }
    }

    private BigDecimal positive(BigDecimal value, String message) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
        return normalized;
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

    private String requiredTrim(String value, String message) {
        String text = trimNullable(value);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(message);
        }
        return text;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean contains(String value, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return defaultIfBlank(value, "").toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean equalsIfPresent(String value, String expected) {
        return !StringUtils.hasText(expected) || Objects.equals(value, expected);
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

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATETIME_FORMATTER.format(value);
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private enum DocumentKind {
        APPLICATION("applications", "APPLICATION", "PURCHASE_APPLICATION", "采购单申请", "PA", "/purchase/application-reviews"),
        ORDER("orders", "ORDER", "PURCHASE_ORDER", "采购订单", "PO", "/purchase/orders"),
        RECEIPT("receipts", "RECEIPT", "PURCHASE_RECEIPT", "采购收货单", "PRC", "/purchase/receipts"),
        RETURN("returns", "RETURN", "PURCHASE_RETURN", "采购退货单", "PRT", "/purchase/returns");

        private final String routeKey;
        private final String storageType;
        private final String businessCode;
        private final String businessName;
        private final String codePrefix;
        private final String routePath;

        DocumentKind(String routeKey, String storageType, String businessCode, String businessName, String codePrefix, String routePath) {
            this.routeKey = routeKey;
            this.storageType = storageType;
            this.businessCode = businessCode;
            this.businessName = businessName;
            this.codePrefix = codePrefix;
            this.routePath = routePath;
        }

        String routeKey() { return routeKey; }
        String storageType() { return storageType; }
        String businessCode() { return businessCode; }
        String businessName() { return businessName; }
        String codePrefix() { return codePrefix; }
        String routePath() { return routePath; }
    }

    private interface HeaderMutator {
        void apply(Header header);
    }

    private static final class Header {
        Long id;
        String scopeType;
        Long scopeId;
        String documentType;
        String documentCode;
        LocalDate documentDate;
        LocalDate expectedArrivalDate;
        String purchaseOrg;
        String warehouseName;
        String supplierName;
        String sourceDocumentCode;
        String downstreamDocumentCode;
        String documentStatus;
        String reviewStatus;
        String shipStatus;
        String receiveStatus;
        String reconciliationStatus;
        String supplierSplitStatus;
        String documentBizType;
        String splitReceiptStatus;
        String printStatus;
        String returnReason;
        String inspectionStatus;
        Integer inspectionCount = 0;
        Boolean adjustedPrice = false;
        String workflowProcessCode;
        String workflowDefinitionKey;
        String workflowDefinitionId;
        String workflowInstanceId;
        String workflowTaskId;
        String workflowTaskName;
        String workflowStatus;
        String pendingOperation;
        String rejectionReason;
        String creatorName;
        String submitterName;
        String applicantName;
        String lastOperatorName;
        LocalDateTime lastOperatedAt;
        String remark;
        Long createdBy;
        Long approvedBy;
        LocalDateTime approvedAt;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    private static final class Line {
        Long id;
        Long documentId;
        String itemCode;
        String itemName;
        String spec;
        String itemCategory;
        String supplierName;
        String purchaseUnit;
        String baseUnit;
        String baseConversion;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal reviewQty = BigDecimal.ZERO;
        BigDecimal receivedQty = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        Boolean isGift = false;
        String warehouseName;
        LocalDate expectedArrivalDate;
        String reviewStatus;
        String remark;
    }

    public record IdPayload(Long id, String documentCode) {
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    public record PurchaseDocumentView(Long id,
                                       String documentType,
                                       String documentCode,
                                       String documentDate,
                                       String expectedArrivalDate,
                                       String purchaseOrg,
                                       String warehouse,
                                       String supplier,
                                       String sourceDocumentCode,
                                       String downstreamDocumentCode,
                                       String documentStatus,
                                       String reviewStatus,
                                       String shipStatus,
                                       String receiveStatus,
                                       String reconciliationStatus,
                                       String supplierSplit,
                                       String documentBizType,
                                       String splitReceipt,
                                       String printStatus,
                                       String returnReason,
                                       String inspectionStatus,
                                       int inspectionCount,
                                       boolean adjustedPrice,
                                       String creator,
                                       String submitter,
                                       String applicant,
                                       String lastOperator,
                                       String lastOperatedAt,
                                       String remark,
                                       String rejectionReason,
                                       BigDecimal amount,
                                       int itemCount,
                                       String workflowStatus,
                                       String workflowTaskName,
                                       List<PurchaseDocumentLineView> items,
                                       String createdAt) {
    }

    public record PurchaseDocumentLineView(Long id,
                                           String itemCode,
                                           String itemName,
                                           String spec,
                                           String itemCategory,
                                           String supplier,
                                           String purchaseUnit,
                                           String baseUnit,
                                           String baseConversion,
                                           BigDecimal quantity,
                                           BigDecimal reviewQty,
                                           BigDecimal receivedQty,
                                           BigDecimal unitPrice,
                                           BigDecimal taxRate,
                                           BigDecimal amount,
                                           boolean isGift,
                                           String warehouse,
                                           String expectedArrivalDate,
                                           String reviewStatus,
                                           String remark) {
    }

    public record SavePurchaseDocumentRequest(String documentDate,
                                              String expectedArrivalDate,
                                              String purchaseOrg,
                                              String warehouse,
                                              String supplier,
                                              String sourceDocumentCode,
                                              String downstreamDocumentCode,
                                              String documentBizType,
                                              String shipStatus,
                                              String receiveStatus,
                                              String reconciliationStatus,
                                              String supplierSplit,
                                              String splitReceipt,
                                              String printStatus,
                                              String returnReason,
                                              String inspectionStatus,
                                              Boolean adjustedPrice,
                                              String applicant,
                                              String submitter,
                                              String remark,
                                              List<LineRequest> items) {
    }

    public record LineRequest(String itemCode,
                              String itemName,
                              String spec,
                              String itemCategory,
                              String supplier,
                              String purchaseUnit,
                              String baseUnit,
                              String baseConversion,
                              BigDecimal quantity,
                              BigDecimal reviewQty,
                              BigDecimal receivedQty,
                              BigDecimal unitPrice,
                              BigDecimal taxRate,
                              BigDecimal amount,
                              Boolean isGift,
                              String warehouse,
                              String expectedArrivalDate,
                              String reviewStatus,
                              String remark) {
    }

    public record BatchActionRequest(List<Long> ids, String rejectionReason) {
    }

    public record LineReviewBatchRequest(Boolean approved,
                                         String rejectionReason,
                                         List<LineReviewRequest> lineReviews) {
    }

    public record LineReviewRequest(Long lineId, BigDecimal reviewQty, String remark) {
    }

    public record LineUpdateBatchRequest(List<LineUpdateRequest> lineUpdates) {
    }

    public record LineUpdateRequest(Long lineId, String supplier, String expectedArrivalDate, BigDecimal reviewQty, String remark) {
    }
}
