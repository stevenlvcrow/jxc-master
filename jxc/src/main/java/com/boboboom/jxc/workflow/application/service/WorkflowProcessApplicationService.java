package com.boboboom.jxc.workflow.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessBatchStoreBindRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessStoreBindRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessUpsertRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowTemplateBindRequest;

/** 流程管理业务服务，负责流程定义、模板绑定和门店绑定维护。 */
@Service
public class WorkflowProcessApplicationService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final Set<String> PROTECTED_PROCESS_CODES = InventoryDocumentType.workflowTypes().stream()
            .map(InventoryDocumentType::getBusinessCode)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    private static final Set<String> BUILT_IN_EXTRA_PROCESS_CODES = Stream.of(
            "PURCHASE_APPLICATION",
            "PURCHASE_ORDER",
            "PURCHASE_RECEIPT",
            "PURCHASE_RETURN",
            "PERIOD_OPENING_BALANCE",
            "INVENTORY_CHECK",
            "MULTI_INVENTORY_CHECK"
    ).collect(Collectors.toUnmodifiableSet());
    private static final Set<String> ALL_PROTECTED_PROCESS_CODES = Stream.concat(
            PROTECTED_PROCESS_CODES.stream(),
            BUILT_IN_EXTRA_PROCESS_CODES.stream()
    )
            .collect(Collectors.toUnmodifiableSet());
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final WorkflowProcessRegistryRepository processRegistryRepository;
    private final WorkflowProcessStoreBindingRepository processStoreBindingRepository;
    private final WorkflowDefinitionConfigRepository configRepository;
    private final StoreRepository storeRepository;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;
    private final InventoryWorkflowBootstrapService inventoryWorkflowBootstrapService;

    /** 流程管理业务服务，负责流程定义、模板绑定和门店绑定维护。 */
    public WorkflowProcessApplicationService(WorkflowProcessRegistryRepository processRegistryRepositoryValue,
                                             WorkflowProcessStoreBindingRepository processStoreBindingRepositoryValue,
                                             WorkflowDefinitionConfigRepository configRepositoryValue,
                                             StoreRepository storeRepositoryValue,
                                             OrgScopeService orgScopeServiceValue,
                                             DictionaryLookupService dictionaryLookupServiceValue,
                                             InventoryWorkflowBootstrapService inventoryWorkflowBootstrapServiceValue) {
        this.processRegistryRepository = processRegistryRepositoryValue;
        this.processStoreBindingRepository = processStoreBindingRepositoryValue;
        this.configRepository = configRepositoryValue;
        this.storeRepository = storeRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
        this.inventoryWorkflowBootstrapService = inventoryWorkflowBootstrapServiceValue;
    }

    /** 分页查询业务列表。 */
    public List<WorkflowProcessView> list(String orgId) {
        Long groupId = resolveGroupScope(orgId);
        List<WorkflowProcessRegistryDO> processes = processRegistryRepository.findByScopeOrdered(SCOPE_GROUP, groupId);
        if (processes.isEmpty()) {
            return List.of();
        }

        List<Long> processIds = processes.stream().map(WorkflowProcessRegistryDO::getId).toList();
        List<WorkflowProcessStoreBindingDO> bindings = processStoreBindingRepository.findByGroupAndProcessRegistryIds(groupId, processIds);

        Set<Long> storeIds = bindings.stream().map(WorkflowProcessStoreBindingDO::getStoreId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, StoreDO> storeMap = storeIds.isEmpty()
                ? Map.of()
                : storeRepository.findByGroupId(groupId).stream()
                .filter(store -> storeIds.contains(store.getId()))
                .collect(Collectors.toMap(StoreDO::getId, item -> item));

        Map<Long, List<Long>> processStoreIdsMap = new HashMap<>();
        Map<Long, List<String>> processStoreNamesMap = new HashMap<>();
        for (WorkflowProcessStoreBindingDO binding : bindings) {
            processStoreIdsMap.computeIfAbsent(binding.getProcessRegistryId(), key -> new ArrayList<>()).add(binding.getStoreId());
            StoreDO store = storeMap.get(binding.getStoreId());
            if (store == null) {
                continue;
            }
            processStoreNamesMap.computeIfAbsent(binding.getProcessRegistryId(), key -> new ArrayList<>())
                    .add(store.getStoreName() == null ? "" : store.getStoreName());
        }

        return processes.stream()
                .map(item -> toView(
                        item,
                        processStoreIdsMap.getOrDefault(item.getId(), List.of()),
                        processStoreNamesMap.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    /** 查询可绑定门店列表。 */
    public List<StoreOptionView> listStores(String orgId) {
        Long groupId = resolveGroupScope(orgId);
        return storeRepository.findByGroupId(groupId).stream()
                .filter(item -> enabledStatus().equals(item.getStatus()))
                .sorted(Comparator.comparing(StoreDO::getId))
                .map(item -> new StoreOptionView(item.getId(), item.getStoreCode(), item.getStoreName()))
                .toList();
    }

    /** 创建业务记录。 */
    @Transactional
    public IdPayload create(String orgId, WorkflowProcessUpsertRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String processCode = normalizeCode(request.process_code(), "业务编码不能为空");
        String businessName = normalizeName(request.businessName(), "业务名称不能为空");
        ensureProcessCodeUnique(groupId, processCode, null);

        WorkflowProcessRegistryDO row = new WorkflowProcessRegistryDO();
        row.setScopeType(SCOPE_GROUP);
        row.setScopeId(groupId);
        row.setProcessCode(processCode);
        row.setBusinessName(businessName);
        row.setTemplateId(InventoryWorkflowBootstrapService.DEFAULT_WORKFLOW_CODE);
        row.setCreatedBy(operatorId);
        row.setUpdatedBy(operatorId);
        processRegistryRepository.save(row);
        inventoryWorkflowBootstrapService.ensureDefaultWorkflowConfig(groupId, operatorId, row, businessName);
        ensureTemplatePublished(groupId, processCode, row.getTemplateId());
        return new IdPayload(row.getId());
    }

    /** 更新业务记录。 */
    @Transactional
    public void update(Long id, String orgId, WorkflowProcessUpsertRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        String processCode = normalizeCode(request.process_code(), "业务编码不能为空");
        String businessName = normalizeName(request.businessName(), "业务名称不能为空");
        String templateId = trimNullable(request.templateId());
        ensureTemplatePublished(groupId, processCode, templateId);
        ensureProcessCodeUnique(groupId, processCode, id);

        row.setProcessCode(processCode);
        row.setBusinessName(businessName);
        row.setTemplateId(templateId);
        row.setUpdatedBy(operatorId);
        processRegistryRepository.update(row);
    }

    /** 绑定流程模板。 */
    @Transactional
    public void bindTemplate(Long id, String orgId, WorkflowTemplateBindRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        String templateId = normalizeCode(request.templateId(), "模板ID不能为空");
        ensureTemplatePublished(groupId, row.getProcessCode(), templateId);
        row.setTemplateId(templateId);
        row.setUpdatedBy(operatorId);
        processRegistryRepository.update(row);
    }

    /** 绑定流程适用门店。 */
    @Transactional
    public void bindStores(Long id, String orgId, WorkflowProcessStoreBindRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        List<Long> storeIds = request.storeIds() == null ? List.of() : request.storeIds().stream()
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        validateStoreIds(groupId, storeIds);
        applyStoreBindings(groupId, operatorId, row, storeIds);
    }

    /** 批量绑定流程适用门店。 */
    @Transactional
    public void batchBindStores(String orgId, WorkflowProcessBatchStoreBindRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        List<Long> processIds = request.processIds() == null ? List.of() : request.processIds().stream()
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        if (processIds.isEmpty()) {
            throw new BusinessException("请选择要绑定门店的业务");
        }
        List<Long> storeIds = request.storeIds() == null ? List.of() : request.storeIds().stream()
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        validateStoreIds(groupId, storeIds);
        for (Long processId : processIds) {
            applyStoreBindings(groupId, operatorId, requireProcess(processId, groupId), storeIds);
        }
    }

    private void applyStoreBindings(Long groupId,
                                    Long operatorId,
                                    WorkflowProcessRegistryDO row,
                                    List<Long> storeIds) {
        List<WorkflowProcessStoreBindingDO> existingBindings = processStoreBindingRepository
                .findByGroupAndProcessRegistryId(groupId, row.getId());
        Set<Long> existingStoreIds = existingBindings.stream()
                .map(WorkflowProcessStoreBindingDO::getStoreId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> targetStoreIds = new LinkedHashSet<>(storeIds);
        for (Long storeId : existingStoreIds) {
            if (targetStoreIds.contains(storeId)) {
                continue;
            }
            processStoreBindingRepository.deleteByGroupAndProcessRegistryIdAndStoreId(groupId, row.getId(), storeId);
        }
        for (Long storeId : targetStoreIds) {
            if (existingStoreIds.contains(storeId)) {
                continue;
            }
            WorkflowProcessStoreBindingDO binding = new WorkflowProcessStoreBindingDO();
            binding.setGroupId(groupId);
            binding.setProcessRegistryId(row.getId());
            binding.setStoreId(storeId);
            binding.setCreatedBy(operatorId);
            binding.setUpdatedBy(operatorId);
            processStoreBindingRepository.save(binding);
        }
    }

    /** 删除业务记录。 */
    @Transactional
    public void delete(Long id, String orgId) {
        throw new BusinessException("流程业务不允许删除");
    }

    private void validateStoreIds(Long groupId, List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return;
        }
        Set<Long> existingStoreIds = storeRepository.findByGroupId(groupId).stream()
                .filter(store -> enabledStatus().equals(store.getStatus()))
                .map(StoreDO::getId)
                .filter(storeIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (existingStoreIds.size() != storeIds.size()) {
            throw new BusinessException("存在无效的门店ID");
        }
    }

    private WorkflowProcessRegistryDO requireProcess(Long id, Long groupId) {
        WorkflowProcessRegistryDO row = processRegistryRepository.findById(id).orElse(null);
        if (row == null || !SCOPE_GROUP.equals(row.getScopeType()) || !groupId.equals(row.getScopeId())) {
            throw new BusinessException("业务不存在");
        }
        return row;
    }

    private void ensureProcessCodeUnique(Long groupId, String processCode, Long currentId) {
        Optional<WorkflowProcessRegistryDO> exists = processRegistryRepository.findByScopeAndProcessCode(SCOPE_GROUP, groupId, processCode);
        if (exists.isPresent() && (currentId == null || !currentId.equals(exists.get().getId()))) {
            throw new BusinessException("业务编码已存在");
        }
    }

    private void ensureTemplatePublished(Long groupId, String processCode, String templateId) {
        if (!StringUtils.hasText(templateId)) {
            return;
        }
        WorkflowDefinitionConfigDO config = configRepository.findByScopeBusinessAndWorkflow(SCOPE_GROUP, groupId, processCode, templateId).orElse(null);
        if (config == null) {
            throw new BusinessException("流程模板不存在");
        }
        if (!publishedStatus().equals(config.getStatus())) {
            throw new BusinessException("未发布的流程模板不能使用，请先发布流程");
        }
    }

    private WorkflowProcessView toView(WorkflowProcessRegistryDO item, List<Long> storeIds, List<String> storeNames) {
        return new WorkflowProcessView(
                item.getId(),
                item.getProcessCode(),
                item.getBusinessName(),
                item.getTemplateId(),
                "",
                "",
                formatDateTime(item.getCreatedAt()),
                formatDateTime(item.getUpdatedAt()),
                storeIds,
                storeNames
        );
    }

    private Long resolveGroupScope(String orgId) {
        return orgScopeService.resolveGroupWorkflowScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
    }

    private String normalizeCode(String value, String message) {
        String normalized = requiredTrim(value, message).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_\\-]+")) {
            throw new BusinessException("缂傚倹鐗滈悥婊勭閸涱喗鏆滈柟闀愮閻⊙冃掑鍐ｅ亾娴ｈ娈堕悗娑欍仠閳ь兛妞掔粭鍛村礆閹烘柨娈犻柛婊冨閼垫垿宕氶幒鏂挎疇");
        }
        return normalized;
    }

    private String normalizeName(String value, String message) {
        return requiredTrim(value, message);
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

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATETIME_FORMATTER.format(value);
    }

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }

    private String publishedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.WORKFLOW_DEFINITION_STATUS, DictionaryCodes.PUBLISHED);
    }

    /** 审批流程视图模型，承载页面展示数据。 */
    public record WorkflowProcessView(Long id,
                                      String process_code,
                                      String businessName,
                                      String templateId,
                                      String templateWorkflowName,
                                      String status,
                                      String createdAt,
                                      String updatedAt,
                                      List<Long> storeIds,
                                      List<String> storeNames) {
    }

    /** 审批流程视图模型，承载页面展示数据。 */
    public record StoreOptionView(Long storeId,
                                  String storeCode,
                                  String storeName) {
    }

    /** 审批流程载荷模型，承载接口返回的关键标识。 */
    public record IdPayload(Long id) {
    }
}


