package com.boboboom.jxc.inventory.application.service;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryDocumentSaveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 通用库存单据业务服务。
 */
@Service
public class InventoryDocumentApplicationService {

    private static final int DOCUMENT_CODE_MAX_ATTEMPTS = 999;
    private static final int DOCUMENT_CODE_SUFFIX_MODULUS = 1000;
    private static final String DOCUMENT_CODE_SUFFIX_FORMAT = "%03d";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int LIST_MAX_PAGE_SIZE = 100;
    private static final int INVENTORY_QUANTITY_SCALE = 4;

    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final String STORE_TRANSFER_SOURCE_WAREHOUSE = "sourceWarehouse";
    private static final String STORE_TRANSFER_TARGET_WAREHOUSE = "targetWarehouse";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final InventoryDocumentPermissionService inventoryDocumentPermissionService;
    private final InventoryDocumentNotificationService inventoryDocumentNotificationService;
    private final InventoryDocumentWorkflowService inventoryDocumentWorkflowService;
    private final OrgScopeService orgScopeService;
    private final WarehouseRepository warehouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;
    private final DictionaryLookupService dictionaryLookupService;
    private final InventoryItemCategoryValidator inventoryItemCategoryValidator;

    /** 通用库存单据业务服务，负责库存单据保存、审核、反审核、列表查询和打印数据组装。 */
    public InventoryDocumentApplicationService(InventoryDocumentRepository inventoryDocumentRepositoryValue,
                                               InventoryStockMutationService inventoryStockMutationServiceValue,
                                               InventoryDocumentPermissionService inventoryDocumentPermissionServiceValue,
                                               InventoryDocumentNotificationService inventoryDocumentNotificationServiceValue,
                                               InventoryDocumentWorkflowService inventoryDocumentWorkflowServiceValue,
                                               OrgScopeService orgScopeServiceValue,
                                               WarehouseRepository warehouseRepositoryValue,
                                               UserAccountRepository userAccountRepositoryValue,
                                               ObjectMapper objectMapperValue,
                                               DictionaryLookupService dictionaryLookupServiceValue,
                                               InventoryItemCategoryValidator inventoryItemCategoryValidatorValue) {
        this.inventoryDocumentRepository = inventoryDocumentRepositoryValue;
        this.inventoryStockMutationService = inventoryStockMutationServiceValue;
        this.inventoryDocumentPermissionService = inventoryDocumentPermissionServiceValue;
        this.inventoryDocumentNotificationService = inventoryDocumentNotificationServiceValue;
        this.inventoryDocumentWorkflowService = inventoryDocumentWorkflowServiceValue;
        this.orgScopeService = orgScopeServiceValue;
        this.warehouseRepository = warehouseRepositoryValue;
        this.userAccountRepository = userAccountRepositoryValue;
        this.objectMapper = objectMapperValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
        this.inventoryItemCategoryValidator = inventoryItemCategoryValidatorValue;
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
                    .filter(item -> belongsToOperator(item, operatorId))
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

        if (type == InventoryDocumentType.WAREHOUSE_OPENING_BALANCE) {
            return listWarehouseOpeningBalance(
                    scope,
                    headers,
                    lineMap,
                    pageNum,
                    pageSize,
                    start,
                    end,
                    documentCodeKeyword,
                    primaryKeyword,
                    itemKeyword,
                    statusKeyword,
                    remarkKeyword
            );
        }

        List<InventoryDocumentRow> rows = headers.stream()
                .filter(header -> matchDocumentListRow(header, lineMap.getOrDefault(header.getId(), List.of()),
                        start, end, documentCodeKeyword, primaryKeyword, itemKeyword, statusKeyword, remarkKeyword))
                .map(header -> toRow(header, lineMap.getOrDefault(header.getId(), List.of())))
                .toList();

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
        int startIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int endIndex = Math.min(startIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(startIndex, endIndex), rows.size(), safePageNum, safePageSize);
    }

