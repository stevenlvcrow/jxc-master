package com.boboboom.jxc.item.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.domain.repository.SupplierContractRepository;
import com.boboboom.jxc.item.domain.repository.SupplierFinanceAccountRepository;
import com.boboboom.jxc.item.domain.repository.SupplierProfileRepository;
import com.boboboom.jxc.item.domain.repository.SupplierQualificationRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierFinanceAccountDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierQualificationDO;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SupplierApplicationService {

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
    private static final String SUPPLIER_CODE_PREFIX = "GYBM";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupplierProfileRepository supplierProfileRepository;
    private final SupplierQualificationRepository supplierQualificationRepository;
    private final SupplierContractRepository supplierContractRepository;
    private final SupplierFinanceAccountRepository supplierFinanceAccountRepository;
    private final OrgScopeService orgScopeService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public SupplierApplicationService(SupplierProfileRepository supplierProfileRepository,
                                      SupplierQualificationRepository supplierQualificationRepository,
                                      SupplierContractRepository supplierContractRepository,
                                      SupplierFinanceAccountRepository supplierFinanceAccountRepository,
                                      OrgScopeService orgScopeService,
                                      BusinessCodeGenerator businessCodeGenerator) {
        this.supplierProfileRepository = supplierProfileRepository;
        this.supplierQualificationRepository = supplierQualificationRepository;
        this.supplierContractRepository = supplierContractRepository;
        this.supplierFinanceAccountRepository = supplierFinanceAccountRepository;
        this.orgScopeService = orgScopeService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CodeDataResponse<PageData<SupplierListRow>> list(
            Integer pageNo,
            Integer pageSize,
            String supplierInfo,
            String status,
            String bindStatus,
            String source,
            String supplyRelation,
            String treeNode,
            String orgId
    ) {
        SupplierScope scope = resolveSupplierScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        List<SupplierProfileDO> records = filterSupplierProfiles(
                supplierProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()),
                supplierInfo,
                status,
                bindStatus,
                source,
                supplyRelation,
                treeNode,
                offset,
                safePageSize
        );
        Long total = (long) filterSupplierProfiles(
                supplierProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()),
                supplierInfo,
                status,
                bindStatus,
                source,
                supplyRelation,
                treeNode,
                0,
                Integer.MAX_VALUE
        ).size();

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

    @Transactional
    public CodeDataResponse<IdPayload> create(String orgId, SupplierCreateRequest request) {
        SupplierScope scope = resolveSupplierScope(orgId);
        String supplierCode = generateSupplierCode(scope);
        ensureSupplierCodeUnique(scope, supplierCode);

        SupplierProfileDO profile = new SupplierProfileDO();
        profile.setScopeType(scope.scopeType());
        profile.setScopeId(scope.scopeId());
        applyProfileFields(profile, request, scope.scopeType(), supplierCode);
        profile.setBindStatus(BIND_STATUS_UNBOUND);
        supplierProfileRepository.save(profile);
        replaceSupplierDetails(profile.getId(), request);
        return CodeDataResponse.ok(new IdPayload(profile.getId()));
    }

    public CodeDataResponse<SupplierDetailResponse> detail(Long id, String orgId) {
        SupplierScope scope = resolveSupplierScope(orgId);
        SupplierProfileDO profile = requireSupplierInScope(scope, id, "供应商不存在或无权限访问");
        return CodeDataResponse.ok(buildSupplierDetail(profile));
    }

    @Transactional
    public CodeDataResponse<IdPayload> update(Long id, String orgId, SupplierCreateRequest request) {
        SupplierScope scope = resolveSupplierScope(orgId);
        SupplierProfileDO profile = requireSupplierInScope(scope, id, "供应商不存在或无权限编辑");
        String supplierCode = trim(request.supplierCode());
        if (!Objects.equals(profile.getSupplierCode(), supplierCode)) {
            ensureSupplierCodeUnique(scope, supplierCode);
        }
        applyProfileFields(profile, request, scope.scopeType(), supplierCode);
        supplierProfileRepository.update(profile);
        replaceSupplierDetails(profile.getId(), request);
        return CodeDataResponse.ok(new IdPayload(profile.getId()));
    }

    private String generateSupplierCode(SupplierScope scope) {
        List<String> existingCodes = supplierProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(SupplierProfileDO::getSupplierCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(SUPPLIER_CODE_PREFIX, existingCodes);
    }

    private SupplierDetailResponse buildSupplierDetail(SupplierProfileDO profile) {
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
                loadQualificationItems(profile.getId()),
                loadContractItems(profile.getId()),
                loadFinanceItems(profile.getId()),
                trimNullable(profile.getInvoiceCompanyName()),
                trimNullable(profile.getTaxpayerId()),
                trimNullable(profile.getInvoicePhone()),
                trimNullable(profile.getInvoiceAddress())
        );
    }

    private List<SupplierDetailResponse.QualificationItem> loadQualificationItems(Long supplierId) {
        List<SupplierQualificationDO> qualificationRows = supplierQualificationRepository.findBySupplierIdOrdered(supplierId);
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
        return qualificationList;
    }

    private List<SupplierDetailResponse.ContractItem> loadContractItems(Long supplierId) {
        List<SupplierContractDO> contractRows = supplierContractRepository.findBySupplierIdOrdered(supplierId);
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
        return contractList;
    }

    private List<SupplierDetailResponse.FinanceItem> loadFinanceItems(Long supplierId) {
        List<SupplierFinanceAccountDO> financeRows = supplierFinanceAccountRepository.findBySupplierIdOrdered(supplierId);
        List<SupplierDetailResponse.FinanceItem> financeList = new ArrayList<>(financeRows.size());
        for (SupplierFinanceAccountDO row : financeRows) {
            financeList.add(new SupplierDetailResponse.FinanceItem(
                    trimNullable(row.getBankAccount()),
                    trimNullable(row.getAccountName()),
                    trimNullable(row.getBankName()),
                    Boolean.TRUE.equals(row.getDefaultAccount())
            ));
        }
        return financeList;
    }

    private SupplierProfileDO requireSupplierInScope(SupplierScope scope, Long id, String missingMessage) {
        SupplierProfileDO profile = supplierProfileRepository.findById(id)
                .filter(row -> Objects.equals(row.getScopeType(), scope.scopeType()))
                .filter(row -> Objects.equals(row.getScopeId(), scope.scopeId()))
                .orElse(null);
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
        supplierQualificationRepository.deleteBySupplierId(supplierId);
        supplierContractRepository.deleteBySupplierId(supplierId);
        supplierFinanceAccountRepository.deleteBySupplierId(supplierId);

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
            supplierQualificationRepository.save(row);
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
            supplierContractRepository.save(row);
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
            supplierFinanceAccountRepository.save(row);
        }
    }

    private void ensureSupplierCodeUnique(SupplierScope scope, String supplierCode) {
        boolean existing = supplierProfileRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getSupplierCode(), supplierCode));
        if (existing) {
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
        if (Objects.equals(scopeType, OrgScopeService.SCOPE_STORE)) {
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

    private List<SupplierProfileDO> filterSupplierProfiles(List<SupplierProfileDO> rows,
                                                           String supplierInfo,
                                                           String status,
                                                           String bindStatus,
                                                           String source,
                                                           String supplyRelation,
                                                           String treeNode,
                                                           int offset,
                                                           int limit) {
        String infoKeyword = StringUtils.hasText(supplierInfo) ? trim(supplierInfo) : null;
        String queryStatus = normalizeQueryStatus(status);
        String queryBindStatus = normalizeQueryBindStatus(bindStatus);
        String querySource = normalizeQuerySource(source);
        String querySupplyRelation = normalizeQuerySupplyRelation(supplyRelation);
        String treeNodeValue = trimNullable(treeNode);
        return rows.stream()
                .filter(row -> infoKeyword == null
                        || contains(row.getSupplierCode(), infoKeyword)
                        || contains(row.getSupplierName(), infoKeyword)
                        || contains(row.getSupplierShortName(), infoKeyword)
                        || contains(row.getSupplierMnemonic(), infoKeyword)
                        || contains(row.getSupplierCategory(), infoKeyword))
                .filter(row -> queryStatus == null || Objects.equals(row.getStatus(), queryStatus))
                .filter(row -> queryBindStatus == null || Objects.equals(row.getBindStatus(), queryBindStatus))
                .filter(row -> querySource == null || Objects.equals(row.getSource(), querySource))
                .filter(row -> querySupplyRelation == null || Objects.equals(row.getSupplyRelation(), querySupplyRelation))
                .filter(row -> treeNodeValue == null
                        || Objects.equals(treeNodeValue, "all")
                        || Objects.equals(treeNodeValue, "全部")
                        || Objects.equals(treeNodeValue, "供应商类别")
                        || Objects.equals(treeNodeValue, "全部供应商")
                        || Objects.equals(row.getSupplierCategory(), treeNodeValue))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private SupplierScope resolveSupplierScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new SupplierScope(scope.scopeType(), scope.scopeId());
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

    private record SupplierScope(String scopeType, Long scopeId) {
    }
}
