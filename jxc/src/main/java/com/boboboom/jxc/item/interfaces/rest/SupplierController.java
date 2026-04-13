package com.boboboom.jxc.item.interfaces.rest;

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
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierFinanceAccountDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierQualificationDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierContractMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierFinanceAccountMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierProfileMapper;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierQualificationMapper;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCreateRequest;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Validated
@RestController
@RequestMapping("/api/items/suppliers")
public class SupplierController {

    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "停用";
    private static final String BIND_STATUS_BOUND = "已绑定";
    private static final String BIND_STATUS_UNBOUND = "未绑定";
    private static final String SOURCE_GROUP = "集团";
    private static final String SOURCE_STORE = "门店";
    private static final String SUPPLY_RELATION_YES = "有";
    private static final String SUPPLY_RELATION_NO = "无";
    private static final String SCOPE_CONTROL_ON = "开启";
    private static final String SCOPE_CONTROL_OFF = "关闭";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupplierProfileMapper supplierProfileMapper;
    private final SupplierQualificationMapper supplierQualificationMapper;
    private final SupplierContractMapper supplierContractMapper;
    private final SupplierFinanceAccountMapper supplierFinanceAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public SupplierController(SupplierProfileMapper supplierProfileMapper,
                              SupplierQualificationMapper supplierQualificationMapper,
                              SupplierContractMapper supplierContractMapper,
                              SupplierFinanceAccountMapper supplierFinanceAccountMapper,
                              RoleMapper roleMapper,
                              UserRoleRelMapper userRoleRelMapper,
                              StoreMapper storeMapper) {
        this.supplierProfileMapper = supplierProfileMapper;
        this.supplierQualificationMapper = supplierQualificationMapper;
        this.supplierContractMapper = supplierContractMapper;
        this.supplierFinanceAccountMapper = supplierFinanceAccountMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public CodeDataResponse<PageData<SupplierListRow>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String supplierInfo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bindStatus,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String supplyRelation,
            @RequestParam(required = false) String treeNode,
            @RequestParam(required = false) String orgId
    ) {
        SupplierScope scope = resolveSupplierScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<SupplierProfileDO> query = buildListQuery(scope, supplierInfo, status, bindStatus, source, supplyRelation, treeNode);
        Long total = supplierProfileMapper.selectCount(query);
        List<SupplierProfileDO> records = supplierProfileMapper.selectList(
                buildListQuery(scope, supplierInfo, status, bindStatus, source, supplyRelation, treeNode)
                        .orderByDesc(SupplierProfileDO::getUpdatedAt)
                        .orderByDesc(SupplierProfileDO::getId)
                        .last("limit " + safePageSize + " offset " + offset)
        );

        List<SupplierListRow> rows = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            SupplierProfileDO row = records.get(i);
            rows.add(new SupplierListRow(
                    row.getId(),
                    offset + i + 1,
                    row.getSupplierCode(),
                    row.getSupplierName(),
                    row.getSupplierCategory(),
                    trimNullable(row.getContactPerson()),
                    trimNullable(row.getContactPhone()),
                    trimNullable(row.getSettlementMethod()),
                    trimNullable(row.getSource()),
                    trimNullable(row.getSupplyRelation()),
                    trimNullable(row.getRemark()),
                    row.getStatus(),
                    formatDateTime(row.getUpdatedAt())
            ));
        }
        return CodeDataResponse.ok(new PageData<>(rows, total == null ? 0 : total, safePageNo, safePageSize));
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        SupplierScope scope = resolveSupplierScope(orgId);
        String supplierCode = trim(request.supplierCode());
        ensureSupplierCodeUnique(scope, supplierCode);

