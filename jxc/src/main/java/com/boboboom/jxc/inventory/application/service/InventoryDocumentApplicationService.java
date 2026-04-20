package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentSaveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 通用库存单据业务服务。
 */
@Service
public class InventoryDocumentApplicationService {

    private static final String STATUS_DRAFT = "草稿";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_APPROVED = "已审核";
    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final InventoryDocumentPermissionService inventoryDocumentPermissionService;
    private final InventoryDocumentNotificationService inventoryDocumentNotificationService;
    private final InventoryDocumentWorkflowService inventoryDocumentWorkflowService;
    private final OrgScopeService orgScopeService;
    private final ObjectMapper objectMapper;

    public InventoryDocumentApplicationService(InventoryDocumentRepository inventoryDocumentRepository,
                                               InventoryStockMutationService inventoryStockMutationService,
                                               InventoryDocumentPermissionService inventoryDocumentPermissionService,
                                               InventoryDocumentNotificationService inventoryDocumentNotificationService,
                                               InventoryDocumentWorkflowService inventoryDocumentWorkflowService,
                                               OrgScopeService orgScopeService,
                                               ObjectMapper objectMapper) {
        this.inventoryDocumentRepository = inventoryDocumentRepository;
        this.inventoryStockMutationService = inventoryStockMutationService;
        this.inventoryDocumentPermissionService = inventoryDocumentPermissionService;
        this.inventoryDocumentNotificationService = inventoryDocumentNotificationService;
        this.inventoryDocumentWorkflowService = inventoryDocumentWorkflowService;
        this.orgScopeService = orgScopeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询库存单据。
     *
     * @param type         业务类型
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param documentCode 单据编号
     * @param primaryName  主体一
     * @param itemName     物品名称
     * @param status       单据状态
     * @param remark       备注
     * @param orgId        机构标识
     * @return 分页结果
     */
    public PageData<InventoryDocumentRow> list(InventoryDocumentType type,
                                               Integer pageNum,
                                               Integer pageSize,
                                               String startDate,
                                               String endDate,
                                               String documentCode,
                                               String primaryName,
                                               String itemName,
                                               String status,
                                               String remark,
                                               String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeOrdered(type, scope.scopeType(), scope.scopeId());
        if (!canViewAll) {
            headers = headers.stream()
                    .filter(item -> Objects.equals(item.getCreatedBy(), operatorId))
                    .toList();
        }
        Map<Long, List<InventoryDocumentLine>> lineMap = loadLineMap(type, headers.stream().map(InventoryDocumentHeader::getId).toList());
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String documentCodeKeyword = toLower(trimNullable(documentCode));
        String primaryKeyword = toLower(trimNullable(primaryName));
        String itemKeyword = toLower(trimNullable(itemName));
        String statusKeyword = trimNullable(status);
        String remarkKeyword = toLower(trimNullable(remark));

        List<InventoryDocumentRow> rows = headers.stream()
                .filter(header -> matchDate(header, start, end))
                .filter(header -> !StringUtils.hasText(documentCodeKeyword)
                        || toLower(header.getDocumentCode()).contains(documentCodeKeyword))
                .filter(header -> !StringUtils.hasText(primaryKeyword)
                        || toLower(defaultIfBlank(header.getPrimaryName(), "")).contains(primaryKeyword))
                .filter(header -> !StringUtils.hasText(statusKeyword)
                        || Objects.equals(defaultIfBlank(header.getStatus(), STATUS_DRAFT), statusKeyword))
                .filter(header -> !StringUtils.hasText(remarkKeyword)
                        || toLower(defaultIfBlank(header.getRemark(), "")).contains(remarkKeyword))
                .filter(header -> matchItem(lineMap.getOrDefault(header.getId(), List.of()), itemKeyword))
                .map(header -> toRow(header, lineMap.getOrDefault(header.getId(), List.of())))
                .toList();

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int startIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int endIndex = Math.min(startIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(startIndex, endIndex), rows.size(), safePageNum, safePageSize);
    }

    /**
     * 查询页面权限。
     *
     * @param type  业务类型
     * @param orgId 机构标识
     * @return 权限视图
     */
    public InventoryDocumentPermissionView permissions(InventoryDocumentType type, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        InventoryDocumentPermissionService.PermissionSnapshot permissionSnapshot = inventoryDocumentPermissionService.resolvePermissions(
                type,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
        return new InventoryDocumentPermissionView(
                permissionSnapshot.canCreate(),
                permissionSnapshot.canUpdate(),
                permissionSnapshot.canDelete(),
                permissionSnapshot.canApprove(),
                permissionSnapshot.canUnapprove()
        );
    }

    /**
     * 创建单据。
     *
     * @param type    业务类型
     * @param orgId   机构标识
     * @param request 保存请求
     * @return 创建结果
     */
    @Transactional
    public IdPayload create(InventoryDocumentType type, String orgId, InventoryDocumentSaveRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureOperationPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "CREATE");
        InventoryDocumentHeader header = saveDocument(type, scope, null, request, true, operatorId);
        return new IdPayload(header.getId(), header.getDocumentCode());
    }

    /**
     * 查询详情。
     *
     * @param type  业务类型
     * @param id    主键
     * @param orgId 机构标识
     * @return 单据详情
     */
    public InventoryDocumentDetail detail(InventoryDocumentType type, Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        InventoryDocumentHeader header = requireHeader(type, scope, id, operatorId, canViewAll);
        List<InventoryDocumentLine> lines = inventoryDocumentRepository.findLinesByHeaderId(type, header.getId());
        return new InventoryDocumentDetail(
                header.getId(),
                header.getDocumentCode(),
                defaultIfBlank(header.getStatus(), STATUS_DRAFT),
                header.getDocumentDate() == null ? "" : header.getDocumentDate().toString(),
                defaultIfBlank(header.getPrimaryName(), ""),
                defaultIfBlank(header.getSecondaryName(), ""),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getCounterpartyName2(), ""),
                defaultIfBlank(header.getReason(), ""),
                defaultIfBlank(header.getUpstreamCode(), ""),
                header.getSalesmanUserId(),
                defaultIfBlank(header.getSalesmanName(), ""),
                defaultIfBlank(header.getRemark(), ""),
                defaultIfBlank(header.getRejectionReason(), ""),
                parseExtraJson(header.getExtraJson()),
                lines.stream().map(this::toDetailLine).toList()
        );
    }

