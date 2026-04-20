package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessStoreBindingRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessStoreBindingDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 流程绑定解析服务。
 */
@Service
public class WorkflowBindingResolverService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final WorkflowProcessRegistryRepository processRegistryRepository;
    private final WorkflowDefinitionConfigRepository definitionConfigRepository;
    private final WorkflowProcessStoreBindingRepository processStoreBindingRepository;

    public WorkflowBindingResolverService(WorkflowProcessRegistryRepository processRegistryRepository,
                                         WorkflowDefinitionConfigRepository definitionConfigRepository,
                                         WorkflowProcessStoreBindingRepository processStoreBindingRepository) {
        this.processRegistryRepository = processRegistryRepository;
        this.definitionConfigRepository = definitionConfigRepository;
        this.processStoreBindingRepository = processStoreBindingRepository;
    }

    /**
     * 解析已发布的流程绑定。
     *
     * @param scopeType     作用域类型
     * @param scopeId       作用域 ID
     * @param groupId       集团 ID
     * @param businessCode  业务编码
     * @param workflowLabel 流程提示文案
     * @return 流程绑定
     */
    public Optional<ResolvedWorkflowBinding> resolvePublishedBinding(String scopeType,
                                                                    Long scopeId,
                                                                    Long groupId,
                                                                    String businessCode,
                                                                    String workflowLabel) {
        if (!SCOPE_GROUP.equalsIgnoreCase(scopeType) && !SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return Optional.empty();
        }
        if (groupId == null || !StringUtils.hasText(businessCode)) {
            return Optional.empty();
        }
        WorkflowProcessRegistryDO registry = processRegistryRepository
                .findByScopeAndProcessCode(SCOPE_GROUP, groupId, businessCode)
                .orElse(null);
        if (registry == null) {
            return Optional.empty();
        }
        if (SCOPE_STORE.equalsIgnoreCase(scopeType) && scopeId != null) {
            List<WorkflowProcessStoreBindingDO> bindings = processStoreBindingRepository
                    .findByGroupAndProcessRegistryId(groupId, registry.getId());
            if (!bindings.isEmpty() && bindings.stream().noneMatch(item -> scopeId.equals(item.getStoreId()))) {
                return Optional.empty();
            }
        }

        String workflowCode = trimToNull(registry.getTemplateId());
        if (!StringUtils.hasText(workflowCode)) {
            throw new BusinessException(workflowLabel + "尚未绑定模板，请先在流程管理中绑定模板");
        }
        WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, registry.getProcessCode(), workflowCode);
        if (config == null || !STATUS_PUBLISHED.equals(config.getStatus())) {
            throw new BusinessException(workflowLabel + "模板未发布，请先发布流程");
        }
        if (!StringUtils.hasText(config.getProcessDefinitionKey()) || !StringUtils.hasText(config.getProcessDefinitionId())) {
            throw new BusinessException(workflowLabel + "定义缺失，请重新发布流程");
        }
        return Optional.of(new ResolvedWorkflowBinding(
                registry.getId(),
                registry.getProcessCode(),
                workflowCode,
                trimToNull(registry.getBusinessName()),
                trimToNull(config.getNodeConfigJson()),
                config.getProcessDefinitionKey(),
                config.getProcessDefinitionId()
        ));
    }

    private WorkflowDefinitionConfigDO findConfig(String scopeType,
                                                  Long scopeId,
                                                  Long groupId,
                                                  String businessCode,
                                                  String workflowCode) {
        WorkflowDefinitionConfigDO direct = selectConfig(scopeType, scopeId, businessCode, workflowCode);
        if (direct != null) {
            return direct;
        }
        if (SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return selectConfig(SCOPE_GROUP, groupId, businessCode, workflowCode);
        }
        return null;
    }

    private WorkflowDefinitionConfigDO selectConfig(String scopeType,
                                                    Long scopeId,
                                                    String businessCode,
                                                    String workflowCode) {
        if (!StringUtils.hasText(scopeType) || scopeId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(workflowCode)) {
            return null;
        }
        return definitionConfigRepository.findByScopeBusinessAndWorkflow(
                scopeType.toUpperCase(Locale.ROOT),
                scopeId,
                businessCode,
                workflowCode
        ).orElse(null);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ResolvedWorkflowBinding(Long processRegistryId,
                                          String processCode,
                                          String workflowCode,
                                          String businessName,
                                          String nodeConfigJson,
                                          String processDefinitionKey,
                                          String processDefinitionId) {
    }
}
