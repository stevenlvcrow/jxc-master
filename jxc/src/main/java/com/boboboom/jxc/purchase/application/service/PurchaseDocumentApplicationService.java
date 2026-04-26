package com.boboboom.jxc.purchase.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentWorkflowService;
import com.boboboom.jxc.purchase.domain.repository.PurchaseDocumentRepository;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowApprovalNotificationApplicationService;

/** 采购单据业务服务，负责采购申请、订货与入库链路的单据编排。 */
@Service
public class PurchaseDocumentApplicationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String REVIEW_STATUS_APPROVED = "已审核";
    private static final String REVIEW_STATUS_UNAPPROVED = "未审核";

    private final PurchaseDocumentRepository purchaseDocumentRepository;
    private final OrgScopeService orgScopeService;
    private final DataScopeAccessService dataScopeAccessService;
    private final InventoryDocumentWorkflowService workflowService;
    private final WorkflowApprovalNotificationApplicationService notificationService;

    /** 采购单据业务服务，负责采购申请、订货与入库链路的单据编排。 */
    public PurchaseDocumentApplicationService(PurchaseDocumentRepository purchaseDocumentRepositoryValue,
                                              OrgScopeService orgScopeServiceValue,
                                              DataScopeAccessService dataScopeAccessServiceValue,
                                              InventoryDocumentWorkflowService workflowServiceValue,
                                              WorkflowApprovalNotificationApplicationService notificationServiceValue) {
        this.purchaseDocumentRepository = purchaseDocumentRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
        this.workflowService = workflowServiceValue;
        this.notificationService = notificationServiceValue;
    }

    /** 分页查询业务数据。 */
    public PageData<PurchaseDocumentView> page(String rawDocumentType, Map<String, String> params) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(params.get("orgId"));
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = canViewAll(scope, operatorId);
        int pageNo = parsePositiveInt(params.get("pageNo"), 1);
        int pageSize = Math.min(parsePositiveInt(params.get("pageSize"), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        List<Header> headers = loadHeaders(scope, kind.storageType(), operatorId, viewAll);
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

    /** 查询业务详情。 */
    public PurchaseDocumentView detail(String rawDocumentType, Long id, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean viewAll = canViewAll(scope, operatorId) || canReview(scope, kind, operatorId);
        Header header = requireHeader(scope, kind.storageType(), id, operatorId, viewAll);
        return toView(header, loadLines(header.id), kind);
    }

    /** 创建业务记录。 */
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
        header.reviewStatus = REVIEW_STATUS_UNAPPROVED;
        header.workflowStatus = "NONE";
        header.pendingOperation = "NONE";
        header.createdBy = operatorId;
        header.creatorName = AuthContextHolder.userNameOr("system");
        applyRequest(header, request, kind, true);
        header.id = insertHeader(header);
        replaceLines(header.id, request.items());
        return new IdPayload(header.id, header.documentCode);
    }

    /** 更新业务记录。 */
    @Transactional
    public void update(String rawDocumentType, Long id, SavePurchaseDocumentRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        Header header = requireHeader(scope, kind.storageType(), id, operatorId, canViewAll(scope, operatorId));
        ensureEditable(header);
        applyRequest(header, request, kind, false);
        header.updatedAt = LocalDateTime.now();
        updateHeader(header);
        replaceLines(header.id, request.items());
    }

    /** 删除业务记录。 */
    @Transactional
    public void delete(String rawDocumentType, Long id, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        Header header = requireHeader(scope, kind.storageType(), id, operatorId, canViewAll(scope, operatorId));
        ensureEditable(header);
        purchaseDocumentRepository.deleteHeaderById(header.id);
    }

    /** 批量删除业务记录。 */
    @Transactional
    public void batchDelete(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, canViewAll(scope, operatorId))) {
            ensureEditable(header);
            purchaseDocumentRepository.deleteHeaderById(header.id);
        }
    }

    /** 批量提交采购单据。 */
    @Transactional
    public void batchSubmit(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, canViewAll(scope, operatorId))) {
            if ("已审核".equals(header.documentStatus)) {
                continue;
            }
            header.documentStatus = "已提交";
            header.reviewStatus = REVIEW_STATUS_UNAPPROVED;
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

    /** 批量审核通过单据。 */
    @Transactional
    public void batchApprove(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean reviewAccess = canViewAll(scope, operatorId) || canReview(scope, kind, operatorId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, reviewAccess)) {
            approveHeader(scope, kind, header, operatorId);
        }
    }

    /** 批量驳回采购单据。 */
    @Transactional
    public void batchReject(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String reason = requiredTrim(request.rejectionReason(), "驳回原因不能为空");
        boolean reviewAccess = canViewAll(scope, operatorId) || canReview(scope, kind, operatorId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, reviewAccess)) {
            rejectHeader(scope, kind, header, operatorId, reason);
        }
    }

    /** 批量撤销单据审核。 */
    @Transactional
    public void batchUnapprove(String rawDocumentType, BatchActionRequest request, String orgId) {
        DocumentKind kind = resolveKind(rawDocumentType);
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean reviewAccess = canViewAll(scope, operatorId) || canReview(scope, kind, operatorId);
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, reviewAccess)) {
            header.documentStatus = "已提交";
            header.reviewStatus = REVIEW_STATUS_UNAPPROVED;
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

    /** 批量标记采购单据打印。 */
    @Transactional
    public void batchPrint(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.printStatus = "已打印");
    }

    /** 批量关闭采购单据。 */
    @Transactional
    public void batchClose(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.documentStatus = "已关闭");
    }

    /** 批量取消关闭采购单据。 */
    @Transactional
    public void batchCancelClose(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> header.documentStatus = "已提交");
    }

    /** 批量确认采购单据收货。 */
    @Transactional
    public void batchReceive(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> {
            header.receiveStatus = "已收货";
            header.shipStatus = "已发货";
            header.documentStatus = "已收货";
        });
    }

    /** 批量取消采购单据收货。 */
    @Transactional
    public void batchCancelReceive(String rawDocumentType, BatchActionRequest request, String orgId) {
        updateSimpleStatus(rawDocumentType, request, orgId, header -> {
            header.receiveStatus = "未收货";
            header.documentStatus = "待收货";
        });
    }

    /** 查询采购申请审核明细。 */
    @Transactional
    public void reviewApplicationLines(LineReviewBatchRequest request, String orgId) {
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        requireLineReviews(request);
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
        boolean reviewAccess = canViewAll(scope, operatorId) || canReview(scope, DocumentKind.APPLICATION, operatorId);
        List<Header> headers = requireHeaders(scope, "APPLICATION", new ArrayList<>(documentIds), operatorId, reviewAccess);
        Map<Long, Header> headerMap = headers.stream().collect(Collectors.toMap(item -> item.id, item -> item));
        boolean approved = Boolean.TRUE.equals(request.approved());
        applyLineReviewResults(lines, headerMap, reviewMap, approved);
        if (!approved) {
            String reason = requiredTrim(request.rejectionReason(), "驳回原因不能为空");
            for (Header header : headers) {
                rejectHeader(scope, DocumentKind.APPLICATION, header, operatorId, reason);
            }
            return;
        }
        approveFullyReviewedHeaders(scope, headers, operatorId);
    }

    private void applyLineReviewResults(List<Line> lines,
                                        Map<Long, Header> headerMap,
                                        Map<Long, LineReviewRequest> reviewMap,
                                        boolean approved) {
        for (Line line : lines) {
            if (!headerMap.containsKey(line.documentId)) {
                throw new BusinessException("存在无效明细");
            }
            LineReviewRequest review = reviewMap.get(line.id);
            BigDecimal reviewQty = review.reviewQty() == null ? line.quantity : review.reviewQty();
            updateLineReview(line.id, approved ? REVIEW_STATUS_APPROVED : REVIEW_STATUS_UNAPPROVED,
                    reviewQty, trimNullable(review.remark()));
        }
    }

    private void approveFullyReviewedHeaders(OrgScopeService.AccessibleScope scope, List<Header> headers, Long operatorId) {
        for (Header header : headers) {
            List<Line> currentLines = loadLines(header.id);
            boolean allApproved = currentLines.stream().allMatch(line -> REVIEW_STATUS_APPROVED.equals(line.reviewStatus));
            if (allApproved) {
                approveHeader(scope, DocumentKind.APPLICATION, header, operatorId);
            }
        }
    }

    private void requireLineReviews(LineReviewBatchRequest request) {
        if (request.lineReviews() == null || request.lineReviews().isEmpty()) {
            throw new BusinessException("请选择需要审核的明细");
        }
    }

    /** 更新采购申请明细。 */
    @Transactional
    public void updateApplicationLines(LineUpdateBatchRequest request, String orgId) {
        OrgScopeService.AccessibleScope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
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
        boolean reviewAccess = canViewAll(scope, operatorId) || canReview(scope, DocumentKind.APPLICATION, operatorId);
        List<Header> headers = requireHeaders(scope, "APPLICATION", new ArrayList<>(documentIds), operatorId, reviewAccess);
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
        purchaseDocumentRepository.updateLastOperator(new ArrayList<>(documentIds), AuthContextHolder.userNameOr("system"));
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
            header.reviewStatus = REVIEW_STATUS_APPROVED;
            header.approvedBy = operatorId;
            header.approvedAt = LocalDateTime.now();
        } else {
            header.documentStatus = "已提交";
            header.reviewStatus = REVIEW_STATUS_UNAPPROVED;
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
        header.reviewStatus = REVIEW_STATUS_UNAPPROVED;
        header.rejectionReason = reason;
        header.lastOperatorName = AuthContextHolder.userNameOr("system");
        header.lastOperatedAt = LocalDateTime.now();
        header.updatedAt = LocalDateTime.now();
        InventoryDocumentHeader workflowHeader = toWorkflowHeader(header);
        InventoryDocumentWorkflowService.ApprovalResult result = workflowService.rejectBusinessCurrentTask(
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
            throw new BusinessException(kind.businessName() + "未发起审批流，不能驳回");
        }
        applyWorkflowHeader(header, workflowHeader);
        updateHeader(header);
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
                AuthContextHolder.userIdOr(null),
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
                AuthContextHolder.userIdOr(null),
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
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        for (Header header : requireHeaders(scope, kind.storageType(), request.ids(), operatorId, canViewAll(scope, operatorId))) {
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
        header.documentBizType = defaultIfBlank(request.documentBizType(), defaultBizType(kind));
        header.shipStatus = defaultIfBlank(request.shipStatus(), defaultShipStatus(kind));
        header.receiveStatus = defaultIfBlank(request.receiveStatus(), defaultReceiveStatus(kind));
        header.reconciliationStatus = defaultIfBlank(request.reconciliationStatus(), "未对账");
        header.supplierSplitStatus = defaultIfBlank(request.supplierSplit(), "未分账");
        header.splitReceiptStatus = defaultIfBlank(request.splitReceipt(), "不拆分");
        header.printStatus = defaultIfBlank(request.printStatus(), "未打印");
        header.returnReason = trimNullable(request.returnReason());
        header.inspectionStatus = defaultIfBlank(request.inspectionStatus(), "无需质检");
        header.adjustedPrice = Boolean.TRUE.equals(request.adjustedPrice());
        header.applicantName = defaultIfBlank(request.applicant(), AuthContextHolder.userNameOr("system"));
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
        Long id = purchaseDocumentRepository.saveHeader(header);
        if (id == null) {
            throw new BusinessException("采购单据保存失败");
        }
        return id;
    }

    private void updateHeader(Header header) {
        purchaseDocumentRepository.updateHeader(header);
    }

    private void replaceLines(Long documentId, List<LineRequest> items) {
        purchaseDocumentRepository.deleteLinesByDocumentId(documentId);
        for (LineRequest item : items) {
            BigDecimal quantity = positive(item.quantity(), "数量必须大于0");
            BigDecimal price = nonNegative(item.unitPrice(), "单价不能小于0");
            BigDecimal amount = item.amount() == null ? quantity.multiply(price) : nonNegative(item.amount(), "金额不能小于0");
            Line line = new Line();
            line.documentId = documentId;
            line.itemCode = requiredTrim(item.itemCode(), "物品编码不能为空");
            line.itemName = requiredTrim(item.itemName(), "物品名称不能为空");
            line.spec = trimNullable(item.spec());
            line.itemCategory = trimNullable(item.itemCategory());
            line.supplierName = trimNullable(item.supplier());
            line.purchaseUnit = trimNullable(item.purchaseUnit());
            line.baseUnit = trimNullable(item.baseUnit());
            line.baseConversion = trimNullable(item.baseConversion());
            line.quantity = quantity;
            line.reviewQty = item.reviewQty() == null ? quantity : nonNegative(item.reviewQty(), "审核数量不能小于0");
            line.receivedQty = item.receivedQty() == null ? BigDecimal.ZERO : nonNegative(item.receivedQty(), "收货数量不能小于0");
            line.unitPrice = price;
            line.taxRate = item.taxRate() == null ? BigDecimal.ZERO : nonNegative(item.taxRate(), "税率不能小于0");
            line.amount = amount;
            line.isGift = Boolean.TRUE.equals(item.isGift());
            line.warehouseName = trimNullable(item.warehouse());
            line.expectedArrivalDate = parseDateNullable(item.expectedArrivalDate());
            line.reviewStatus = normalizeReviewStatus(item.reviewStatus());
            line.remark = trimNullable(item.remark());
            purchaseDocumentRepository.saveLine(line);
        }
    }

    private List<Header> loadHeaders(OrgScopeService.AccessibleScope scope,
                                     String documentType,
                                     Long operatorId,
                                     boolean viewAll) {
        return purchaseDocumentRepository.findHeaders(scope.scopeType(), scope.scopeId(), documentType, operatorId, viewAll);
    }

    private Header requireHeader(OrgScopeService.AccessibleScope scope,
                                 String documentType,
                                 Long id,
                                 Long operatorId,
                                 boolean viewAll) {
        if (id == null) {
            throw new BusinessException("单据ID不能为空");
        }
        return purchaseDocumentRepository.findHeader(scope.scopeType(), scope.scopeId(), documentType, id, operatorId, viewAll)
                .orElseThrow(() -> new BusinessException("单据不存在"));
    }

    private List<Header> requireHeaders(OrgScopeService.AccessibleScope scope,
                                        String documentType,
                                        List<Long> ids,
                                        Long operatorId,
                                        boolean viewAll) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择单据");
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException("请选择单据");
        }
        List<Header> rows = purchaseDocumentRepository.findHeadersByIds(
                scope.scopeType(),
                scope.scopeId(),
                documentType,
                distinctIds,
                operatorId,
                viewAll
        );
        if (rows.size() != distinctIds.size()) {
            throw new BusinessException("存在无效单据");
        }
        return rows;
    }

    private boolean canViewAll(OrgScopeService.AccessibleScope scope, Long operatorId) {
        return dataScopeAccessService.canViewScopeData(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
    }

    private boolean canReview(OrgScopeService.AccessibleScope scope, DocumentKind kind, Long operatorId) {
        return workflowService.hasBusinessReviewPermission(
                kind.businessCode(),
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
    }

    private Map<Long, List<Line>> loadLineMap(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Map.of();
        }
        return purchaseDocumentRepository.findLinesByDocumentIds(documentIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.documentId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<Line> loadLines(Long documentId) {
        return purchaseDocumentRepository.findLinesByDocumentId(documentId);
    }

    private List<Line> loadLinesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return purchaseDocumentRepository.findLinesByIds(ids);
    }

    private void updateLineReview(Long lineId, String reviewStatus, BigDecimal reviewQty, String remark) {
        purchaseDocumentRepository.updateLineReview(lineId, reviewStatus, reviewQty, remark);
    }

    private void updateLinePatch(Long lineId, String supplier, LocalDate expectedArrivalDate, BigDecimal reviewQty, String remark) {
        purchaseDocumentRepository.updateLinePatch(lineId, supplier, expectedArrivalDate, reviewQty, remark);
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
                defaultIfBlank(header.reviewStatus, REVIEW_STATUS_UNAPPROVED),
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
                defaultIfBlank(line.reviewStatus, REVIEW_STATUS_UNAPPROVED),
                defaultIfBlank(line.remark, "")
        );
    }

    private boolean matches(PurchaseDocumentView row, Map<String, String> params) {
        LocalDate start = parseDateNullable(firstParam(params, "startDate", "dateStart"));
        LocalDate end = parseDateNullable(firstParam(params, "endDate", "dateEnd"));
        LocalDate date = parseDateNullable(row.documentDate());
        return matchDate(date, start, end)
                && matchesPurchaseDocumentText(row, params)
                && matchesPurchaseDocumentStatus(row, params)
                && matchesPurchaseDocumentExtra(row, params);
    }

    private boolean matchesPurchaseDocumentText(PurchaseDocumentView row, Map<String, String> params) {
        return contains(row.documentCode(), firstParam(params, "documentCode", "applicationCode", "receiptCode"))
                && equalsIfPresent(row.warehouse(), firstParam(params, "warehouse", "receiptWarehouse", "returnWarehouse"))
                && equalsIfPresent(row.supplier(), params.get("supplier"))
                && contains(row.sourceDocumentCode(), firstParam(params, "sourceDocumentCode", "sourceCode", "purchaseOrderCode"))
                && contains(row.remark(), params.get("remark"));
    }

    private boolean matchesPurchaseDocumentStatus(PurchaseDocumentView row, Map<String, String> params) {
        return equalsIfPresent(row.documentStatus(), params.get("documentStatus"))
                && equalsIfPresent(row.reviewStatus(), params.get("reviewStatus"))
                && equalsIfPresent(row.shipStatus(), params.get("shipStatus"))
                && equalsIfPresent(row.receiveStatus(), params.get("receiveStatus"))
                && equalsIfPresent(row.reconciliationStatus(), params.get("reconciliationStatus"));
    }

    private boolean matchesPurchaseDocumentExtra(PurchaseDocumentView row, Map<String, String> params) {
        return equalsIfPresent(row.supplierSplit(), params.get("supplierSplit"))
                && equalsIfPresent(row.documentBizType(), params.get("documentType"))
                && equalsIfPresent(row.returnReason(), params.get("returnReason"))
                && equalsIfPresent(row.inspectionStatus(), params.get("inspectionStatus"))
                && matchesPrintStatus(row, params.get("printStatus"))
                && matchItem(row, params.get("itemCode"));
    }

    private boolean matchesPrintStatus(PurchaseDocumentView row, String printStatus) {
        return "全部".equals(defaultIfBlank(printStatus, "全部")) || equalsIfPresent(row.printStatus(), printStatus);
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
        Long count = purchaseDocumentRepository.countByCodePrefix(scope.scopeType(), scope.scopeId(), kind.storageType(), prefix + "%");
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

    private String normalizeReviewStatus(String value) {
        String text = trimNullable(value);
        if (!StringUtils.hasText(text)) {
            return REVIEW_STATUS_UNAPPROVED;
        }
        if (REVIEW_STATUS_APPROVED.equals(text) || REVIEW_STATUS_UNAPPROVED.equals(text)) {
            return text;
        }
        throw new BusinessException("审核状态只能为已审核或未审核");
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

        DocumentKind(String routeKeyValue, String storageTypeValue, String businessCodeValue, String businessNameValue, String codePrefixValue, String routePathValue) {
            this.routeKey = routeKeyValue;
            this.storageType = storageTypeValue;
            this.businessCode = businessCodeValue;
            this.businessName = businessNameValue;
            this.codePrefix = codePrefixValue;
            this.routePath = routePathValue;
        }

        String routeKey() {
            return routeKey;
        }

        String storageType() {
            return storageType;
        }

        String businessCode() {
            return businessCode;
        }

        String businessName() {
            return businessName;
        }

        String codePrefix() {
            return codePrefix;
        }

        String routePath() {
            return routePath;
        }
    }

    private interface HeaderMutator {
        void apply(Header header);
    }

    /**
     * 采购单据头业务数据。
     */
    public static final class Header {
        public Long id;
        public String scopeType;
        public Long scopeId;
        public String documentType;
        public String documentCode;
        public LocalDate documentDate;
        public LocalDate expectedArrivalDate;
        public String purchaseOrg;
        public String warehouseName;
        public String supplierName;
        public String sourceDocumentCode;
        public String downstreamDocumentCode;
        public String documentStatus;
        public String reviewStatus;
        public String shipStatus;
        public String receiveStatus;
        public String reconciliationStatus;
        public String supplierSplitStatus;
        public String documentBizType;
        public String splitReceiptStatus;
        public String printStatus;
        public String returnReason;
        public String inspectionStatus;
        public Integer inspectionCount = 0;
        public Boolean adjustedPrice = false;
        public String workflowProcessCode;
        public String workflowDefinitionKey;
        public String workflowDefinitionId;
        public String workflowInstanceId;
        public String workflowTaskId;
        public String workflowTaskName;
        public String workflowStatus;
        public String pendingOperation;
        public String rejectionReason;
        public String creatorName;
        public String submitterName;
        public String applicantName;
        public String lastOperatorName;
        public LocalDateTime lastOperatedAt;
        public String remark;
        public Long createdBy;
        public Long approvedBy;
        public LocalDateTime approvedAt;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    /**
     * 采购单据行业务数据。
     */
    public static final class Line {
        public Long id;
        public Long documentId;
        public String itemCode;
        public String itemName;
        public String spec;
        public String itemCategory;
        public String supplierName;
        public String purchaseUnit;
        public String baseUnit;
        public String baseConversion;
        public BigDecimal quantity = BigDecimal.ZERO;
        public BigDecimal reviewQty = BigDecimal.ZERO;
        public BigDecimal receivedQty = BigDecimal.ZERO;
        public BigDecimal unitPrice = BigDecimal.ZERO;
        public BigDecimal taxRate = BigDecimal.ZERO;
        public BigDecimal amount = BigDecimal.ZERO;
        public Boolean isGift = false;
        public String warehouseName;
        public LocalDate expectedArrivalDate;
        public String reviewStatus;
        public String remark;
    }

    /** 采购载荷模型，承载接口返回的关键标识。 */
    public record IdPayload(Long id, String documentCode) {
    }

    /** 采购分页数据模型，承载列表数据和分页信息。 */
    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    /** 采购视图模型，承载页面展示数据。 */
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

    /** 采购视图模型，承载页面展示数据。 */
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

    /** 采购请求参数，承载接口入参。 */
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

    /** 采购请求参数，承载接口入参。 */
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

    /** 采购请求参数，承载接口入参。 */
    public record BatchActionRequest(List<Long> ids, String rejectionReason) {
    }

    /** 采购请求参数，承载接口入参。 */
    public record LineReviewBatchRequest(Boolean approved,
                                         String rejectionReason,
                                         List<LineReviewRequest> lineReviews) {
    }

    /** 采购请求参数，承载接口入参。 */
    public record LineReviewRequest(Long lineId, BigDecimal reviewQty, String remark) {
    }

    /** 采购请求参数，承载接口入参。 */
    public record LineUpdateBatchRequest(List<LineUpdateRequest> lineUpdates) {
    }

    /** 采购请求参数，承载接口入参。 */
    public record LineUpdateRequest(Long lineId, String supplier, String expectedArrivalDate, BigDecimal reviewQty, String remark) {
    }
}
