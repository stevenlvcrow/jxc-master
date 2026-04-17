package com.boboboom.jxc.workflow.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessRegistryMapper;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessStoreBindingMapper;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessStoreBindRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowProcessUpsertRequest;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowTemplateBindRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/workflow/processes")
public class WorkflowProcessController {
    private static final Logger log = LoggerFactory.getLogger(WorkflowProcessController.class);

    private static final String SCOPE_GROUP = "GROUP";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final WorkflowProcessRegistryMapper processRegistryMapper;
    private final WorkflowProcessStoreBindingMapper processStoreBindingMapper;
    private final StoreMapper storeMapper;
    private final OrgScopeService orgScopeService;

    public WorkflowProcessController(WorkflowProcessRegistryMapper processRegistryMapper,
                                     WorkflowProcessStoreBindingMapper processStoreBindingMapper,
                                     StoreMapper storeMapper,
                                     OrgScopeService orgScopeService) {
        this.processRegistryMapper = processRegistryMapper;
        this.processStoreBindingMapper = processStoreBindingMapper;
        this.storeMapper = storeMapper;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping
    public CodeDataResponse<List<WorkflowProcessView>> list(@RequestParam(required = false) String orgId) {
        Long groupId = resolveGroupScope(orgId);
        List<WorkflowProcessRegistryDO> processes = processRegistryMapper.selectList(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, SCOPE_GROUP)
                .eq(WorkflowProcessRegistryDO::getScopeId, groupId)
                .orderByDesc(WorkflowProcessRegistryDO::getCreatedAt)
                .orderByDesc(WorkflowProcessRegistryDO::getId));
        if (processes.isEmpty()) {
            return CodeDataResponse.ok(List.of());
        }

        List<Long> processIds = processes.stream().map(WorkflowProcessRegistryDO::getId).toList();
        List<WorkflowProcessStoreBindingDO> bindings = processStoreBindingMapper.selectList(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .in(WorkflowProcessStoreBindingDO::getProcessRegistryId, processIds)
                .orderByAsc(WorkflowProcessStoreBindingDO::getStoreId));

        Set<Long> storeIds = bindings.stream().map(WorkflowProcessStoreBindingDO::getStoreId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, StoreDO> storeMap = storeIds.isEmpty()
                ? Map.of()
                : storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .in(StoreDO::getId, storeIds))
                .stream()
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
                    .add((store.getStoreName() == null ? "" : store.getStoreName()) + "（" + store.getStoreCode() + "）");
        }

