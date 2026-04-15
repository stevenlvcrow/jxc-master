package com.boboboom.jxc.inventory.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryBalanceMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryTransactionMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundLineMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundMapper;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundBatchRequest;
import com.boboboom.jxc.inventory.interfaces.rest.request.PurchaseInboundCreateRequest;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@Validated
@RestController
@RequestMapping("/api/inventory")
public class PurchaseInboundController {

    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private static final String STATUS_DRAFT = "草稿";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_APPROVED = "已审核";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final PurchaseInboundMapper purchaseInboundMapper;
    private final PurchaseInboundLineMapper purchaseInboundLineMapper;
    private final InventoryBalanceMapper inventoryBalanceMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public PurchaseInboundController(PurchaseInboundMapper purchaseInboundMapper,
                                     PurchaseInboundLineMapper purchaseInboundLineMapper,
                                     InventoryBalanceMapper inventoryBalanceMapper,
                                     InventoryTransactionMapper inventoryTransactionMapper,
                                     RoleMapper roleMapper,
                                     UserRoleRelMapper userRoleRelMapper,
                                     StoreMapper storeMapper) {
        this.purchaseInboundMapper = purchaseInboundMapper;
        this.purchaseInboundLineMapper = purchaseInboundLineMapper;
        this.inventoryBalanceMapper = inventoryBalanceMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping("/purchase-inbound")
    public CodeDataResponse<PageData<PurchaseInboundRow>> listPurchaseInbound(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String timeType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String documentStatus,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String reconciliationStatus,
            @RequestParam(required = false) String splitStatus,
            @RequestParam(required = false) String upstreamCode,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) Boolean adjustedPrice,
            @RequestParam(required = false) String inspectionCount,
            @RequestParam(required = false) String printStatus,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
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
        Boolean adjustedPriceValue = adjustedPrice;
        String inspectionCountValue = trimNullable(inspectionCount);
        String printStatusValue = trimNullable(printStatus);
        String remarkKeyword = toLower(trimNullable(remark));

        List<PurchaseInboundDO> headers = purchaseInboundMapper.selectList(scopeQuery(scope)
                .orderByDesc(PurchaseInboundDO::getCreatedAt)
                .orderByDesc(PurchaseInboundDO::getId));
        if (headers.isEmpty()) {
            return CodeDataResponse.ok(new PageData<>(List.of(), 0, safePageNo, safePageSize));
        }

        List<Long> headerIds = headers.stream().map(PurchaseInboundDO::getId).toList();
        Map<Long, List<PurchaseInboundLineDO>> lineMap = purchaseInboundLineMapper.selectList(
                        new LambdaQueryWrapper<PurchaseInboundLineDO>()
                                .in(PurchaseInboundLineDO::getInboundId, headerIds))
                .stream()
                .collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));

        Long currentUserId = currentUserId();
        String currentUserName = currentUserName();
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
                .filter(header -> adjustedPriceValue == null || !adjustedPriceValue)
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
                            defaultIfBlank(header.getRemark(), "")
                    );
                })
                .toList();

        List<PurchaseInboundRow> pageList = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + safePageSize, filtered.size()); i++) {
            pageList.add(filtered.get(i));
        }
        return CodeDataResponse.ok(new PageData<>(pageList, filtered.size(), safePageNo, safePageSize));
    }

    @PostMapping("/purchase-inbound")
    @Transactional
    public CodeDataResponse<IdPayload> createPurchaseInbound(@RequestParam(required = false) String orgId,
                                                             @Valid @RequestBody PurchaseInboundCreateRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = currentUserId();

        PurchaseInboundDO header = new PurchaseInboundDO();
        header.setScopeType(scope.scopeType());
        header.setScopeId(scope.scopeId());
        header.setDocumentCode(generateDocumentCode(scope));
        header.setInboundDate(parseRequiredDate(request.inboundDate(), "入库日期格式不正确"));
        header.setWarehouseName(requiredTrim(request.warehouse(), "仓库不能为空"));
        header.setSupplierName(requiredTrim(request.supplier(), "供应商不能为空"));
        header.setUpstreamCode(trimNullable(request.upstreamCode()));
        header.setStatus(STATUS_SUBMITTED);
        header.setRemark(trimNullable(request.remark()));
        header.setCreatedBy(operatorId);
        purchaseInboundMapper.insert(header);

        for (PurchaseInboundCreateRequest.LineItem item : request.items()) {
            PurchaseInboundLineDO line = new PurchaseInboundLineDO();
            line.setInboundId(header.getId());
            line.setItemCode(requiredTrim(item.itemCode(), "物品编码不能为空"));
            line.setItemName(requiredTrim(item.itemName(), "物品名称不能为空"));
            line.setQuantity(normalizePositive(item.quantity(), "数量必须大于0"));
            line.setUnitPrice(normalizeNonNegative(item.unitPrice(), "单价不能小于0"));
            line.setTaxRate(normalizeNonNegative(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate(), "税率不能小于0"));
            purchaseInboundLineMapper.insert(line);
        }
        return CodeDataResponse.ok(new IdPayload(header.getId(), header.getDocumentCode()));
    }

    @PostMapping("/purchase-inbound/batch-approve")
    @Transactional
    public CodeDataResponse<Void> batchApprove(@RequestParam(required = false) String orgId,
                                               @Valid @RequestBody PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = currentUserId();
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(request.ids());

        for (PurchaseInboundDO header : headers) {
            if (Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                continue;
            }
            List<PurchaseInboundLineDO> lines = lineMap.getOrDefault(header.getId(), List.of());
            for (PurchaseInboundLineDO line : lines) {
                applyInventoryDelta(scope, header, line, line.getQuantity(), "PURCHASE_INBOUND_APPROVE", operatorId);
            }
            header.setStatus(STATUS_APPROVED);
            header.setApprovedBy(operatorId);
            header.setApprovedAt(LocalDateTime.now());
            purchaseInboundMapper.updateById(header);
        }
        return CodeDataResponse.ok();
    }

    @PostMapping("/purchase-inbound/batch-unapprove")
    @Transactional
    public CodeDataResponse<Void> batchUnapprove(@RequestParam(required = false) String orgId,
                                                 @Valid @RequestBody PurchaseInboundBatchRequest request) {
        InventoryScope scope = resolveInventoryScope(orgId);
        Long operatorId = currentUserId();
        List<PurchaseInboundDO> headers = requireHeaders(scope, request.ids());
        Map<Long, List<PurchaseInboundLineDO>> lineMap = loadLineMap(request.ids());

        for (PurchaseInboundDO header : headers) {
            if (!Objects.equals(header.getStatus(), STATUS_APPROVED)) {
                continue;
            }
            List<PurchaseInboundLineDO> lines = lineMap.getOrDefault(header.getId(), List.of());
            for (PurchaseInboundLineDO line : lines) {
                applyInventoryDelta(scope, header, line, line.getQuantity().negate(), "PURCHASE_INBOUND_UNAPPROVE", operatorId);
            }
            header.setStatus(STATUS_SUBMITTED);
            header.setApprovedBy(null);
            header.setApprovedAt(null);
            purchaseInboundMapper.updateById(header);
        }
        return CodeDataResponse.ok();
    }

    @GetMapping("/balances")
    public CodeDataResponse<List<InventoryBalanceRow>> listBalances(@RequestParam(required = false) String warehouse,
                                                                    @RequestParam(required = false) String itemName,
                                                                    @RequestParam(required = false) String orgId) {
        InventoryScope scope = resolveInventoryScope(orgId);
        String warehouseValue = trimNullable(warehouse);
        String itemKeyword = toLower(trimNullable(itemName));
        List<InventoryBalanceRow> rows = inventoryBalanceMapper.selectList(new LambdaQueryWrapper<InventoryBalanceDO>()
                        .eq(InventoryBalanceDO::getScopeType, scope.scopeType())
                        .eq(InventoryBalanceDO::getScopeId, scope.scopeId())
                        .orderByAsc(InventoryBalanceDO::getWarehouseName)
                        .orderByAsc(InventoryBalanceDO::getItemCode))
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
        return CodeDataResponse.ok(rows);
    }

    private List<PurchaseInboundDO> requireHeaders(InventoryScope scope, List<Long> ids) {
        Set<Long> idSet = new LinkedHashSet<>(ids);
        if (idSet.isEmpty()) {
            throw new BusinessException("单据ID不能为空");
        }
        List<PurchaseInboundDO> headers = purchaseInboundMapper.selectList(scopeQuery(scope)
                .in(PurchaseInboundDO::getId, idSet));
        if (headers.size() != idSet.size()) {
            throw new BusinessException("存在无效单据");
        }
        return headers;
    }

    private Map<Long, List<PurchaseInboundLineDO>> loadLineMap(List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Map.of();
        }
        return purchaseInboundLineMapper.selectList(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                        .in(PurchaseInboundLineDO::getInboundId, headerIds))
                .stream()
                .collect(Collectors.groupingBy(PurchaseInboundLineDO::getInboundId, LinkedHashMap::new, Collectors.toList()));
    }

    private void applyInventoryDelta(InventoryScope scope,
                                     PurchaseInboundDO header,
                                     PurchaseInboundLineDO line,
                                     BigDecimal delta,
                                     String bizType,
                                     Long operatorId) {
        InventoryBalanceDO balance = inventoryBalanceMapper.selectOne(new LambdaQueryWrapper<InventoryBalanceDO>()
                .eq(InventoryBalanceDO::getScopeType, scope.scopeType())
                .eq(InventoryBalanceDO::getScopeId, scope.scopeId())
                .eq(InventoryBalanceDO::getWarehouseName, header.getWarehouseName())
                .eq(InventoryBalanceDO::getItemCode, line.getItemCode())
                .last("limit 1"));
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
            inventoryBalanceMapper.insert(balance);
        } else {
            balance.setItemName(line.getItemName());
            balance.setQuantity(after);
            inventoryBalanceMapper.updateById(balance);
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
        inventoryTransactionMapper.insert(transaction);
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
            Long count = purchaseInboundMapper.selectCount(scopeQuery(scope)
                    .eq(PurchaseInboundDO::getDocumentCode, code));
            if (count == null || count == 0L) {
                return code;
            }
        }
        throw new BusinessException("生成单据号失败，请重试");
    }

    private LambdaQueryWrapper<PurchaseInboundDO> scopeQuery(InventoryScope scope) {
        return new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scope.scopeType())
                .eq(PurchaseInboundDO::getScopeId, scope.scopeId());
    }

    private InventoryScope resolveInventoryScope(String orgId) {
        Long userId = currentUserId();
        Scope requested = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return new InventoryScope(requested.scopeType(), requested.scopeId());
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new InventoryScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new InventoryScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new InventoryScope(SCOPE_STORE, requested.scopeId());
            }
            throw new BusinessException("当前账号无该门店权限");
        }
        throw new BusinessException("机构参数非法");
    }

    private Scope parseScope(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return new Scope(SCOPE_PLATFORM, 0L);
        }
        if (orgId.startsWith("group-")) {
            return new Scope(SCOPE_GROUP, parseNumericId(orgId.substring("group-".length())));
        }
        if (orgId.startsWith("store-")) {
            return new Scope(SCOPE_STORE, parseNumericId(orgId.substring("store-".length())));
        }
        throw new BusinessException("机构参数非法");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private Long currentUserId() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        return AuthContextHolder.get().getUserId();
    }

    private String currentUserName() {
        if (AuthContextHolder.get() == null || !StringUtils.hasText(AuthContextHolder.get().getRealName())) {
            return "未知用户";
        }
        return AuthContextHolder.get().getRealName();
    }

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, SCOPE_PLATFORM)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }

    private boolean hasScope(Long userId, String scopeType, Long scopeId) {
        if (scopeId == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }

    private Long findGroupIdByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreDO store = storeMapper.selectById(storeId);
        if (store == null) {
            return null;
        }
        return store.getGroupId();
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

    public record IdPayload(Long id, String documentCode) {
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
                                     String remark) {
    }

    public record InventoryBalanceRow(String warehouse,
                                      String itemCode,
                                      String itemName,
                                      String quantity,
                                      String updatedAt) {
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record InventoryScope(String scopeType, Long scopeId) {
    }
}
