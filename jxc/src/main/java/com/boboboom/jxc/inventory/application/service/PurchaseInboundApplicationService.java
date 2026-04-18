package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.inventory.domain.repository.InventoryBalanceRepository;
import com.boboboom.jxc.inventory.domain.repository.InventoryTransactionRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
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

    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundLineRepository purchaseInboundLineRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PurchaseInboundWorkflowService purchaseInboundWorkflowService;
    private final OrgScopeService orgScopeService;
    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;

    public PurchaseInboundApplicationService(PurchaseInboundRepository purchaseInboundRepository,
                                             PurchaseInboundLineRepository purchaseInboundLineRepository,
                                             InventoryBalanceRepository inventoryBalanceRepository,
                                             InventoryTransactionRepository inventoryTransactionRepository,
                                             PurchaseInboundWorkflowService purchaseInboundWorkflowService,
                                             OrgScopeService orgScopeService,
                                             UserRoleRelRepository userRoleRelRepository,
                                             RoleRepository roleRepository) {
        this.purchaseInboundRepository = purchaseInboundRepository;
        this.purchaseInboundLineRepository = purchaseInboundLineRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.purchaseInboundWorkflowService = purchaseInboundWorkflowService;
        this.orgScopeService = orgScopeService;
        this.userRoleRelRepository = userRoleRelRepository;
        this.roleRepository = roleRepository;
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
        boolean viewAll = canViewAllPurchaseInbound(scope, currentUserId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        String timeTypeValue = trimNullable(timeType);
        LocalDate start = parseDateNullable(startDate, "开始日期格式不正确");
        LocalDate end = parseDateNullable(endDate, "结束日期格式不正确");
        String warehouseValue = trimNullable(warehouse);
        String codeKeyword = toLower(trimNullable(documentCode));
        String supplierValue = trimNullable(supplier);
        String itemKeyword = toLower(trimNullable(itemName));
        String statusValue = trimNullable(documentStatus);
        String reviewStatusValue = trimNullable(reviewStatus);
        String reconciliationStatusValue = trimNullable(reconciliationStatus);
        String splitStatusValue = trimNullable(splitStatus);
        String upstreamKeyword = toLower(trimNullable(upstreamCode));
        String invoiceStatusValue = trimNullable(invoiceStatus);
        String inspectionCountValue = trimNullable(inspectionCount);
        String printStatusValue = trimNullable(printStatus);
        String remarkKeyword = toLower(trimNullable(remark));

        List<PurchaseInboundDO> headers = purchaseInboundRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(header -> viewAll || Objects.equals(header.getCreatedBy(), currentUserId))
                .toList();
        if (headers.isEmpty()) {
            return new PageData<>(List.of(), 0, safePageNo, safePageSize);
        }

        List<Long> headerIds = headers.stream().map(PurchaseInboundDO::getId).toList();
        Map<Long, List<PurchaseInboundLineDO>> lineMap = purchaseInboundLineRepository.findByInboundIds(headerIds).stream()
                .collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));

        String currentUserName = AuthContextHolder.userNameOr("未知用户");
        DecimalFormat amountFormat = new DecimalFormat("#,##0.00");

        List<PurchaseInboundRow> filtered = headers.stream()
                .filter(header -> matchDate(header, timeTypeValue, start, end))
                .filter(header -> !StringUtils.hasText(warehouseValue) || Objects.equals(header.getWarehouseName(), warehouseValue))
                .filter(header -> !StringUtils.hasText(codeKeyword) || toLower(header.getDocumentCode()).contains(codeKeyword))
                .filter(header -> !StringUtils.hasText(supplierValue) || Objects.equals(header.getSupplierName(), supplierValue))
                .filter(header -> !StringUtils.hasText(statusValue) || Objects.equals(header.getStatus(), statusValue))
                .filter(header -> !StringUtils.hasText(reviewStatusValue) || Objects.equals(deriveReviewStatus(header), reviewStatusValue))
                .filter(header -> !StringUtils.hasText(reconciliationStatusValue)
                        || Objects.equals(deriveReconciliationStatus(), reconciliationStatusValue))
                .filter(header -> !StringUtils.hasText(splitStatusValue) || Objects.equals(deriveSplitStatus(), splitStatusValue))
                .filter(header -> !StringUtils.hasText(upstreamKeyword) || toLower(header.getUpstreamCode()).contains(upstreamKeyword))
                .filter(header -> !StringUtils.hasText(invoiceStatusValue) || Objects.equals(deriveInvoiceStatus(), invoiceStatusValue))
                .filter(header -> !StringUtils.hasText(inspectionCountValue)
                        || Objects.equals(deriveInspectionCount(), inspectionCountValue))
                .filter(header -> !StringUtils.hasText(printStatusValue) || Objects.equals(derivePrintStatus(), printStatusValue))
                .filter(header -> !StringUtils.hasText(remarkKeyword) || toLower(header.getRemark()).contains(remarkKeyword))
                .filter(header -> matchItem(lineMap.getOrDefault(header.getId(), List.of()), itemKeyword))
                .map(header -> {
                    List<PurchaseInboundLineDO> lines = lineMap.getOrDefault(header.getId(), List.of());
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
                            amountFormat.format(amount),
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
                })
                .toList();

        List<PurchaseInboundRow> pageList = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + safePageSize, filtered.size()); i++) {
            pageList.add(filtered.get(i));
        }
        return new PageData<>(pageList, filtered.size(), safePageNo, safePageSize);
    }

    public PurchaseInboundPermissionView purchaseInboundPermissions(String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean canCreate = canViewAllPurchaseInbound(scope, operatorId)
                || purchaseInboundWorkflowService.hasBusinessOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "CREATE");
        boolean canUpdate = canViewAllPurchaseInbound(scope, operatorId)
                || purchaseInboundWorkflowService.hasBusinessOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "UPDATE");
        boolean canDelete = canViewAllPurchaseInbound(scope, operatorId)
                || purchaseInboundWorkflowService.hasBusinessOperationPermission(scope.scopeType(), scope.scopeId(), scope.groupId(), operatorId, "DELETE");
        boolean canApprove = purchaseInboundWorkflowService.hasBusinessReviewPermission(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
        boolean canUnapprove = canApprove;
        return new PurchaseInboundPermissionView(canCreate, canUpdate, canDelete, canApprove, canUnapprove);
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
        PurchaseInboundDO header = requireHeader(scope, id);
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
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(request.ids());

        for (PurchaseInboundDO header : headers) {
            if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                continue;
            }
            List<PurchaseInboundLineDO> lines = lineMap.getOrDefault(header.getId(), List.of());
            PurchaseInboundWorkflowService.ApprovalResult workflowResult = purchaseInboundWorkflowService.completeCurrentTask(header, operatorId);
            if (!workflowResult.workflowApplied()) {
                if ("DELETE".equals(header.getPendingOperation())) {
                    deletePurchaseInboundInternal(header);
                    continue;
                }
                for (PurchaseInboundLineDO line : lines) {
                    applyInventoryDelta(scope, header, line, line.getQuantity(), "PURCHASE_INBOUND_APPROVE", operatorId);
                }
                header.setStatus(STATUS_APPROVED);
                header.setApprovedBy(operatorId);
                header.setApprovedAt(LocalDateTime.now());
                header.setPendingOperation("NONE");
                purchaseInboundRepository.update(header);
                continue;
            }

            if (workflowResult.completed()) {
                if ("DELETE".equals(header.getPendingOperation())) {
                    deletePurchaseInboundInternal(header);
                    continue;
                }
                for (PurchaseInboundLineDO line : lines) {
                    applyInventoryDelta(scope, header, line, line.getQuantity(), "PURCHASE_INBOUND_APPROVE", operatorId);
                }
                header.setStatus(STATUS_APPROVED);
                header.setApprovedBy(operatorId);
                header.setApprovedAt(LocalDateTime.now());
                header.setPendingOperation("NONE");
            } else {
                header.setStatus(STATUS_SUBMITTED);
                header.setApprovedBy(null);
                header.setApprovedAt(null);
            }
            purchaseInboundRepository.update(header);
        }
    }

    @Transactional
    public void batchUnapprove(String orgId, PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        ensurePurchaseInboundReviewPermission(scope);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(request.ids());

        for (PurchaseInboundDO header : headers) {
            if (!Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                if ("DELETE".equals(header.getPendingOperation())) {
                    header.setPendingOperation("NONE");
                    if (hasWorkflowMetadata(header)) {
                        purchaseInboundWorkflowService.resetWorkflowState(header);
                    } else {
                        purchaseInboundRepository.update(header);
                    }
                }
                continue;
            }
            List<PurchaseInboundLineDO> lines = lineMap.getOrDefault(header.getId(), List.of());
            for (PurchaseInboundLineDO line : lines) {
                applyInventoryDelta(scope, header, line, line.getQuantity().negate(), "PURCHASE_INBOUND_UNAPPROVE", operatorId);
            }
            header.setStatus(STATUS_SUBMITTED);
            header.setApprovedBy(null);
            header.setApprovedAt(null);
            header.setPendingOperation("NONE");
            if (hasWorkflowMetadata(header)) {
                purchaseInboundWorkflowService.resetWorkflowState(header);
            } else {
                purchaseInboundRepository.update(header);
            }
        }
    }

    public List<InventoryBalanceRow> listBalances(String warehouse, String itemName, String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        String warehouseValue = trimNullable(warehouse);
        String itemKeyword = toLower(trimNullable(itemName));
        return inventoryBalanceRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())
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
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope, List<Long> ids) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        return requireHeaders(scope, ids, operatorId);
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope, List<Long> ids, Long operatorId) {
        Set<Long> idSet = new LinkedHashSet<>(ids);
        if (idSet.isEmpty()) {
            throw new BusinessException("单据ID不能为空");
        }
        boolean viewAll = canViewAllPurchaseInbound(scope, operatorId);
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
        boolean viewAll = canViewAllPurchaseInbound(scope, operatorId);
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
        PurchaseInboundDO header = createMode
                ? new PurchaseInboundDO()
                : requireHeader(scope, id, operatorId);
        if (!createMode && Objects.equals(header.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException("已审核单据不允许编辑，请先反审核");
        }
        if (createMode) {
            header.setScopeType(scope.scopeType());
            header.setScopeId(scope.scopeId());
            header.setDocumentCode(generateDocumentCode(scope));
            header.setCreatedBy(operatorId);
        }
        if (createMode && request.salesmanUserId() == null) {
            throw new BusinessException("业务员不能为空");
        }
        if (isStoreSalesmanOperator(scope, operatorId) && !Objects.equals(request.salesmanUserId(), operatorId)) {
            throw new BusinessException("业务员只能选择本人");
        }
        header.setInboundDate(parseRequiredDate(request.inboundDate(), "入库日期格式不正确"));
        header.setWarehouseName(requiredTrim(request.warehouse(), "仓库不能为空"));
        header.setSupplierName(requiredTrim(request.supplier(), "供应商不能为空"));
        header.setSalesmanUserId(request.salesmanUserId());
        header.setSalesmanName(trimNullable(request.salesmanName()));
        header.setUpstreamCode(trimNullable(request.upstreamCode()));
        header.setRemark(trimNullable(request.remark()));
        header.setStatus(createMode || !StringUtils.hasText(header.getStatus()) ? STATUS_SUBMITTED : header.getStatus());
        if (createMode) {
            purchaseInboundRepository.save(header);
        } else {
            purchaseInboundRepository.update(header);
            purchaseInboundLineRepository.deleteByInboundId(header.getId());
        }

        for (PurchaseInboundCreateRequest.LineItem item : request.items()) {
            PurchaseInboundLineDO line = new PurchaseInboundLineDO();
            line.setInboundId(header.getId());
            line.setItemCode(requiredTrim(item.itemCode(), "物品编码不能为空"));
            line.setItemName(requiredTrim(item.itemName(), "物品名称不能为空"));
            line.setQuantity(normalizePositive(item.quantity(), "数量必须大于0"));
            line.setUnitPrice(normalizeNonNegative(item.unitPrice(), "单价不能小于0"));
            line.setTaxRate(normalizeNonNegative(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate(), "税率不能小于0"));
            purchaseInboundLineRepository.save(line);
        }
        boolean workflowApplied = purchaseInboundWorkflowService.syncOnAction(scope.scopeType(), scope.scopeId(), scope.groupId(), header, operatorId, action);
        if (!workflowApplied) {
            header.setPendingOperation("NONE");
            purchaseInboundRepository.update(header);
        }
        return header;
    }

    private Map<Long, List<PurchaseInboundLineDO>> loadLineMap(List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Map.of();
        }
        return purchaseInboundLineRepository.findByInboundIds(headerIds)
                .stream()
                .collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));
    }

    private void applyInventoryDelta(InventoryScope scope,
                                     PurchaseInboundDO header,
                                     PurchaseInboundLineDO line,
                                     BigDecimal delta,
                                     String bizType,
                                     Long operatorId) {
        InventoryBalanceDO balance = inventoryBalanceRepository.findByScopeWarehouseAndItem(
                scope.scopeType(), scope.scopeId(), header.getWarehouseName(), line.getItemCode()
        ).orElse(null);
        BigDecimal before = balance == null || balance.getQuantity() == null ? BigDecimal.ZERO : balance.getQuantity();
        BigDecimal after = before.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存不足，无法反审核");
        }

        if (balance == null) {
            balance = new InventoryBalanceDO();
            balance.setScopeType(scope.scopeType());
            balance.setScopeId(scope.scopeId());
            balance.setWarehouseName(header.getWarehouseName());
            balance.setItemCode(line.getItemCode());
            balance.setItemName(line.getItemName());
            balance.setQuantity(after);
            inventoryBalanceRepository.save(balance);
        } else {
            balance.setItemName(line.getItemName());
            balance.setQuantity(after);
            inventoryBalanceRepository.update(balance);
        }

        InventoryTransactionDO transaction = new InventoryTransactionDO();
        transaction.setScopeType(scope.scopeType());
        transaction.setScopeId(scope.scopeId());
        transaction.setBizType(bizType);
        transaction.setBizId(header.getId());
        transaction.setBizLineId(line.getId());
        transaction.setWarehouseName(header.getWarehouseName());
        transaction.setItemCode(line.getItemCode());
        transaction.setItemName(line.getItemName());
        transaction.setQuantityDelta(delta);
        transaction.setBeforeQty(before);
        transaction.setAfterQty(after);
        transaction.setOperatorId(operatorId);
        inventoryTransactionRepository.save(transaction);
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

    private boolean canViewAllPurchaseInbound(InventoryScope scope, Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        if (hasRoleInScope(operatorId, "STORE_ADMIN", "STORE", scope.scopeId())) {
            return true;
        }
        return "STORE".equals(scope.scopeType())
                && scope.groupId() != null
                && hasRoleInScope(operatorId, "GROUP_ADMIN", "GROUP", scope.groupId());
    }

    private boolean isStoreSalesmanOperator(InventoryScope scope, Long operatorId) {
        return "STORE".equals(scope.scopeType())
                && scope.scopeId() != null
                && hasRoleInScope(operatorId, "SALESMAN", "STORE", scope.scopeId());
    }

    private void ensurePurchaseInboundOperationPermission(InventoryScope scope, String action) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        if (canViewAllPurchaseInbound(scope, operatorId)) {
            return;
        }
        boolean allowed = purchaseInboundWorkflowService.hasBusinessOperationPermission(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId,
                action
        );
        if (!allowed) {
            throw new BusinessException("当前账号无采购入库操作权限");
        }
    }

    private void ensurePurchaseInboundReviewPermission(InventoryScope scope) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        boolean allowed = purchaseInboundWorkflowService.hasBusinessReviewPermission(
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        );
        if (!allowed) {
            throw new BusinessException("当前账号无采购入库审核权限");
        }
    }

    private boolean hasRoleInScope(Long operatorId, String roleCode, String scopeType, Long scopeId) {
        if (operatorId == null || scopeId == null) {
            return false;
        }
        RoleDO role = roleRepository.findByRoleCode(roleCode).orElse(null);
        if (role == null) {
            return false;
        }
        return userRoleRelRepository.findByUserIdRoleAndScope(operatorId, role.getId(), scopeType, scopeId).isPresent();
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

    private boolean hasWorkflowMetadata(PurchaseInboundDO header) {
        return StringUtils.hasText(header.getWorkflowProcessCode())
                || StringUtils.hasText(header.getWorkflowDefinitionKey())
                || StringUtils.hasText(header.getWorkflowDefinitionId())
                || StringUtils.hasText(header.getWorkflowInstanceId())
                || StringUtils.hasText(header.getWorkflowTaskId())
                || StringUtils.hasText(header.getWorkflowTaskName())
                || (StringUtils.hasText(header.getWorkflowStatus()) && !"NONE".equals(header.getWorkflowStatus()));
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

    private record InventoryScope(String scopeType, Long scopeId, Long groupId) {
    }
}