    /**
     * 更新单据。
     *
     * @param type    业务类型
     * @param id      主键
     * @param orgId   机构标识
     * @param request 保存请求
     */
    @Transactional
    public void update(InventoryDocumentType type, Long id, String orgId, InventoryDocumentSaveRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureOperationPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        saveDocument(type, scope, id, request, false, operatorId);
    }

    /**
     * 删除单据。
     *
     * @param type  业务类型
     * @param id    主键
     * @param orgId 机构标识
     */
    @Transactional
    public void delete(InventoryDocumentType type, Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureOperationPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "DELETE");
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        InventoryDocumentHeader header = requireHeader(type, scope, id, operatorId, canViewAll);
        deleteInternal(type, header);
    }

    /**
     * 批量删除。
     *
     * @param type    业务类型
     * @param orgId   机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchDelete(InventoryDocumentType type, String orgId, InventoryDocumentBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureOperationPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "DELETE");
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryDocumentHeader> headers = requireHeaders(type, scope, request.ids(), operatorId, canViewAll);
        for (InventoryDocumentHeader header : headers) {
            deleteInternal(type, header);
        }
    }

    /**
     * 批量审核。
     *
     * @param type    业务类型
     * @param orgId   机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchApprove(InventoryDocumentType type, String orgId, InventoryDocumentBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureReviewPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryDocumentHeader> headers = requireHeaders(type, scope, request.ids(), operatorId, true);
        Map<Long, List<InventoryDocumentLine>> lineMap = loadLineMap(type, request.ids());
        for (InventoryDocumentHeader header : headers) {
            approveHeader(type, scope, header, lineMap.getOrDefault(header.getId(), List.of()), operatorId);
        }
    }

    /**
     * 批量反审核。
     *
     * @param type    业务类型
     * @param orgId   机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchUnapprove(InventoryDocumentType type, String orgId, InventoryDocumentBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryDocumentPermissionService.ensureReviewPermission(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        String rejectionReason = requiredTrim(request.rejectionReason(), "拒审原因不能为空");
        List<InventoryDocumentHeader> headers = requireHeaders(type, scope, request.ids(), operatorId, true);
        Map<Long, List<InventoryDocumentLine>> lineMap = loadLineMap(type, request.ids());
        for (InventoryDocumentHeader header : headers) {
            unapproveHeader(type, scope, header, lineMap.getOrDefault(header.getId(), List.of()), operatorId, rejectionReason);
        }
    }

    private InventoryDocumentHeader saveDocument(InventoryDocumentType type,
                                                 InventoryScope scope,
                                                 Long id,
                                                 InventoryDocumentSaveRequest request,
                                                 boolean createMode,
                                                 Long operatorId) {
        InventoryDocumentHeader header = createMode
                ? new InventoryDocumentHeader()
                : requireHeader(type, scope, id, operatorId,
                inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId));
        if (!createMode && Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException("已审核单据不允许编辑，请先反审核");
        }
        header.setScopeType(scope.scopeType());
        header.setScopeId(scope.scopeId());
        header.setDocumentCode(createMode ? generateDocumentCode(type, scope) : header.getDocumentCode());
        header.setDocumentDate(parseRequiredDate(request.documentDate(), "业务日期不能为空"));
        header.setPrimaryName(trimNullable(request.primaryName()));
        header.setSecondaryName(trimNullable(request.secondaryName()));
        header.setCounterpartyName(trimNullable(request.counterpartyName()));
        header.setCounterpartyName2(trimNullable(request.counterpartyName2()));
        header.setReason(trimNullable(request.reason()));
        header.setUpstreamCode(trimNullable(request.upstreamCode()));
        header.setSalesmanUserId(request.salesmanUserId());
        header.setSalesmanName(trimNullable(request.salesmanName()));
        header.setRemark(trimNullable(request.remark()));
        header.setRejectionReason(null);
        header.setCreatedBy(createMode ? operatorId : header.getCreatedBy());
        header.setPendingOperation(PENDING_OPERATION_NONE);
        header.setStatus(STATUS_SUBMITTED);
        header.setExtraJson(writeJson(request.extraFields()));

        List<InventoryDocumentLine> lines = normalizeLines(request.items());
        BigDecimal totalAmount = lines.stream()
                .map(InventoryDocumentLine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        header.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

        if (createMode) {
            inventoryDocumentRepository.saveHeader(type, header);
        } else {
            inventoryDocumentRepository.updateHeader(type, header);
            inventoryDocumentRepository.deleteLinesByHeaderId(type, header.getId());
        }
        for (InventoryDocumentLine line : lines) {
            line.setHeaderId(header.getId());
            inventoryDocumentRepository.saveLine(type, line);
        }
        if (type.isWorkflowEnabled()) {
            boolean workflowApplied = inventoryDocumentWorkflowService.syncOnAction(
                    type,
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.groupId(),
                    header,
                    operatorId,
                    createMode ? "CREATE" : "UPDATE"
            );
            if (workflowApplied && StringUtils.hasText(header.getWorkflowTaskName())) {
                inventoryDocumentNotificationService.recordSubmit(type, scope.scopeType(), scope.scopeId(), scope.groupId(), header);
            }
        }
        return header;
    }

    private void approveHeader(InventoryDocumentType type,
                               InventoryScope scope,
                               InventoryDocumentHeader header,
                               List<InventoryDocumentLine> lines,
                               Long operatorId) {
        if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            return;
        }
        String approverRole = inventoryDocumentWorkflowService.resolveApprovalRoleLabel(
                type,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                header.getWorkflowTaskName()
        );
        if (type.isWorkflowEnabled()) {
            InventoryDocumentWorkflowService.ApprovalResult workflowResult = inventoryDocumentWorkflowService.completeCurrentTask(
                    type,
                    header,
                    operatorId
            );
            if (workflowResult.workflowApplied() && !workflowResult.completed()) {
                inventoryDocumentRepository.updateHeader(type, header);
                inventoryDocumentNotificationService.recordSubmit(type, scope.scopeType(), scope.scopeId(), scope.groupId(), header);
                return;
            }
        }
        applyInventoryDelta(type, scope, header, lines, operatorId, false);
        header.setStatus(STATUS_APPROVED);
        header.setApprovedBy(operatorId);
        header.setApprovedAt(LocalDateTime.now());
        header.setPendingOperation(PENDING_OPERATION_NONE);
        inventoryDocumentRepository.updateHeader(type, header);
        inventoryDocumentNotificationService.recordApproved(type, scope.scopeType(), scope.scopeId(), header, approverRole, header.getApprovedAt());
    }

    private void unapproveHeader(InventoryDocumentType type,
                                 InventoryScope scope,
                                 InventoryDocumentHeader header,
                                 List<InventoryDocumentLine> lines,
                                 Long operatorId,
                                 String rejectionReason) {
        if (!Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            return;
        }
        applyInventoryDelta(type, scope, header, lines, operatorId, true);
        String approverRole = inventoryDocumentWorkflowService.resolveApprovalRoleLabel(
                type,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                header.getWorkflowTaskName()
        );
        header.setStatus(STATUS_SUBMITTED);
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        header.setRejectionReason(rejectionReason);
        header.setPendingOperation(PENDING_OPERATION_NONE);
        if (type.isWorkflowEnabled()) {
            inventoryDocumentWorkflowService.resetWorkflowState(type, header);
        } else {
            inventoryDocumentRepository.updateHeader(type, header);
        }
        inventoryDocumentNotificationService.recordRejected(type, scope.scopeType(), scope.scopeId(), header, approverRole, rejectionReason);
    }

    private void applyInventoryDelta(InventoryDocumentType type,
                                     InventoryScope scope,
                                     InventoryDocumentHeader header,
                                     List<InventoryDocumentLine> lines,
                                     Long operatorId,
                                     boolean reverse) {
        if (type.getStockDirection() == InventoryDocumentType.StockDirection.NONE) {
            return;
        }
        String stockLocation = resolveStockLocation(type, header);
        BigDecimal multiplier = switch (type.getStockDirection()) {
            case INBOUND -> reverse ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
            case OUTBOUND -> reverse ? BigDecimal.ONE : BigDecimal.valueOf(-1);
            case NONE -> BigDecimal.ZERO;
        };
        if (multiplier.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        for (InventoryDocumentLine line : lines) {
            inventoryStockMutationService.applyDelta(
                    scope.scopeType(),
                    scope.scopeId(),
                    stockLocation,
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getQuantity().multiply(multiplier),
                    reverse ? type.getBusinessCode() + "_UNAPPROVE" : type.getBusinessCode() + "_APPROVE",
                    operatorId
            );
        }
    }

    private String resolveStockLocation(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.STOCK_TRANSFER_INBOUND
                || type == InventoryDocumentType.DEPARTMENT_RETURN
                || type == InventoryDocumentType.OTHER_INBOUND
                || type == InventoryDocumentType.PRODUCTION_INBOUND
                || type == InventoryDocumentType.CUSTOMER_RETURN_INBOUND) {
            return defaultIfBlank(header.getPrimaryName(), type.getBusinessName());
        }
        return defaultIfBlank(header.getPrimaryName(), type.getBusinessName());
    }

    private void deleteInternal(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException("已审核单据请先反审核后再删除");
        }
        if (StringUtils.hasText(header.getWorkflowInstanceId())) {
            inventoryDocumentWorkflowService.cancelWorkflowInstanceIfRunning(header);
        }
        inventoryDocumentRepository.deleteLinesByHeaderId(type, header.getId());
        inventoryDocumentRepository.deleteHeaderById(type, header.getId());
    }

    private List<InventoryDocumentHeader> requireHeaders(InventoryDocumentType type,
                                                         InventoryScope scope,
                                                         List<Long> ids,
                                                         Long operatorId,
                                                         boolean canViewAll) {
        List<Long> validIds = ids == null ? List.of() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw new BusinessException("请选择单据");
        }
        List<InventoryDocumentHeader> headers = inventoryDocumentRepository.findHeadersByScopeAndIds(
                type,
                scope.scopeType(),
                scope.scopeId(),
                operatorId,
                canViewAll,
                validIds
        );
        if (headers.size() != validIds.size()) {
            throw new BusinessException("单据不存在或无权操作");
        }
        Map<Long, InventoryDocumentHeader> headerMap = headers.stream()
                .collect(Collectors.toMap(InventoryDocumentHeader::getId, item -> item));
        List<InventoryDocumentHeader> ordered = new ArrayList<>();
        for (Long id : validIds) {
            InventoryDocumentHeader header = headerMap.get(id);
            if (header != null) {
                ordered.add(header);
            }
        }
        return ordered;
    }

    private InventoryDocumentHeader requireHeader(InventoryDocumentType type,
                                                  InventoryScope scope,
                                                  Long id,
                                                  Long operatorId,
                                                  boolean canViewAll) {
        return inventoryDocumentRepository.findHeaderByScopeAndId(
                        type,
                        scope.scopeType(),
                        scope.scopeId(),
                        operatorId,
                        canViewAll,
                        id
                )
                .orElseThrow(() -> new BusinessException("单据不存在或无权查看"));
    }

    private Map<Long, List<InventoryDocumentLine>> loadLineMap(InventoryDocumentType type, List<Long> headerIds) {
        return inventoryDocumentRepository.findLinesByHeaderIds(type, headerIds).stream()
                .collect(Collectors.groupingBy(InventoryDocumentLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
    }

    private boolean matchDate(InventoryDocumentHeader header, LocalDate start, LocalDate end) {
        if (header.getDocumentDate() == null) {
            return start == null && end == null;
        }
        if (start != null && header.getDocumentDate().isBefore(start)) {
            return false;
        }
        return end == null || !header.getDocumentDate().isAfter(end);
    }

    private boolean matchItem(List<InventoryDocumentLine> lines, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return lines.stream().anyMatch(item ->
                toLower(defaultIfBlank(item.getItemCode(), "")).contains(keyword)
                        || toLower(defaultIfBlank(item.getItemName(), "")).contains(keyword));
    }

    private InventoryDocumentRow toRow(InventoryDocumentHeader header, List<InventoryDocumentLine> lines) {
        String creator = header.getCreatedBy() == null ? "" : String.valueOf(header.getCreatedBy());
        BigDecimal amount = header.getTotalAmount() == null
                ? lines.stream()
                .map(InventoryDocumentLine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : header.getTotalAmount();
        return new InventoryDocumentRow(
                header.getId(),
                defaultIfBlank(header.getDocumentCode(), ""),
                header.getDocumentDate() == null ? "" : header.getDocumentDate().toString(),
                defaultIfBlank(header.getPrimaryName(), ""),
                defaultIfBlank(header.getSecondaryName(), ""),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getStatus(), STATUS_DRAFT),
                Objects.equals(defaultIfBlank(header.getStatus(), STATUS_DRAFT), STATUS_APPROVED) ? "已复审" : "未复审",
                amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                formatDateTime(header.getCreatedAt()),
                creator,
                defaultIfBlank(header.getRemark(), "")
        );
    }

    private InventoryDocumentDetailLine toDetailLine(InventoryDocumentLine line) {
        return new InventoryDocumentDetailLine(
                defaultIfBlank(line.getItemCode(), ""),
                defaultIfBlank(line.getItemName(), ""),
                defaultIfBlank(line.getSpec(), ""),
                defaultIfBlank(line.getCategory(), ""),
                defaultIfBlank(line.getUnitName(), ""),
                line.getAvailableQty(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getAmount(),
                defaultIfBlank(line.getLineReason(), ""),
                defaultIfBlank(line.getRemark(), ""),
                parseExtraJson(line.getExtraJson())
        );
    }

    private List<InventoryDocumentLine> normalizeLines(List<InventoryDocumentSaveRequest.InventoryDocumentLineRequest> items) {
        List<InventoryDocumentLine> rows = new ArrayList<>();
        for (InventoryDocumentSaveRequest.InventoryDocumentLineRequest item : items) {
            InventoryDocumentLine line = new InventoryDocumentLine();
            line.setItemCode(requiredTrim(item.itemCode(), "物品编码不能为空"));
            line.setItemName(requiredTrim(item.itemName(), "物品名称不能为空"));
            line.setSpec(trimNullable(item.spec()));
            line.setCategory(trimNullable(item.category()));
            line.setUnitName(trimNullable(item.unitName()));
            line.setAvailableQty(normalizeNonNegative(item.availableQty()));
            line.setQuantity(normalizePositive(item.quantity(), "数量必须大于 0"));
            line.setUnitPrice(normalizeNonNegative(item.unitPrice()));
            BigDecimal amount = item.amount();
            if (amount == null) {
                amount = line.getQuantity().multiply(line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice());
            }
            line.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            line.setLineReason(trimNullable(item.lineReason()));
            line.setRemark(trimNullable(item.remark()));
            line.setExtraJson(writeJson(item.extraFields()));
            rows.add(line);
        }
        return rows;
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private String generateDocumentCode(InventoryDocumentType type, InventoryScope scope) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT));
        for (int cursor = 1; cursor < 999; cursor++) {
            int seed = ThreadLocalRandom.current().nextInt(1, 999);
            String suffix = String.format(Locale.ROOT, "%03d", (cursor + seed) % 1000);
            String candidate = type.getDocumentPrefix() + "-" + datePart + "-" + suffix;
            if (inventoryDocumentRepository.countByScopeAndDocumentCode(type, scope.scopeType(), scope.scopeId(), candidate) == 0) {
                return candidate;
            }
        }
        throw new BusinessException("单据编号生成失败，请稍后重试");
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

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("数值不能小于 0");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
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

    private String writeJson(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("扩展字段保存失败");
        }
    }

    private Map<String, String> parseExtraJson(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(extraJson, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 创建结果。
     *
     * @param id 单据 ID
     * @param documentCode 单据编号
     */
    public record IdPayload(Long id, String documentCode) {
    }

    /**
     * 列表行。
     *
     * @param id 主键
     * @param documentCode 单据编号
     * @param documentDate 业务日期
     * @param primaryName 主体一
     * @param secondaryName 主体二
     * @param counterpartyName 对方主体
     * @param status 单据状态
     * @param reviewStatus 审核状态
     * @param amount 金额
     * @param createdAt 创建时间
     * @param creator 创建人
     * @param remark 备注
     */
    public record InventoryDocumentRow(Long id,
                                       String documentCode,
                                       String documentDate,
                                       String primaryName,
                                       String secondaryName,
                                       String counterpartyName,
                                       String status,
                                       String reviewStatus,
                                       String amount,
                                       String createdAt,
                                       String creator,
                                       String remark) {
    }

    /**
     * 详情。
     *
     * @param id 主键
     * @param documentCode 单据编号
     * @param status 状态
     * @param documentDate 业务日期
     * @param primaryName 主体一
     * @param secondaryName 主体二
     * @param counterpartyName 对方主体一
     * @param counterpartyName2 对方主体二
     * @param reason 业务原因
     * @param upstreamCode 上游单号
     * @param salesmanUserId 业务员 ID
     * @param salesmanName 业务员名称
     * @param remark 备注
     * @param rejectionReason 拒审原因
     * @param extraFields 扩展字段
     * @param items 明细
     */
    public record InventoryDocumentDetail(Long id,
                                          String documentCode,
                                          String status,
                                          String documentDate,
                                          String primaryName,
                                          String secondaryName,
                                          String counterpartyName,
                                          String counterpartyName2,
                                          String reason,
                                          String upstreamCode,
                                          Long salesmanUserId,
                                          String salesmanName,
                                          String remark,
                                          String rejectionReason,
                                          Map<String, String> extraFields,
                                          List<InventoryDocumentDetailLine> items) {
    }

    /**
     * 详情明细。
     *
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param spec 规格
     * @param category 分类
     * @param unitName 单位
     * @param availableQty 可用数量
     * @param quantity 数量
     * @param unitPrice 单价
     * @param amount 金额
     * @param lineReason 行原因
     * @param remark 备注
     * @param extraFields 扩展字段
     */
    public record InventoryDocumentDetailLine(String itemCode,
                                              String itemName,
                                              String spec,
                                              String category,
                                              String unitName,
                                              BigDecimal availableQty,
                                              BigDecimal quantity,
                                              BigDecimal unitPrice,
                                              BigDecimal amount,
                                              String lineReason,
                                              String remark,
                                              Map<String, String> extraFields) {
    }

    /**
     * 页面权限。
     *
     * @param canCreate 是否可创建
     * @param canUpdate 是否可编辑
     * @param canDelete 是否可删除
     * @param canApprove 是否可审核
     * @param canUnapprove 是否可反审核
     */
    public record InventoryDocumentPermissionView(boolean canCreate,
                                                  boolean canUpdate,
                                                  boolean canDelete,
                                                  boolean canApprove,
                                                  boolean canUnapprove) {
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }
}
