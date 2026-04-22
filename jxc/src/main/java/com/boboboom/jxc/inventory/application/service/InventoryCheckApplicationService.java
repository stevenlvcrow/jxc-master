package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryCheckBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.InventoryCheckSaveRequest;
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
 * 盘点单业务服务。
 */
@Service
public class InventoryCheckApplicationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String PRINT_STATUS_UNPRINTED = "UNPRINTED";
    private static final String PRINT_STATUS_PRINTED = "PRINTED";
    private static final String GENERATED_STATUS_UNGENERATED = "UNGENERATED";
    private static final String GENERATED_STATUS_GENERATED = "GENERATED";

    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckPermissionService inventoryCheckPermissionService;
    private final InventoryStockMutationService inventoryStockMutationService;
    private final OrgScopeService orgScopeService;
    private final ObjectMapper objectMapper;
    private final DictionaryLookupService dictionaryLookupService;

    public InventoryCheckApplicationService(InventoryCheckRepository inventoryCheckRepository,
                                            InventoryCheckPermissionService inventoryCheckPermissionService,
                                            InventoryStockMutationService inventoryStockMutationService,
                                            OrgScopeService orgScopeService,
                                            ObjectMapper objectMapper,
                                            DictionaryLookupService dictionaryLookupService) {
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.inventoryCheckPermissionService = inventoryCheckPermissionService;
        this.inventoryStockMutationService = inventoryStockMutationService;
        this.orgScopeService = orgScopeService;
        this.objectMapper = objectMapper;
        this.dictionaryLookupService = dictionaryLookupService;
    }

    /**
     * 分页查询盘点单。
     *
     * @param kind 盘点单类型
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param timeType 时间类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param warehouse 仓库
     * @param documentCode 单据编号
     * @param itemName 物品名称
     * @param status 状态
     * @param checkRangeType 盘点范围类型
     * @param printStatus 打印状态
     * @param generatedStatus 生成状态
     * @param remark 备注
     * @param orgId 机构标识
     * @return 分页结果
     */
    public PageData<InventoryCheckRow> list(InventoryCheckKind kind,
                                            Integer pageNum,
                                            Integer pageSize,
                                            String timeType,
                                            String startDate,
                                            String endDate,
                                            String warehouse,
                                            String documentCode,
                                            String itemName,
                                            String status,
                                            String checkRangeType,
                                            String printStatus,
                                            String generatedStatus,
                                            String remark,
                                            String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean canViewAll = inventoryCheckPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryCheckHeader> headers = inventoryCheckRepository.findHeadersByScopeAndKindOrdered(kind, scope.scopeType(), scope.scopeId());
        if (!canViewAll) {
            headers = headers.stream()
                    .filter(item -> Objects.equals(item.getCreatedBy(), operatorId))
                    .toList();
        }
        Map<Long, List<InventoryCheckLine>> lineMap = loadLineMap(kind, headers.stream().map(InventoryCheckHeader::getId).toList());
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String warehouseKeyword = toLower(trimNullable(warehouse));
        String documentCodeKeyword = toLower(trimNullable(documentCode));
        String itemKeyword = toLower(trimNullable(itemName));
        String statusKeyword = trimNullable(status);
        String rangeKeyword = trimNullable(checkRangeType);
        String printKeyword = trimNullable(printStatus);
        String generatedKeyword = trimNullable(generatedStatus);
        String remarkKeyword = toLower(trimNullable(remark));

        List<InventoryCheckRow> rows = headers.stream()
                .filter(header -> matchDate(header, timeType, start, end))
                .filter(header -> !StringUtils.hasText(warehouseKeyword)
                        || toLower(defaultIfBlank(header.getWarehouseName(), "")).contains(warehouseKeyword))
                .filter(header -> !StringUtils.hasText(documentCodeKeyword)
                        || toLower(defaultIfBlank(header.getDocumentCode(), "")).contains(documentCodeKeyword))
                .filter(header -> !StringUtils.hasText(statusKeyword)
                        || Objects.equals(defaultIfBlank(header.getStatus(), draftStatus()), statusKeyword))
                .filter(header -> !StringUtils.hasText(rangeKeyword)
                        || Objects.equals(defaultIfBlank(header.getCheckRangeType(), ""), rangeKeyword))
                .filter(header -> !StringUtils.hasText(printKeyword)
                        || Objects.equals(defaultIfBlank(header.getPrintStatus(), printStatusUnprinted()), printKeyword))
                .filter(header -> !StringUtils.hasText(generatedKeyword)
                        || Objects.equals(defaultIfBlank(header.getGeneratedStatus(), generatedUngeneratedStatus()), generatedKeyword))
                .filter(header -> !StringUtils.hasText(remarkKeyword)
                        || toLower(defaultIfBlank(header.getRemark(), "")).contains(remarkKeyword))
                .filter(header -> matchItem(lineMap.getOrDefault(header.getId(), List.of()), itemKeyword))
                .map(header -> toRow(kind, header, lineMap.getOrDefault(header.getId(), List.of())))
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
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @return 权限视图
     */
    public InventoryCheckPermissionView permissions(InventoryCheckKind kind, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        InventoryCheckPermissionService.PermissionSnapshot permissionSnapshot = inventoryCheckPermissionService.resolvePermissions(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
        return new InventoryCheckPermissionView(
                permissionSnapshot.canCreate(),
                permissionSnapshot.canUpdate(),
                permissionSnapshot.canDelete(),
                permissionSnapshot.canApprove(),
                permissionSnapshot.canUnapprove()
        );
    }

    /**
     * 创建盘点单。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 保存请求
     * @return 创建结果
     */
    @Transactional
    public IdPayload create(InventoryCheckKind kind, String orgId, InventoryCheckSaveRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "CREATE");
        InventoryCheckHeader header = saveDocument(kind, scope, null, request, true, operatorId);
        return new IdPayload(header.getId(), header.getDocumentCode());
    }

    /**
     * 查询盘点单详情。
     *
     * @param kind 盘点单类型
     * @param id 主键
     * @param orgId 机构标识
     * @return 详情
     */
    public InventoryCheckDetail detail(InventoryCheckKind kind, Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean canViewAll = inventoryCheckPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        InventoryCheckHeader header = requireHeader(kind, scope, id, operatorId, canViewAll);
        List<InventoryCheckLine> lines = inventoryCheckRepository.findLinesByHeaderId(kind, header.getId());
        return toDetail(header, lines);
    }

    /**
     * 更新盘点单。
     *
     * @param kind 盘点单类型
     * @param id 主键
     * @param orgId 机构标识
     * @param request 保存请求
     */
    @Transactional
    public void update(InventoryCheckKind kind, Long id, String orgId, InventoryCheckSaveRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        saveDocument(kind, scope, id, request, false, operatorId);
    }

    /**
     * 删除盘点单。
     *
     * @param kind 盘点单类型
     * @param id 主键
     * @param orgId 机构标识
     */
    @Transactional
    public void delete(InventoryCheckKind kind, Long id, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "DELETE");
        boolean canViewAll = inventoryCheckPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        InventoryCheckHeader header = requireHeader(kind, scope, id, operatorId, canViewAll);
        deleteInternal(kind, header);
    }

    /**
     * 批量删除。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchDelete(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "DELETE");
        boolean canViewAll = inventoryCheckPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, canViewAll);
        for (InventoryCheckHeader header : headers) {
            deleteInternal(kind, header);
        }
    }

    /**
     * 批量提交。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchSubmit(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, true);
        Map<Long, List<InventoryCheckLine>> lineMap = loadLineMap(kind, request.ids());
        for (InventoryCheckHeader header : headers) {
            submitHeader(kind, header, lineMap.getOrDefault(header.getId(), List.of()));
        }
    }

    /**
     * 批量审核。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchApprove(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureReviewPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, true);
        Map<Long, List<InventoryCheckLine>> lineMap = loadLineMap(kind, request.ids());
        for (InventoryCheckHeader header : headers) {
            approveHeader(kind, scope, header, lineMap.getOrDefault(header.getId(), List.of()), operatorId);
        }
    }

    /**
     * 批量反审核。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchUnapprove(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureReviewPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId);
        String rejectionReason = requiredTrim(request.rejectionReason(), "拒审原因不能为空");
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, true);
        Map<Long, List<InventoryCheckLine>> lineMap = loadLineMap(kind, request.ids());
        for (InventoryCheckHeader header : headers) {
            unapproveHeader(kind, scope, header, lineMap.getOrDefault(header.getId(), List.of()), operatorId, rejectionReason);
        }
    }

    /**
     * 批量打印。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void batchPrint(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, true);
        for (InventoryCheckHeader header : headers) {
            header.setPrintStatus(printStatusPrinted());
            inventoryCheckRepository.updateHeader(kind, header);
        }
    }

    /**
     * 批量生成多人盘点单。
     *
     * @param kind 盘点单类型
     * @param orgId 机构标识
     * @param request 批量请求
     */
    @Transactional
    public void generate(InventoryCheckKind kind, String orgId, InventoryCheckBatchRequest request) {
        if (!kind.isMulti()) {
            throw new BusinessException("当前类型不支持生成");
        }
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        inventoryCheckPermissionService.ensureOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        List<InventoryCheckHeader> headers = requireHeaders(kind, scope, request.ids(), operatorId, true);
        for (InventoryCheckHeader header : headers) {
            header.setGeneratedStatus(generatedStatusGenerated());
            inventoryCheckRepository.updateHeader(kind, header);
        }
    }

    private InventoryCheckHeader saveDocument(InventoryCheckKind kind,
                                              InventoryScope scope,
                                              Long id,
                                              InventoryCheckSaveRequest request,
                                              boolean createMode,
                                              Long operatorId) {
        InventoryCheckHeader header = createMode
                ? new InventoryCheckHeader()
                : requireHeader(kind, scope, id, operatorId, inventoryCheckPermissionService.canViewAll(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId));
        header.setScopeType(scope.scopeType());
        header.setScopeId(scope.scopeId());
        header.setCheckDate(parseRequiredDate(request.checkDate(), "盘点日期不能为空"));
        header.setWarehouseName(requiredTrim(request.warehouseName(), "盘点仓库不能为空"));
        header.setCheckRangeType(requiredTrim(request.checkRangeType(), "盘点范围类型不能为空"));
        header.setFreezeStock(Boolean.TRUE.equals(request.freezeStock()));
        header.setCollaborativeFlag(Boolean.TRUE.equals(request.collaborativeFlag()) || kind.isMulti());
        header.setPlanName(trimNullable(request.planName()));
        header.setThirdPartyDocument(trimNullable(request.thirdPartyDocument()));
        header.setSalesmanUserId(request.salesmanUserId());
        header.setSalesmanName(trimNullable(request.salesmanName()));
        header.setRemark(trimNullable(request.remark()));
        header.setStatus(Boolean.TRUE.equals(request.submitted()) ? submittedStatus() : draftStatus());
        header.setGeneratedStatus(kind.isMulti() ? generatedUngeneratedStatus() : null);
        header.setPrintStatus(printStatusUnprinted());
        header.setRejectionReason(null);
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        header.setCreatedBy(createMode ? operatorId : header.getCreatedBy());
        header.setItemCount(request.items() == null ? 0 : request.items().size());
        List<InventoryCheckLine> lines = normalizeLines(request.items());
        applyHeaderTotals(header, lines);
        if (createMode) {
            header.setDocumentCode(generateDocumentCode(kind, scope));
            inventoryCheckRepository.saveHeader(kind, header);
        } else {
            inventoryCheckRepository.updateHeader(kind, header);
            inventoryCheckRepository.deleteLinesByHeaderId(kind, header.getId());
        }
        for (InventoryCheckLine line : lines) {
            line.setHeaderId(header.getId());
            inventoryCheckRepository.saveLine(kind, line);
        }
        return header;
    }

    private InventoryCheckDetail toDetail(InventoryCheckHeader header, List<InventoryCheckLine> lines) {
        return new InventoryCheckDetail(
                header.getId(),
                defaultIfBlank(header.getDocumentCode(), ""),
                defaultIfBlank(header.getStatus(), draftStatus()),
                header.getCheckDate() == null ? "" : header.getCheckDate().toString(),
                defaultIfBlank(header.getWarehouseName(), ""),
                defaultIfBlank(header.getCheckRangeType(), ""),
                Boolean.TRUE.equals(header.getFreezeStock()),
                Boolean.TRUE.equals(header.getCollaborativeFlag()),
                defaultIfBlank(header.getPlanName(), ""),
                defaultIfBlank(header.getThirdPartyDocument(), ""),
                header.getSalesmanUserId(),
                defaultIfBlank(header.getSalesmanName(), ""),
                defaultIfBlank(header.getRemark(), ""),
                defaultIfBlank(header.getRejectionReason(), ""),
                defaultIfBlank(header.getGeneratedStatus(), generatedUngeneratedStatus()),
                defaultIfBlank(header.getPrintStatus(), printStatusUnprinted()),
                parseExtraJson(header.getExtraJson()),
                lines.stream().map(this::toDetailLine).toList()
        );
    }

    private InventoryCheckDetailLine toDetailLine(InventoryCheckLine line) {
        return new InventoryCheckDetailLine(
                defaultIfBlank(line.getItemCode(), ""),
                defaultIfBlank(line.getItemName(), ""),
                defaultIfBlank(line.getSpec(), ""),
                defaultIfBlank(line.getCategory(), ""),
                defaultIfBlank(line.getUnitName(), ""),
                line.getAvailableQty(),
                line.getBookQty(),
                line.getActualQty(),
                line.getBookPrice(),
                line.getBookAmount(),
                line.getActualAmount(),
                line.getDiffQty(),
                line.getDiffAmount(),
                line.getProfitQty(),
                line.getLossQty(),
                defaultIfBlank(line.getProfitLossReason(), ""),
                line.getProfitInboundPrice(),
                line.getProfitAmount(),
                line.getLossOutboundPrice(),
                line.getLossAmount(),
                defaultIfBlank(line.getAbnormalFlag(), ""),
                defaultIfBlank(line.getRemark(), ""),
                parseExtraJson(line.getExtraJson())
        );
    }

    private void submitHeader(InventoryCheckKind kind, InventoryCheckHeader header, List<InventoryCheckLine> lines) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
            return;
        }
        header.setStatus(submittedStatus());
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        inventoryCheckRepository.updateHeader(kind, header);
    }

    private void approveHeader(InventoryCheckKind kind,
                               InventoryScope scope,
                               InventoryCheckHeader header,
                               List<InventoryCheckLine> lines,
                               Long operatorId) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
            return;
        }
        if (!Objects.equals(header.getStatus(), submittedStatus())) {
            throw new BusinessException("单据未提交，无法审核");
        }
        for (InventoryCheckLine line : lines) {
            BigDecimal targetQuantity = normalizeNonNegative(line.getActualQty());
            inventoryStockMutationService.applyAbsolute(
                    scope.scopeType(),
                    scope.scopeId(),
                    header.getWarehouseName(),
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    targetQuantity,
                    kind.name() + "_APPROVE",
                    operatorId
            );
        }
        header.setStatus(approvedStatus());
        header.setApprovedBy(operatorId);
        header.setApprovedAt(LocalDateTime.now());
        header.setRejectionReason(null);
        inventoryCheckRepository.updateHeader(kind, header);
    }

    private void unapproveHeader(InventoryCheckKind kind,
                                 InventoryScope scope,
                                 InventoryCheckHeader header,
                                 List<InventoryCheckLine> lines,
                                 Long operatorId,
                                 String rejectionReason) {
        if (!Objects.equals(header.getStatus(), approvedStatus())) {
            header.setStatus(submittedStatus());
            header.setRejectionReason(rejectionReason);
            inventoryCheckRepository.updateHeader(kind, header);
            return;
        }
        for (InventoryCheckLine line : lines) {
            BigDecimal targetQuantity = normalizeNonNegative(line.getBookQty());
            inventoryStockMutationService.applyAbsolute(
                    scope.scopeType(),
                    scope.scopeId(),
                    header.getWarehouseName(),
                    header.getId(),
                    line.getId(),
                    line.getItemCode(),
                    line.getItemName(),
                    targetQuantity,
                    kind.name() + "_UNAPPROVE",
                    operatorId
            );
        }
        header.setStatus(submittedStatus());
        header.setApprovedBy(null);
        header.setApprovedAt(null);
        header.setRejectionReason(rejectionReason);
        inventoryCheckRepository.updateHeader(kind, header);
    }

    private void deleteInternal(InventoryCheckKind kind, InventoryCheckHeader header) {
        if (Objects.equals(header.getStatus(), approvedStatus())) {
            throw new BusinessException("已审核单据请先反审核后再删除");
        }
        inventoryCheckRepository.deleteLinesByHeaderId(kind, header.getId());
        inventoryCheckRepository.deleteHeaderById(kind, header.getId());
    }

    private List<InventoryCheckHeader> requireHeaders(InventoryCheckKind kind, InventoryScope scope, List<Long> ids, Long operatorId, boolean viewAll) {
        Set<Long> idSet = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (idSet.isEmpty()) {
            throw new BusinessException("单据ID不能为空");
        }
        List<InventoryCheckHeader> headers = inventoryCheckRepository.findHeadersByScopeAndKindAndIds(
                kind,
                scope.scopeType(),
                scope.scopeId(),
                operatorId,
                viewAll,
                new ArrayList<>(idSet)
        );
        if (headers.size() != idSet.size()) {
            throw new BusinessException("存在无效单据");
        }
        return headers;
    }

    private InventoryCheckHeader requireHeader(InventoryCheckKind kind,
                                               InventoryScope scope,
                                               Long id,
                                               Long operatorId,
                                               boolean viewAll) {
        return inventoryCheckRepository.findHeaderByScopeAndKindAndId(kind, scope.scopeType(), scope.scopeId(), operatorId, viewAll, id)
                .orElseThrow(() -> new BusinessException("单据不存在或无权查看"));
    }

    private Map<Long, List<InventoryCheckLine>> loadLineMap(InventoryCheckKind kind, List<Long> headerIds) {
        return inventoryCheckRepository.findLinesByHeaderIds(kind, headerIds).stream()
                .collect(Collectors.groupingBy(InventoryCheckLine::getHeaderId, LinkedHashMap::new, Collectors.toList()));
    }

    private boolean matchDate(InventoryCheckHeader header, String timeType, LocalDate start, LocalDate end) {
        LocalDate value = Objects.equals(timeType, "创建时间")
                ? (header.getCreatedAt() == null ? null : header.getCreatedAt().toLocalDate())
                : header.getCheckDate();
        if (value == null) {
            return false;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        return end == null || !value.isAfter(end);
    }

    private boolean matchItem(List<InventoryCheckLine> lines, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return lines.stream().anyMatch(line ->
                toLower(defaultIfBlank(line.getItemCode(), "")).contains(keyword)
                        || toLower(defaultIfBlank(line.getItemName(), "")).contains(keyword));
    }

    private InventoryCheckRow toRow(InventoryCheckKind kind, InventoryCheckHeader header, List<InventoryCheckLine> lines) {
        BigDecimal totalBookAmount = header.getTotalBookAmount() == null ? sumAmount(lines, true) : header.getTotalBookAmount();
        BigDecimal totalActualAmount = header.getTotalActualAmount() == null ? sumAmount(lines, false) : header.getTotalActualAmount();
        BigDecimal totalDiffAmount = header.getTotalDiffAmount() == null
                ? totalActualAmount.subtract(totalBookAmount)
                : header.getTotalDiffAmount();
        String diffStatus = defaultIfBlank(header.getDiffStatus(), deriveDiffStatus(totalDiffAmount));
        String generatedStatus = kind.isMulti()
                ? defaultIfBlank(header.getGeneratedStatus(), generatedUngeneratedStatus())
                : "";
        return new InventoryCheckRow(
                header.getId(),
                defaultIfBlank(header.getDocumentCode(), ""),
                header.getCheckDate() == null ? "" : header.getCheckDate().toString(),
                defaultIfBlank(header.getWarehouseName(), ""),
                lines.size(),
                formatMoney(totalBookAmount),
                formatMoney(totalActualAmount),
                formatMoney(totalDiffAmount),
                defaultIfBlank(header.getCheckRangeType(), ""),
                defaultIfBlank(header.getStatus(), draftStatus()),
                diffStatus,
                header.getApprovedAt() == null ? "" : formatDateTime(header.getApprovedAt()),
                defaultIfBlank(header.getPrintStatus(), printStatusUnprinted()),
                generatedStatus,
                formatDateTime(header.getCreatedAt()),
                header.getCreatedBy() == null ? "" : String.valueOf(header.getCreatedBy()),
                defaultIfBlank(header.getRemark(), "")
        );
    }

    private List<InventoryCheckLine> normalizeLines(List<InventoryCheckSaveRequest.InventoryCheckLineRequest> items) {
        List<InventoryCheckLine> rows = new ArrayList<>();
        for (InventoryCheckSaveRequest.InventoryCheckLineRequest item : items) {
            InventoryCheckLine line = new InventoryCheckLine();
            line.setItemCode(requiredTrim(item.itemCode(), "物品编码不能为空"));
            line.setItemName(requiredTrim(item.itemName(), "物品名称不能为空"));
            line.setSpec(trimNullable(item.spec()));
            line.setCategory(trimNullable(item.category()));
            line.setUnitName(trimNullable(item.unitName()));
            line.setAvailableQty(normalizeNullableNonNegative(item.availableQty()));
            line.setBookQty(normalizeNullableNonNegative(item.bookQty()));
            line.setActualQty(normalizeNullableNonNegative(item.actualQty()));
            line.setBookPrice(normalizeNullableNonNegative(item.bookPrice()));
            line.setBookAmount(line.getBookQty() != null && line.getBookPrice() != null
                    ? line.getBookQty().multiply(line.getBookPrice()).setScale(2, RoundingMode.HALF_UP)
                    : null);
            line.setActualAmount(line.getActualQty() != null && line.getBookPrice() != null
                    ? line.getActualQty().multiply(line.getBookPrice()).setScale(2, RoundingMode.HALF_UP)
                    : null);
            line.setDiffQty(line.getActualQty() != null && line.getBookQty() != null
                    ? line.getActualQty().subtract(line.getBookQty()).setScale(4, RoundingMode.HALF_UP)
                    : null);
            line.setDiffAmount(line.getActualAmount() != null && line.getBookAmount() != null
                    ? line.getActualAmount().subtract(line.getBookAmount()).setScale(2, RoundingMode.HALF_UP)
                    : null);
            line.setProfitQty(line.getDiffQty() == null
                    ? null
                    : (line.getDiffQty().compareTo(BigDecimal.ZERO) > 0 ? line.getDiffQty() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)));
            line.setLossQty(line.getDiffQty() == null
                    ? null
                    : (line.getDiffQty().compareTo(BigDecimal.ZERO) < 0 ? line.getDiffQty().abs().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)));
            line.setProfitLossReason(trimNullable(item.profitLossReason()));
            line.setProfitInboundPrice(line.getBookPrice());
            line.setProfitAmount(line.getProfitQty() != null && line.getBookPrice() != null
                    ? line.getProfitQty().multiply(line.getBookPrice()).setScale(2, RoundingMode.HALF_UP)
                    : null);
            line.setLossOutboundPrice(line.getBookPrice());
            line.setLossAmount(line.getLossQty() != null && line.getBookPrice() != null
                    ? line.getLossQty().multiply(line.getBookPrice()).setScale(2, RoundingMode.HALF_UP)
                    : null);
            line.setAbnormalFlag(line.getDiffQty() == null ? null : (Objects.equals(line.getDiffQty(), BigDecimal.ZERO) ? "NORMAL" : "ABNORMAL"));
            line.setRemark(trimNullable(item.remark()));
            line.setExtraJson(writeJson(item.extraFields()));
            rows.add(line);
        }
        return rows;
    }

    private void applyHeaderTotals(InventoryCheckHeader header, List<InventoryCheckLine> lines) {
        header.setItemCount(lines.size());
        header.setTotalBookAmount(sumAmount(lines, true));
        header.setTotalActualAmount(sumAmount(lines, false));
        header.setTotalDiffAmount(header.getTotalActualAmount().subtract(header.getTotalBookAmount()).setScale(2, RoundingMode.HALF_UP));
        header.setDiffStatus(deriveDiffStatus(header.getTotalDiffAmount()));
    }

    private BigDecimal sumAmount(List<InventoryCheckLine> lines, boolean bookAmount) {
        return lines.stream()
                .map(bookAmount ? InventoryCheckLine::getBookAmount : InventoryCheckLine::getActualAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String deriveDiffStatus(BigDecimal diffAmount) {
        if (diffAmount == null || diffAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "NO_DIFF";
        }
        return diffAmount.compareTo(BigDecimal.ZERO) > 0 ? "PROFIT" : "LOSS";
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new InventoryScope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private String generateDocumentCode(InventoryCheckKind kind, InventoryScope scope) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT));
        for (int cursor = 1; cursor < 999; cursor++) {
            int seed = ThreadLocalRandom.current().nextInt(1, 999);
            String suffix = String.format(Locale.ROOT, "%03d", (cursor + seed) % 1000);
            String candidate = kind.getDocumentPrefix() + "-" + datePart + "-" + suffix;
            if (inventoryCheckRepository.countByScopeAndKindAndDocumentCode(kind, scope.scopeType(), scope.scopeId(), candidate) == 0) {
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

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("数值不能小于 0");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableNonNegative(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("数值不能小于 0");
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

    private String draftStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.DRAFT);
    }

    private String submittedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.SUBMITTED);
    }

    private String approvedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_DOCUMENT_STATUS, DictionaryCodes.APPROVED);
    }

    private String printStatusUnprinted() {
        return dictionaryLookupService.codeOf(DictionaryCodes.DOCUMENT_PRINT_STATUS, "UNPRINTED");
    }

    private String printStatusPrinted() {
        return dictionaryLookupService.codeOf(DictionaryCodes.DOCUMENT_PRINT_STATUS, "PRINTED");
    }

    private String generatedUngeneratedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_CHECK_GENERATION_STATUS, "UNGENERATED");
    }

    private String generatedStatusGenerated() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_CHECK_GENERATION_STATUS, "GENERATED");
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATETIME_FORMATTER.format(value);
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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
     * @param checkDate 盘点日期
     * @param warehouseName 仓库
     * @param itemCount 物品数
     * @param totalBookAmount 账面金额
     * @param totalActualAmount 实盘金额
     * @param totalDiffAmount 差异金额
     * @param checkRangeType 盘点范围类型
     * @param status 状态
     * @param diffStatus 差异状态
     * @param auditDate 审核日期
     * @param printStatus 打印状态
     * @param generatedStatus 生成状态
     * @param createdAt 创建时间
     * @param creator 创建人
     * @param remark 备注
     */
    public record InventoryCheckRow(Long id,
                                    String documentCode,
                                    String checkDate,
                                    String warehouseName,
                                    Integer itemCount,
                                    String totalBookAmount,
                                    String totalActualAmount,
                                    String totalDiffAmount,
                                    String checkRangeType,
                                    String status,
                                    String diffStatus,
                                    String auditDate,
                                    String printStatus,
                                    String generatedStatus,
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
     * @param checkDate 盘点日期
     * @param warehouseName 仓库
     * @param checkRangeType 盘点范围类型
     * @param freezeStock 是否冻结库存
     * @param collaborativeFlag 是否多人协同盘点
     * @param planName 盘点方案名称
     * @param thirdPartyDocument 第三方单号
     * @param salesmanUserId 盘点人用户 ID
     * @param salesmanName 盘点人名称
     * @param remark 备注
     * @param rejectionReason 拒审原因
     * @param generatedStatus 生成状态
     * @param printStatus 打印状态
     * @param extraFields 扩展字段
     * @param items 明细
     */
    public record InventoryCheckDetail(Long id,
                                       String documentCode,
                                       String status,
                                       String checkDate,
                                       String warehouseName,
                                       String checkRangeType,
                                       boolean freezeStock,
                                       boolean collaborativeFlag,
                                       String planName,
                                       String thirdPartyDocument,
                                       Long salesmanUserId,
                                       String salesmanName,
                                       String remark,
                                       String rejectionReason,
                                       String generatedStatus,
                                       String printStatus,
                                       Map<String, String> extraFields,
                                       List<InventoryCheckDetailLine> items) {
    }

    /**
     * 详情明细。
     *
     * @param itemCode 物品编码
     * @param itemName 物品名称
     * @param spec 规格
     * @param category 分类
     * @param unitName 单位
     * @param availableQty 账面可用数量
     * @param bookQty 账面数量
     * @param actualQty 实盘数量
     * @param bookPrice 账面单价
     * @param bookAmount 账面金额
     * @param actualAmount 实盘金额
     * @param diffQty 差异数量
     * @param diffAmount 差异金额
     * @param profitQty 盘盈数量
     * @param lossQty 盘亏数量
     * @param profitLossReason 盈亏原因
     * @param profitInboundPrice 盘盈入库单价
     * @param profitAmount 盘盈金额
     * @param lossOutboundPrice 盘亏出库单价
     * @param lossAmount 盘亏金额
     * @param abnormalFlag 异常标识
     * @param remark 备注
     * @param extraFields 扩展字段
     */
    public record InventoryCheckDetailLine(String itemCode,
                                           String itemName,
                                           String spec,
                                           String category,
                                           String unitName,
                                           BigDecimal availableQty,
                                           BigDecimal bookQty,
                                           BigDecimal actualQty,
                                           BigDecimal bookPrice,
                                           BigDecimal bookAmount,
                                           BigDecimal actualAmount,
                                           BigDecimal diffQty,
                                           BigDecimal diffAmount,
                                           BigDecimal profitQty,
                                           BigDecimal lossQty,
                                           String profitLossReason,
                                           BigDecimal profitInboundPrice,
                                           BigDecimal profitAmount,
                                           BigDecimal lossOutboundPrice,
                                           BigDecimal lossAmount,
                                           String abnormalFlag,
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
    public record InventoryCheckPermissionView(boolean canCreate,
                                               boolean canUpdate,
                                               boolean canDelete,
                                               boolean canApprove,
                                               boolean canUnapprove) {
    }

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }
}