        List<WorkflowProcessView> rows = processes.stream()
                .map(item -> toView(
                        item,
                        processStoreIdsMap.getOrDefault(item.getId(), List.of()),
                        processStoreNamesMap.getOrDefault(item.getId(), List.of())))
                .toList();
        return CodeDataResponse.ok(rows);
    }

    @GetMapping("/stores")
    public CodeDataResponse<List<StoreOptionView>> listStores(@RequestParam(required = false) String orgId) {
        try {
            Long groupId = resolveGroupScope(orgId);
            List<StoreOptionView> rows = storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                            .eq(StoreDO::getGroupId, groupId)
                            .eq(StoreDO::getStatus, "ENABLED")
                            .orderByAsc(StoreDO::getId))
                    .stream()
                    .map(item -> new StoreOptionView(item.getId(), item.getStoreCode(), item.getStoreName()))
                    .toList();
            return CodeDataResponse.ok(rows);
        } catch (Exception ex) {
            log.error("查询流程门店列表失败, orgId={}", orgId, ex);
            throw ex;
        }
    }

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String processCode = normalizeCode(request.processCode(), "流程ID不能为空");
        String businessName = normalizeName(request.businessName(), "业务名称不能为空");
        ensureProcessCodeUnique(groupId, processCode, null);

        WorkflowProcessRegistryDO row = new WorkflowProcessRegistryDO();
        row.setScopeType(SCOPE_GROUP);
        row.setScopeId(groupId);
        row.setProcessCode(processCode);
        row.setBusinessName(businessName);
        row.setTemplateId(trimNullable(request.templateId()));
        row.setCreatedBy(operatorId);
        row.setUpdatedBy(operatorId);
        processRegistryMapper.insert(row);
        return CodeDataResponse.ok(new IdPayload(row.getId()));
    }

    @PutMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> update(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @Valid @RequestBody WorkflowProcessUpsertRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        String processCode = normalizeCode(request.processCode(), "流程ID不能为空");
        String businessName = normalizeName(request.businessName(), "业务名称不能为空");
        ensureProcessCodeUnique(groupId, processCode, id);

        row.setProcessCode(processCode);
        row.setBusinessName(businessName);
        row.setTemplateId(trimNullable(request.templateId()));
        row.setUpdatedBy(operatorId);
        processRegistryMapper.updateById(row);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/bind-template")
    @Transactional
    public CodeDataResponse<Void> bindTemplate(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId,
                                               @Valid @RequestBody WorkflowTemplateBindRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        row.setTemplateId(normalizeCode(request.templateId(), "模板ID不能为空"));
        row.setUpdatedBy(operatorId);
        processRegistryMapper.updateById(row);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/bind-stores")
    @Transactional
    public CodeDataResponse<Void> bindStores(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @Valid @RequestBody WorkflowProcessStoreBindRequest request) {
        Long groupId = resolveGroupScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        List<Long> storeIds = request.storeIds() == null ? List.of() : request.storeIds().stream()
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        validateStoreIds(groupId, storeIds);

        processStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, row.getId()));
        if (!storeIds.isEmpty()) {
            for (Long storeId : storeIds) {
                WorkflowProcessStoreBindingDO binding = new WorkflowProcessStoreBindingDO();
                binding.setGroupId(groupId);
                binding.setProcessRegistryId(row.getId());
                binding.setStoreId(storeId);
                binding.setCreatedBy(operatorId);
                binding.setUpdatedBy(operatorId);
                processStoreBindingMapper.insert(binding);
            }
        }
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> delete(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        Long groupId = resolveGroupScope(orgId);
        WorkflowProcessRegistryDO row = requireProcess(id, groupId);
        processStoreBindingMapper.delete(new LambdaQueryWrapper<WorkflowProcessStoreBindingDO>()
                .eq(WorkflowProcessStoreBindingDO::getGroupId, groupId)
                .eq(WorkflowProcessStoreBindingDO::getProcessRegistryId, row.getId()));
        processRegistryMapper.deleteById(row.getId());
        return CodeDataResponse.ok();
    }

    private void validateStoreIds(Long groupId, List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return;
        }
        List<Long> existingStoreIds = storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                        .select(StoreDO::getId)
                        .eq(StoreDO::getGroupId, groupId)
                        .eq(StoreDO::getStatus, "ENABLED")
                        .in(StoreDO::getId, storeIds))
                .stream()
                .map(StoreDO::getId)
                .toList();
        if (existingStoreIds.size() != storeIds.size()) {
            throw new BusinessException("门店选择无效，请刷新后重试");
        }
    }

    private WorkflowProcessRegistryDO requireProcess(Long id, Long groupId) {
        WorkflowProcessRegistryDO row = processRegistryMapper.selectOne(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getId, id)
                .eq(WorkflowProcessRegistryDO::getScopeType, SCOPE_GROUP)
                .eq(WorkflowProcessRegistryDO::getScopeId, groupId)
                .last("limit 1"));
        if (row == null) {
            throw new BusinessException("流程不存在");
        }
        return row;
    }

    private void ensureProcessCodeUnique(Long groupId, String processCode, Long selfId) {
        LambdaQueryWrapper<WorkflowProcessRegistryDO> query = new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, SCOPE_GROUP)
                .eq(WorkflowProcessRegistryDO::getScopeId, groupId)
                .eq(WorkflowProcessRegistryDO::getProcessCode, processCode);
        if (selfId != null) {
            query.ne(WorkflowProcessRegistryDO::getId, selfId);
        }
        WorkflowProcessRegistryDO exists = processRegistryMapper.selectOne(query.last("limit 1"));
        if (exists != null) {
            throw new BusinessException("流程ID已存在");
        }
    }

    private WorkflowProcessView toView(WorkflowProcessRegistryDO row,
                                       List<Long> storeIds,
                                       List<String> storeNames) {
        List<Long> safeStoreIds = storeIds == null ? List.of() : storeIds;
        List<String> safeStoreNames = storeNames == null ? List.of() : storeNames;
        return new WorkflowProcessView(
                row.getId(),
                row.getProcessCode(),
                row.getBusinessName(),
                row.getTemplateId(),
                Collections.unmodifiableList(new ArrayList<>(safeStoreIds)),
                String.join("、", safeStoreNames),
                formatDateTime(row.getCreatedAt())
        );
    }

    private Long resolveGroupScope(String orgId) {
        return orgScopeService.resolveGroupWorkflowScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
    }

    private String normalizeCode(String value, String message) {
        String normalized = requiredTrim(value, message).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_\\-]+")) {
            throw new BusinessException("编码仅支持字母、数字、下划线和中划线");
        }
        return normalized;
    }

    private String normalizeName(String value, String message) {
        return requiredTrim(value, message);
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATETIME_FORMATTER.format(value);
    }

    public record WorkflowProcessView(Long id,
                                      String processId,
                                      String businessName,
                                      String templateId,
                                      List<Long> storeIds,
                                      String storeNames,
                                      String createdAt) {
    }

    public record StoreOptionView(Long storeId,
                                  String storeCode,
                                  String storeName) {
    }

    public record IdPayload(Long id) {
    }
}