        SupplierProfileDO profile = new SupplierProfileDO();
        profile.setScopeType(scope.scopeType());
        profile.setScopeId(scope.scopeId());
        applyProfileFields(profile, request, scope.scopeType(), supplierCode);
        profile.setBindStatus(BIND_STATUS_UNBOUND);
        supplierProfileMapper.insert(profile);
        replaceSupplierDetails(profile.getId(), request);
        return CodeDataResponse.ok(new IdPayload(profile.getId()));
    }

    @GetMapping("/{id}")
    public CodeDataResponse<SupplierDetailResponse> detail(@PathVariable Long id,
                                                           @RequestParam(required = false) String orgId) {
        SupplierScope scope = resolveSupplierScope(orgId);
        SupplierProfileDO profile = requireSupplierInScope(scope, id, "供应商不存在或无权限访问");
        return CodeDataResponse.ok(buildSupplierDetail(profile));
    }

    @PutMapping("/{id}")
    @Transactional
    public CodeDataResponse<IdPayload> update(@PathVariable Long id,
                                              @RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCreateRequest request) {
        SupplierScope scope = resolveSupplierScope(orgId);
        SupplierProfileDO profile = requireSupplierInScope(scope, id, "供应商不存在或无权限编辑");
        String supplierCode = trim(request.supplierCode());
        if (!Objects.equals(profile.getSupplierCode(), supplierCode)) {
            ensureSupplierCodeUnique(scope, supplierCode);
        }
        applyProfileFields(profile, request, scope.scopeType(), supplierCode);
        supplierProfileMapper.updateById(profile);
        replaceSupplierDetails(profile.getId(), request);
        return CodeDataResponse.ok(new IdPayload(profile.getId()));
    }

    private SupplierDetailResponse buildSupplierDetail(SupplierProfileDO profile) {
        List<SupplierQualificationDO> qualificationRows = supplierQualificationMapper.selectList(
                new LambdaQueryWrapper<SupplierQualificationDO>()
                        .eq(SupplierQualificationDO::getSupplierId, profile.getId())
                        .orderByAsc(SupplierQualificationDO::getSortNo)
                        .orderByAsc(SupplierQualificationDO::getId)
        );
        List<SupplierContractDO> contractRows = supplierContractMapper.selectList(
                new LambdaQueryWrapper<SupplierContractDO>()
                        .eq(SupplierContractDO::getSupplierId, profile.getId())
                        .orderByAsc(SupplierContractDO::getSortNo)
                        .orderByAsc(SupplierContractDO::getId)
        );
        List<SupplierFinanceAccountDO> financeRows = supplierFinanceAccountMapper.selectList(
                new LambdaQueryWrapper<SupplierFinanceAccountDO>()
                        .eq(SupplierFinanceAccountDO::getSupplierId, profile.getId())
                        .orderByAsc(SupplierFinanceAccountDO::getSortNo)
                        .orderByAsc(SupplierFinanceAccountDO::getId)
        );
        List<SupplierDetailResponse.QualificationItem> qualificationList = new ArrayList<>(qualificationRows.size());
        for (SupplierQualificationDO row : qualificationRows) {
            qualificationList.add(new SupplierDetailResponse.QualificationItem(
                    trimNullable(row.getFileName()),
                    trimNullable(row.getFileUrl()),
                    trimNullable(row.getQualificationType()),
                    formatDate(row.getValidTo()),
                    trimNullable(row.getStatus()),
                    trimNullable(row.getRemark())
            ));
        }
        List<SupplierDetailResponse.ContractItem> contractList = new ArrayList<>(contractRows.size());
        for (SupplierContractDO row : contractRows) {
            contractList.add(new SupplierDetailResponse.ContractItem(
                    trimNullable(row.getAttachmentName()),
                    trimNullable(row.getAttachmentUrl()),
                    trimNullable(row.getContractName()),
                    trimNullable(row.getContractCode()),
                    formatDate(row.getValidTo()),
                    trimNullable(row.getStatus())
            ));
        }
        List<SupplierDetailResponse.FinanceItem> financeList = new ArrayList<>(financeRows.size());
        for (SupplierFinanceAccountDO row : financeRows) {
            financeList.add(new SupplierDetailResponse.FinanceItem(
                    trimNullable(row.getBankAccount()),
                    trimNullable(row.getAccountName()),
                    trimNullable(row.getBankName()),
                    Boolean.TRUE.equals(row.getDefaultAccount())
            ));
        }
        return new SupplierDetailResponse(
                profile.getId(),
                trimNullable(profile.getSupplierCode()),
                trimNullable(profile.getSupplierName()),
                trimNullable(profile.getSupplierShortName()),
                trimNullable(profile.getSupplierMnemonic()),
                trimNullable(profile.getSupplierCategory()),
                profile.getTaxRate(),
                trimNullable(profile.getStatus()),
                trimNullable(profile.getContactPerson()),
                trimNullable(profile.getContactPhone()),
                trimNullable(profile.getEmail()),
                trimNullable(profile.getContactAddress()),
                trimNullable(profile.getRemark()),
                trimNullable(profile.getSettlementMethod()),
                trimNullable(profile.getOrderSummaryRule()),
                Boolean.TRUE.equals(profile.getInputBatchWhenDelivery()),
                Boolean.TRUE.equals(profile.getSyncReceiptData()),
                trimNullable(profile.getPurchaseReceiptDependShipping()),
                trimNullable(profile.getDeliveryDependShipping()),
                Boolean.TRUE.equals(profile.getSupplierManageInventory()),
                Boolean.TRUE.equals(profile.getControlOrderTime()),
                Boolean.TRUE.equals(profile.getAllowCloseOrder()),
                trimNullable(profile.getReconciliationMode()),
                trimNullable(profile.getScopeControl()),
                qualificationList,
                contractList,
                financeList,
                trimNullable(profile.getInvoiceCompanyName()),
                trimNullable(profile.getTaxpayerId()),
                trimNullable(profile.getInvoicePhone()),
                trimNullable(profile.getInvoiceAddress())
        );
    }

    private SupplierProfileDO requireSupplierInScope(SupplierScope scope, Long id, String missingMessage) {
        SupplierProfileDO profile = supplierProfileMapper.selectOne(
                scopeQuery(scope)
                        .eq(SupplierProfileDO::getId, id)
                        .last("limit 1")
        );
        if (profile == null) {
            throw new BusinessException(missingMessage);
        }
        return profile;
    }

    private void applyProfileFields(SupplierProfileDO profile,
                                    SupplierCreateRequest request,
                                    String scopeType,
                                    String supplierCode) {
        profile.setSupplierCode(supplierCode);
        profile.setSupplierName(trim(request.supplierName()));
        profile.setSupplierShortName(trimNullable(request.supplierShortName()));
        profile.setSupplierMnemonic(trimNullable(request.supplierMnemonic()));
        profile.setSupplierCategory(trim(request.supplierCategory()));
        profile.setTaxRate(normalizeTaxRate(request.taxRate()));
        profile.setStatus(normalizeStatus(request.status()));
        profile.setContactPerson(trimNullable(request.contactPerson()));
        profile.setContactPhone(trimNullable(request.contactPhone()));
        profile.setEmail(trimNullable(request.email()));
        profile.setContactAddress(trimNullable(request.contactAddress()));
        profile.setRemark(trimNullable(request.remark()));
        profile.setSettlementMethod(normalizeSettlementMethod(request.settlementMethod()));
        profile.setOrderSummaryRule(normalizeOrderSummaryRule(request.orderSummaryRule()));
        profile.setInputBatchWhenDelivery(Boolean.TRUE.equals(request.inputBatchWhenDelivery()));
        profile.setSyncReceiptData(Boolean.TRUE.equals(request.syncReceiptData()));
        profile.setPurchaseReceiptDependShipping(normalizeDependency(request.purchaseReceiptDependShipping()));
        profile.setDeliveryDependShipping(normalizeDependency(request.deliveryDependShipping()));
        profile.setSupplierManageInventory(Boolean.TRUE.equals(request.supplierManageInventory()));
        profile.setControlOrderTime(Boolean.TRUE.equals(request.controlOrderTime()));
        profile.setAllowCloseOrder(Boolean.TRUE.equals(request.allowCloseOrder()));
        profile.setReconciliationMode(trim(request.reconciliationMode()));
        profile.setScopeControl(normalizeScopeControl(request.scopeControl()));
        profile.setSource(resolveSource(scopeType));
        profile.setSupplyRelation(resolveSupplyRelation(profile.getScopeControl()));
        profile.setInvoiceCompanyName(trimNullable(request.invoiceCompanyName()));
        profile.setTaxpayerId(trimNullable(request.taxpayerId()));
        profile.setInvoicePhone(trimNullable(request.invoicePhone()));
        profile.setInvoiceAddress(trimNullable(request.invoiceAddress()));
    }

    private void replaceSupplierDetails(Long supplierId, SupplierCreateRequest request) {
        supplierQualificationMapper.delete(new LambdaQueryWrapper<SupplierQualificationDO>()
                .eq(SupplierQualificationDO::getSupplierId, supplierId));
        supplierContractMapper.delete(new LambdaQueryWrapper<SupplierContractDO>()
                .eq(SupplierContractDO::getSupplierId, supplierId));
        supplierFinanceAccountMapper.delete(new LambdaQueryWrapper<SupplierFinanceAccountDO>()
                .eq(SupplierFinanceAccountDO::getSupplierId, supplierId));

        List<SupplierCreateRequest.QualificationItemRequest> qualificationItems = request.qualificationList() == null
                ? List.of()
                : request.qualificationList();
        for (int i = 0; i < qualificationItems.size(); i++) {
            SupplierCreateRequest.QualificationItemRequest item = qualificationItems.get(i);
            SupplierQualificationDO row = new SupplierQualificationDO();
            row.setSupplierId(supplierId);
            row.setSortNo(i + 1);
            row.setFileName(trimNullable(item.fileName()));
            row.setFileUrl(trimNullable(item.fileUrl()));
            row.setQualificationType(trim(item.qualificationType()));
            row.setValidTo(parseNullableDate(item.validTo(), "资质有效期"));
            row.setStatus(StringUtils.hasText(item.status()) ? trim(item.status()) : "临期");
            row.setRemark(trimNullable(item.remark()));
            supplierQualificationMapper.insert(row);
        }

        List<SupplierCreateRequest.ContractItemRequest> contractItems = request.contractList() == null
                ? List.of()
                : request.contractList();
        for (int i = 0; i < contractItems.size(); i++) {
            SupplierCreateRequest.ContractItemRequest item = contractItems.get(i);
            SupplierContractDO row = new SupplierContractDO();
            row.setSupplierId(supplierId);
            row.setSortNo(i + 1);
            row.setAttachmentName(trimNullable(item.attachmentName()));
            row.setAttachmentUrl(trimNullable(item.attachmentUrl()));
            row.setContractName(trimNullable(item.contractName()));
            row.setContractCode(trimNullable(item.contractCode()));
            row.setValidTo(parseNullableDate(item.validTo(), "合同有效期"));
            row.setStatus(trimNullable(item.status()));
            supplierContractMapper.insert(row);
        }

        List<SupplierCreateRequest.FinanceItemRequest> financeItems = request.financeList() == null
                ? List.of()
                : request.financeList();
        if (financeItems.isEmpty()) {
            throw new BusinessException("请至少维护一条财务信息");
        }
        for (int i = 0; i < financeItems.size(); i++) {
            SupplierCreateRequest.FinanceItemRequest item = financeItems.get(i);
            SupplierFinanceAccountDO row = new SupplierFinanceAccountDO();
            row.setSupplierId(supplierId);
            row.setSortNo(i + 1);
            row.setBankAccount(trim(item.bankAccount()));
            row.setAccountName(trim(item.accountName()));
            row.setBankName(trim(item.bankName()));
            row.setDefaultAccount(Boolean.TRUE.equals(item.defaultAccount()));
            supplierFinanceAccountMapper.insert(row);
        }
    }

    private LambdaQueryWrapper<SupplierProfileDO> buildListQuery(SupplierScope scope,
                                                                 String supplierInfo,
                                                                 String status,
                                                                 String bindStatus,
                                                                 String source,
                                                                 String supplyRelation,
                                                                 String treeNode) {
        LambdaQueryWrapper<SupplierProfileDO> wrapper = scopeQuery(scope);
        if (StringUtils.hasText(supplierInfo)) {
            String keyword = trim(supplierInfo);
            wrapper.and(q -> q.like(SupplierProfileDO::getSupplierCode, keyword)
                    .or()
                    .like(SupplierProfileDO::getSupplierName, keyword)
                    .or()
                    .like(SupplierProfileDO::getSupplierShortName, keyword)
                    .or()
                    .like(SupplierProfileDO::getSupplierMnemonic, keyword)
                    .or()
                    .like(SupplierProfileDO::getSupplierCategory, keyword));
        }

        String queryStatus = normalizeQueryStatus(status);
        if (queryStatus != null) {
            wrapper.eq(SupplierProfileDO::getStatus, queryStatus);
        }
        String queryBindStatus = normalizeQueryBindStatus(bindStatus);
        if (queryBindStatus != null) {
            wrapper.eq(SupplierProfileDO::getBindStatus, queryBindStatus);
        }
        String querySource = normalizeQuerySource(source);
        if (querySource != null) {
            wrapper.eq(SupplierProfileDO::getSource, querySource);
        }
        String querySupplyRelation = normalizeQuerySupplyRelation(supplyRelation);
        if (querySupplyRelation != null) {
            wrapper.eq(SupplierProfileDO::getSupplyRelation, querySupplyRelation);
        }
        if (StringUtils.hasText(treeNode)) {
            String node = trim(treeNode);
            if (!Objects.equals(node, "all")
                    && !Objects.equals(node, "全部")
                    && !Objects.equals(node, "供应商类别")
                    && !Objects.equals(node, "全部供应商")) {
                wrapper.eq(SupplierProfileDO::getSupplierCategory, node);
            }
        }
        return wrapper;
    }

    private void ensureSupplierCodeUnique(SupplierScope scope, String supplierCode) {
        SupplierProfileDO existing = supplierProfileMapper.selectOne(
                scopeQuery(scope)
                        .eq(SupplierProfileDO::getSupplierCode, supplierCode)
                        .last("limit 1")
        );
        if (existing != null) {
            throw new BusinessException("供货商编码已存在");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = trim(status);
        if (Objects.equals(normalized, STATUS_ENABLED) || Objects.equals(normalized, "ENABLED")) {
            return STATUS_ENABLED;
        }
        if (Objects.equals(normalized, STATUS_DISABLED) || Objects.equals(normalized, "DISABLED")) {
            return STATUS_DISABLED;
        }
        throw new BusinessException("启用状态仅支持 启用/停用");
    }

    private String normalizeQueryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = trim(status);
        if (Objects.equals(normalized, "全部") || Objects.equals(normalized, "ALL")) {
            return null;
        }
        return normalizeStatus(normalized);
    }

    private String normalizeQueryBindStatus(String bindStatus) {
        if (!StringUtils.hasText(bindStatus)) {
            return null;
        }
        String normalized = trim(bindStatus);
        if (Objects.equals(normalized, "全部") || Objects.equals(normalized, "ALL")) {
            return null;
        }
        if (!Objects.equals(normalized, BIND_STATUS_BOUND) && !Objects.equals(normalized, BIND_STATUS_UNBOUND)) {
            throw new BusinessException("绑定状态仅支持 全部/已绑定/未绑定");
        }
        return normalized;
    }

    private String normalizeQuerySource(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String normalized = trim(source);
        if (Objects.equals(normalized, "全部") || Objects.equals(normalized, "ALL")) {
            return null;
        }
        if (!Objects.equals(normalized, SOURCE_GROUP) && !Objects.equals(normalized, SOURCE_STORE)) {
            throw new BusinessException("来源仅支持 全部/集团/门店");
        }
        return normalized;
    }

    private String normalizeQuerySupplyRelation(String supplyRelation) {
        if (!StringUtils.hasText(supplyRelation)) {
            return null;
        }
        String normalized = trim(supplyRelation);
        if (Objects.equals(normalized, "全部") || Objects.equals(normalized, "ALL")) {
            return null;
        }
        if (!Objects.equals(normalized, SUPPLY_RELATION_YES) && !Objects.equals(normalized, SUPPLY_RELATION_NO)) {
            throw new BusinessException("供货关系仅支持 全部/有/无");
        }
        return normalized;
    }

    private String normalizeSettlementMethod(String value) {
        String normalized = trim(value);
        if (List.of("预付款", "货到付款", "日结", "月结").contains(normalized)) {
            return normalized;
        }
        throw new BusinessException("结算方式不合法");
    }

    private String normalizeOrderSummaryRule(String value) {
        String normalized = trim(value);
        if (Objects.equals(normalized, "按机构") || Objects.equals(normalized, "按仓库")) {
            return normalized;
        }
        throw new BusinessException("订单汇总规则不合法");
    }

    private String normalizeDependency(String value) {
        String normalized = trim(value);
        if (Objects.equals(normalized, "依赖") || Objects.equals(normalized, "不依赖")) {
            return normalized;
        }
        throw new BusinessException("依赖配置不合法");
    }

    private String normalizeScopeControl(String value) {
        String normalized = trim(value);
        if (Objects.equals(normalized, SCOPE_CONTROL_ON) || Objects.equals(normalized, SCOPE_CONTROL_OFF)) {
            return normalized;
        }
        throw new BusinessException("范围控制仅支持 开启/关闭");
    }

    private BigDecimal normalizeTaxRate(BigDecimal taxRate) {
        if (taxRate == null) {
            throw new BusinessException("税率不能为空");
        }
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("税率范围需在0-100之间");
        }
        return taxRate.stripTrailingZeros();
    }

    private String resolveSource(String scopeType) {
        if (Objects.equals(scopeType, SCOPE_STORE)) {
            return SOURCE_STORE;
        }
        return SOURCE_GROUP;
    }

    private String resolveSupplyRelation(String scopeControl) {
        if (Objects.equals(scopeControl, SCOPE_CONTROL_OFF)) {
            return SUPPLY_RELATION_NO;
        }
        return SUPPLY_RELATION_YES;
    }

    private LocalDate parseNullableDate(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(label + "格式不正确，需为yyyy-MM-dd");
        }
    }

    private LambdaQueryWrapper<SupplierProfileDO> scopeQuery(SupplierScope scope) {
        return new LambdaQueryWrapper<SupplierProfileDO>()
                .eq(SupplierProfileDO::getScopeType, scope.scopeType())
                .eq(SupplierProfileDO::getScopeId, scope.scopeId());
    }

    private SupplierScope resolveSupplierScope(String orgId) {
        Long userId = currentUserId();
        Scope requested = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return new SupplierScope(requested.scopeType(), requested.scopeId());
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new SupplierScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new SupplierScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new SupplierScope(SCOPE_STORE, requested.scopeId());
            }
            throw new BusinessException("当前账号无该门店权限");
        }
        throw new BusinessException("机构参数非法");
    }

    private Scope parseScope(String orgId) {
        if (orgId == null || orgId.isBlank()) {
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

    private String trim(String value) {
        if (value == null) {
            throw new BusinessException("参数不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    private String trimNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(value);
    }

    private String formatDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public record SupplierListRow(Long id,
                                  Integer index,
                                  String supplierCode,
                                  String supplierName,
                                  String supplierCategory,
                                  String contactPerson,
                                  String contactPhone,
                                  String settlementMethod,
                                  String source,
                                  String supplyRelation,
                                  String remark,
                                  String status,
                                  String operatedAt) {
    }

    public record PageData<T>(List<T> list, long total, int pageNo, int pageSize) {
    }

    public record IdPayload(Long id) {
    }

    public record SupplierDetailResponse(Long id,
                                         String supplierCode,
                                         String supplierName,
                                         String supplierShortName,
                                         String supplierMnemonic,
                                         String supplierCategory,
                                         BigDecimal taxRate,
                                         String status,
                                         String contactPerson,
                                         String contactPhone,
                                         String email,
                                         String contactAddress,
                                         String remark,
                                         String settlementMethod,
                                         String orderSummaryRule,
                                         Boolean inputBatchWhenDelivery,
                                         Boolean syncReceiptData,
                                         String purchaseReceiptDependShipping,
                                         String deliveryDependShipping,
                                         Boolean supplierManageInventory,
                                         Boolean controlOrderTime,
                                         Boolean allowCloseOrder,
                                         String reconciliationMode,
                                         String scopeControl,
                                         List<QualificationItem> qualificationList,
                                         List<ContractItem> contractList,
                                         List<FinanceItem> financeList,
                                         String invoiceCompanyName,
                                         String taxpayerId,
                                         String invoicePhone,
                                         String invoiceAddress) {
        public record QualificationItem(String fileName,
                                        String fileUrl,
                                        String qualificationType,
                                        String validTo,
                                        String status,
                                        String remark) {
        }

        public record ContractItem(String attachmentName,
                                   String attachmentUrl,
                                   String contractName,
                                   String contractCode,
                                   String validTo,
                                   String status) {
        }

        public record FinanceItem(String bankAccount,
                                  String accountName,
                                  String bankName,
                                  Boolean defaultAccount) {
        }
    }

    private record Scope(String scopeType, Long scopeId) {
    }

    private record SupplierScope(String scopeType, Long scopeId) {
    }
}