    private boolean matchDocumentListRow(InventoryDocumentHeader header,
                                         List<InventoryDocumentLine> lines,
                                         LocalDate start,
                                         LocalDate end,
                                         String documentCodeKeyword,
                                         String primaryKeyword,
                                         String itemKeyword,
                                         String statusKeyword,
                                         String remarkKeyword) {
        return matchDate(header, start, end)
                && matchDocumentCode(header, documentCodeKeyword)
                && matchPrimaryName(header, primaryKeyword)
                && matchDocumentStatus(header, statusKeyword)
                && matchRemark(header, remarkKeyword)
                && matchItem(lines, itemKeyword);
    }

    private boolean matchDocumentCode(InventoryDocumentHeader header, String documentCodeKeyword) {
        return !StringUtils.hasText(documentCodeKeyword) || toLower(header.getDocumentCode()).contains(documentCodeKeyword);
    }

    private boolean matchPrimaryName(InventoryDocumentHeader header, String primaryKeyword) {
        return !StringUtils.hasText(primaryKeyword)
                || toLower(defaultIfBlank(header.getPrimaryName(), "")).contains(primaryKeyword);
    }

    private boolean matchDocumentStatus(InventoryDocumentHeader header, String statusKeyword) {
        return !StringUtils.hasText(statusKeyword) || Objects.equals(defaultIfBlank(header.getStatus(), draftStatus()), statusKeyword);
    }

    private boolean matchRemark(InventoryDocumentHeader header, String remarkKeyword) {
        return !StringUtils.hasText(remarkKeyword) || toLower(defaultIfBlank(header.getRemark(), "")).contains(remarkKeyword);
    }

    private PageData<InventoryDocumentRow> listWarehouseOpeningBalance(InventoryScope scope,
                                                                       List<InventoryDocumentHeader> headers,
                                                                       Map<Long, List<InventoryDocumentLine>> lineMap,
                                                                       Integer pageNum,
                                                                       Integer pageSize,
                                                                       LocalDate start,
                                                                       LocalDate end,
                                                                       String documentCodeKeyword,
                                                                       String primaryKeyword,
                                                                       String itemKeyword,
                                                                       String statusKeyword,
                                                                       String remarkKeyword) {
        Map<String, InventoryDocumentHeader> latestHeaderByWarehouse = new LinkedHashMap<>();
        for (InventoryDocumentHeader header : headers) {
            String warehouseName = trimNullable(header.getPrimaryName());
            if (!StringUtils.hasText(warehouseName) || latestHeaderByWarehouse.containsKey(warehouseName)) {
                continue;
            }
            latestHeaderByWarehouse.put(warehouseName, header);
        }

        List<WarehouseDO> warehouses = loadScopeWarehouses(scope).stream()
                .filter(warehouse -> Objects.equals(defaultIfBlank(warehouse.getStatus(), ""), enabledStatus()))
                .toList();

        List<InventoryDocumentRow> rows = warehouses.stream()
                .map(warehouse -> {
                    InventoryDocumentHeader header = latestHeaderByWarehouse.get(warehouse.getWarehouseName());
                    if (header == null) {
                        return buildWarehouseOpeningBalancePendingRow(warehouse);
                    }
                    return toWarehouseOpeningBalanceRow(warehouse, header, lineMap.getOrDefault(header.getId(), List.of()));
                })
                .filter(row -> matchWarehouseOpeningBalanceRow(row, start, end, documentCodeKeyword, primaryKeyword, itemKeyword, statusKeyword, remarkKeyword))
                .toList();

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
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
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId)
                || canReviewDocument(type, scope, operatorId);
        InventoryDocumentHeader header = requireHeader(type, scope, id, operatorId, canViewAll);
        List<InventoryDocumentLine> lines = inventoryDocumentRepository.findLinesByHeaderId(type, header.getId());
        return new InventoryDocumentDetail(
                header.getId(),
                header.getDocumentCode(),
                defaultIfBlank(header.getStatus(), draftStatus()),
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
                resolveUserDisplayName(header.getCreatedBy()),
                formatDateTime(header.getCreatedAt()),
                resolveUserDisplayName(header.getApprovedBy()),
                formatDateTime(header.getApprovedAt()),
                parseExtraJson(header.getExtraJson()),
                lines.stream().map(this::toDetailLine).toList()
        );
    }

    private boolean canReviewDocument(InventoryDocumentType type, InventoryScope scope, Long operatorId) {
        return type != InventoryDocumentType.WAREHOUSE_OPENING_BALANCE
                && type.isWorkflowEnabled()
                && inventoryDocumentPermissionService.canReview(type, scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
    }

    private boolean belongsToOperator(InventoryDocumentHeader header, Long operatorId) {
        return Objects.equals(header.getCreatedBy(), operatorId) || Objects.equals(header.getSalesmanUserId(), operatorId);
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
        boolean allowReviewAccess = type != InventoryDocumentType.WAREHOUSE_OPENING_BALANCE
                && type.isWorkflowEnabled();
        boolean canViewAll = inventoryDocumentPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryDocumentHeader> headers = requireHeaders(type, scope, request.ids(), operatorId, allowReviewAccess || canViewAll);
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
        if (!createMode && Objects.equals(header.getStatus(), approvedStatus())) {
            throw new BusinessException("已审核单据不允许编辑，请先反审核");
        }
        applyDocumentHeader(type, scope, header, request, createMode, operatorId);

        List<InventoryDocumentLine> lines = normalizeLines(request.items());
        validateItemCategories(scope, lines);
        header.setTotalAmount(calculateTotalAmount(lines));

        saveDocumentWithLines(type, header, lines, createMode);
        syncDocumentWorkflow(type, scope, header, operatorId, createMode);
        return header;
    }

    private void applyDocumentHeader(InventoryDocumentType type,
                                     InventoryScope scope,
                                     InventoryDocumentHeader header,
                                     InventoryDocumentSaveRequest request,
                                     boolean createMode,
                                     Long operatorId) {
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
        header.setStatus(submittedStatus());
        header.setExtraJson(writeJson(normalizeHeaderExtraFields(type, request.extraFields())));
        initializeWorkflowState(type, header);
    }

    private Map<String, String> normalizeHeaderExtraFields(InventoryDocumentType type, Map<String, String> extraFields) {
        Map<String, String> normalized = extraFields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extraFields);
        if (type == InventoryDocumentType.STORE_TRANSFER) {
            normalized.put(STORE_TRANSFER_SOURCE_WAREHOUSE,
                    requiredTrim(normalized.get(STORE_TRANSFER_SOURCE_WAREHOUSE), "调出仓库不能为空"));
            normalized.put(STORE_TRANSFER_TARGET_WAREHOUSE,
                    requiredTrim(normalized.get(STORE_TRANSFER_TARGET_WAREHOUSE), "调入仓库不能为空"));
        }
        return normalized;
    }

    private BigDecimal calculateTotalAmount(List<InventoryDocumentLine> lines) {
        return lines.stream()
                .map(InventoryDocumentLine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void saveDocumentWithLines(InventoryDocumentType type,
                                       InventoryDocumentHeader header,
                                       List<InventoryDocumentLine> lines,
                                       boolean createMode) {
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
    }

    private void syncDocumentWorkflow(InventoryDocumentType type,
                                      InventoryScope scope,
                                      InventoryDocumentHeader header,
                                      Long operatorId,
                                      boolean createMode) {
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
    }

    private void initializeWorkflowState(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (!type.isWorkflowEnabled()) {
            header.setWorkflowProcessCode(null);
            header.setWorkflowDefinitionKey(null);
            header.setWorkflowDefinitionId(null);
            header.setWorkflowInstanceId(null);
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
        }
        if (!StringUtils.hasText(header.getWorkflowStatus())) {
            header.setWorkflowStatus(workflowNoneStatus());
        }
        if (!StringUtils.hasText(header.getPendingOperation())) {
            header.setPendingOperation(PENDING_OPERATION_NONE);
        }
    }

    private void approveHeader(InventoryDocumentType type,
                               InventoryScope scope,
                               InventoryDocumentHeader header,
                               List<InventoryDocumentLine> lines,
                               Long operatorId) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
            return;
        }
        validateItemCategories(scope, lines);
        String approverRole = null;
        if (type.isWorkflowEnabled()) {
            approverRole = inventoryDocumentWorkflowService.resolveApprovalRoleLabel(
                    type,
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.groupId(),
                    operatorId,
                    header.getWorkflowTaskName()
            );
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
        header.setStatus(approvedStatus());
        header.setApprovedBy(operatorId);
        header.setApprovedAt(LocalDateTime.now());
        header.setPendingOperation(PENDING_OPERATION_NONE);
        inventoryDocumentRepository.updateHeader(type, header);
        if (type.isWorkflowEnabled()) {
            inventoryDocumentNotificationService.recordApproved(type, scope.scopeType(), scope.scopeId(), header, approverRole, header.getApprovedAt());
        }
    }

    private void unapproveHeader(InventoryDocumentType type,
                                 InventoryScope scope,
                                 InventoryDocumentHeader header,
                                 List<InventoryDocumentLine> lines,
                                 Long operatorId,
                                 String rejectionReason) {
        if (!Objects.equals(header.getStatus(), approvedStatus())) {
            return;
        }
        validateItemCategories(scope, lines);
        applyInventoryDelta(type, scope, header, lines, operatorId, true);
        String approverRole = inventoryDocumentWorkflowService.resolveApprovalRoleLabel(
                type,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                header.getWorkflowTaskName()
        );
        header.setStatus(submittedStatus());
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
        if (type == InventoryDocumentType.WAREHOUSE_OPENING_BALANCE) {
            applyWarehouseOpeningBalance(scope, header, lines, operatorId, reverse);
            return;
        }
        if (type.getStockDirection() == InventoryDocumentType.StockDirection.NONE) {
            return;
        }
        String stockLocation = resolveStockLocation(type, header);
        BigDecimal multiplier = resolveStockDeltaMultiplier(type, reverse);
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
                    line.getAmount(),
                    header.getDocumentDate(),
                    reverse ? type.getBusinessCode() + "_UNAPPROVE" : type.getBusinessCode() + "_APPROVE",
                    operatorId
            );
        }
    }

    private BigDecimal resolveStockDeltaMultiplier(InventoryDocumentType type, boolean reverse) {
        return switch (type.getStockDirection()) {
            case INBOUND -> inboundDeltaMultiplier(reverse);
            case OUTBOUND -> outboundDeltaMultiplier(reverse);
            case NONE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal inboundDeltaMultiplier(boolean reverse) {
        return reverse ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
    }

    private BigDecimal outboundDeltaMultiplier(boolean reverse) {
        return reverse ? BigDecimal.ONE : BigDecimal.valueOf(-1);
    }

    private void applyWarehouseOpeningBalance(InventoryScope scope,
                                              InventoryDocumentHeader header,
                                              List<InventoryDocumentLine> lines,
                                              Long operatorId,
                                              boolean reverse) {
        if (reverse) {
            throw new BusinessException("仓库期初不支持反确认");
        }
        String warehouseName = resolveStockLocation(InventoryDocumentType.WAREHOUSE_OPENING_BALANCE, header);
        for (InventoryDocumentLine line : lines) {
            inventoryStockMutationService.applyAbsolute(
                    scope.scopeType(),
                    scope.scopeId(),
                    warehouseName,
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    resolveWarehouseOpeningBalanceQuantity(line),
                    line.getAmount(),
                    header.getDocumentDate(),
                    InventoryDocumentType.WAREHOUSE_OPENING_BALANCE.getBusinessCode() + "_CONFIRM",
                    operatorId
            );
        }
    }

    private BigDecimal resolveWarehouseOpeningBalanceQuantity(InventoryDocumentLine line) {
        Map<String, String> extraFields = parseExtraJson(line.getExtraJson());
        String baseUnitQuantity = trimNullable(extraFields.get("baseUnitQuantity"));
        if (StringUtils.hasText(baseUnitQuantity)) {
            try {
                return normalizeNonNegative(new BigDecimal(baseUnitQuantity));
            } catch (NumberFormatException ex) {
                throw new BusinessException("基准单位数量格式不正确");
            }
        }
        return line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
    }

    private String resolveStockLocation(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (type == InventoryDocumentType.STORE_TRANSFER) {
            return requiredStoreTransferWarehouse(header, STORE_TRANSFER_SOURCE_WAREHOUSE, "调出仓库不能为空");
        }
        if (type == InventoryDocumentType.STOCK_TRANSFER_INBOUND
                || type == InventoryDocumentType.DEPARTMENT_RETURN
                || type == InventoryDocumentType.OTHER_INBOUND
                || type == InventoryDocumentType.PRODUCTION_INBOUND
                || type == InventoryDocumentType.CUSTOMER_RETURN_INBOUND) {
            return defaultIfBlank(header.getPrimaryName(), type.getBusinessName());
        }
        return defaultIfBlank(header.getPrimaryName(), type.getBusinessName());
    }

    private String requiredStoreTransferWarehouse(InventoryDocumentHeader header, String fieldName, String message) {
        return requiredTrim(parseExtraJson(header.getExtraJson()).get(fieldName), message);
    }

    private void deleteInternal(InventoryDocumentType type, InventoryDocumentHeader header) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
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
                null,
                "",
                defaultIfBlank(header.getPrimaryName(), ""),
                defaultIfBlank(header.getSecondaryName(), ""),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getStatus(), draftStatus()),
                Objects.equals(defaultIfBlank(header.getStatus(), draftStatus()), approvedStatus()) ? "已审核" : "未审核",
                amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                formatDateTime(header.getCreatedAt()),
                creator,
                defaultIfBlank(header.getRemark(), "")
        );
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return "";
        }
        UserAccountDO user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在: " + userId));
        String username = defaultIfBlank(user.getUsername(), "");
        String realName = defaultIfBlank(user.getRealName(), "");
        if (StringUtils.hasText(username) && StringUtils.hasText(realName)) {
            return "[" + username + "]" + realName;
        }
        if (StringUtils.hasText(realName)) {
            return realName;
        }
        if (StringUtils.hasText(username)) {
            return "[" + username + "]";
        }
        throw new BusinessException("用户显示名不存在: " + userId);
    }

    private InventoryDocumentRow toWarehouseOpeningBalanceRow(WarehouseDO warehouse, InventoryDocumentHeader header, List<InventoryDocumentLine> lines) {
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
                warehouse.getId(),
                defaultIfBlank(warehouse.getWarehouseCode(), ""),
                defaultIfBlank(header.getPrimaryName(), defaultIfBlank(warehouse.getWarehouseName(), "")),
                defaultIfBlank(header.getSecondaryName(), ""),
                defaultIfBlank(header.getCounterpartyName(), ""),
                defaultIfBlank(header.getStatus(), submittedStatus()),
                Objects.equals(defaultIfBlank(header.getStatus(), draftStatus()), approvedStatus()) ? "已审核" : "未审核",
                amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                formatDateTime(header.getCreatedAt()),
                creator,
                defaultIfBlank(header.getRemark(), "")
        );
    }

    private InventoryDocumentRow buildWarehouseOpeningBalancePendingRow(WarehouseDO warehouse) {
        return new InventoryDocumentRow(
                0L,
                "",
                "",
                warehouse.getId(),
                defaultIfBlank(warehouse.getWarehouseCode(), ""),
                defaultIfBlank(warehouse.getWarehouseName(), ""),
                "",
                "",
                "UNINITIALIZED",
                "未审核",
                "0.00",
                "",
                "",
                ""
        );
    }

    private boolean matchWarehouseOpeningBalanceRow(InventoryDocumentRow row,
                                                    LocalDate start,
                                                    LocalDate end,
                                                    String documentCodeKeyword,
                                                    String primaryKeyword,
                                                    String itemKeyword,
                                                    String statusKeyword,
                                                    String remarkKeyword) {
        if (!matchOpeningBalanceBasicQuery(row, documentCodeKeyword, primaryKeyword, itemKeyword, statusKeyword, remarkKeyword)) {
            return false;
        }
        return matchOpeningBalanceDate(row, start, end);
    }

    private boolean matchOpeningBalanceBasicQuery(InventoryDocumentRow row,
                                                  String documentCodeKeyword,
                                                  String primaryKeyword,
                                                  String itemKeyword,
                                                  String statusKeyword,
                                                  String remarkKeyword) {
        return matchOpeningBalanceDocumentCode(row, documentCodeKeyword)
                && matchOpeningBalancePrimaryName(row, primaryKeyword)
                && !StringUtils.hasText(itemKeyword)
                && matchOpeningBalanceStatus(row, statusKeyword)
                && matchOpeningBalanceRemark(row, remarkKeyword);
    }

    private boolean matchOpeningBalanceDocumentCode(InventoryDocumentRow row, String documentCodeKeyword) {
        return !StringUtils.hasText(documentCodeKeyword)
                || toLower(defaultIfBlank(row.documentCode(), "")).contains(documentCodeKeyword);
    }

    private boolean matchOpeningBalancePrimaryName(InventoryDocumentRow row, String primaryKeyword) {
        return !StringUtils.hasText(primaryKeyword)
                || toLower(defaultIfBlank(row.primaryName(), "")).contains(primaryKeyword);
    }

    private boolean matchOpeningBalanceStatus(InventoryDocumentRow row, String statusKeyword) {
        return !StringUtils.hasText(statusKeyword)
                || Objects.equals(defaultIfBlank(row.status(), "UNINITIALIZED"), statusKeyword);
    }

    private boolean matchOpeningBalanceRemark(InventoryDocumentRow row, String remarkKeyword) {
        return !StringUtils.hasText(remarkKeyword)
                || toLower(defaultIfBlank(row.remark(), "")).contains(remarkKeyword);
    }

    private boolean matchOpeningBalanceDate(InventoryDocumentRow row, LocalDate start, LocalDate end) {
        if (!StringUtils.hasText(row.documentDate())) {
            return start == null && end == null;
        }
        LocalDate rowDate = parseDateNullable(row.documentDate(), "业务日期格式不正确");
        if (rowDate == null) {
            return start == null && end == null;
        }
        if (start != null && rowDate.isBefore(start)) {
            return false;
        }
        return end == null || !rowDate.isAfter(end);
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

    private void validateItemCategories(InventoryScope scope, List<InventoryDocumentLine> lines) {
        inventoryItemCategoryValidator.validateLines(
                scope.scopeType(),
                scope.scopeId(),
                lines.stream()
                        .map(line -> new InventoryItemCategoryValidator.LineCategory(line.getItemCode(), line.getCategory()))
                        .toList()
        );
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private String generateDocumentCode(InventoryDocumentType type, InventoryScope scope) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT));
        for (int cursor = 1; cursor < DOCUMENT_CODE_MAX_ATTEMPTS; cursor++) {
            int seed = ThreadLocalRandom.current().nextInt(1, DOCUMENT_CODE_MAX_ATTEMPTS);
            String suffix = String.format(Locale.ROOT, DOCUMENT_CODE_SUFFIX_FORMAT,
                    (cursor + seed) % DOCUMENT_CODE_SUFFIX_MODULUS);
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
        return value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("数值不能小于 0");
        }
        return value.setScale(INVENTORY_QUANTITY_SCALE, RoundingMode.HALF_UP);
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

    private String draftStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.DRAFT);
    }

    private String submittedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.SUBMITTED);
    }

    private String approvedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.APPROVED);
    }

    private String workflowNoneStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.NONE);
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
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
            throw new BusinessException("单据扩展字段格式错误，请清理脏数据");
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
     * @param primaryId 主体主键
     * @param primaryCode 主体编码
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
                                       Long primaryId,
                                       String primaryCode,
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
     * @param creator 创建人
     * @param createdAt 创建时间
     * @param auditor 审核人
     * @param auditedAt 审核时间
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
                                          String creator,
                                          String createdAt,
                                          String auditor,
                                          String auditedAt,
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
