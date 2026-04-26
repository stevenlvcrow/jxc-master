package com.boboboom.jxc.workflow.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 库存单据流程默认配置初始化服务。
 */
@Service
public class InventoryWorkflowBootstrapService {

    private static final String SCOPE_GROUP = "GROUP";
    public static final String DEFAULT_WORKFLOW_CODE = "BUILTIN_DEFAULT_APPROVAL";
    private static final String DEFAULT_WORKFLOW_NAME = "内置通用审批流程";
    private static final String DEFAULT_NODE_CONFIG_JSON = WorkflowBpmnModelSupport.defaultNodeConfigJson();
    private static final List<WorkflowProcessSeed> EXTRA_PROCESS_SEEDS = List.of(
            new WorkflowProcessSeed("PURCHASE_APPLICATION", "采购单申请流程"),
            new WorkflowProcessSeed("PURCHASE_ORDER", "采购订单流程"),
            new WorkflowProcessSeed("PURCHASE_RECEIPT", "采购收货单流程"),
            new WorkflowProcessSeed("PURCHASE_RETURN", "采购退货单流程"),
            new WorkflowProcessSeed("PERIOD_OPENING_BALANCE", "期初库存流程"),
            new WorkflowProcessSeed("INVENTORY_CHECK", "盘点单流程"),
            new WorkflowProcessSeed("MULTI_INVENTORY_CHECK", "多人盘点单流程")
    );

    private final WorkflowProcessRegistryRepository workflowProcessRegistryRepository;
    private final WorkflowDefinitionConfigRepository workflowDefinitionConfigRepository;
    private final RepositoryService repositoryService;
    private final DictionaryLookupService dictionaryLookupService;
    private final ObjectMapper objectMapper;

    /** 审批流程服务，负责相关业务规则和流程协作。 */
    public InventoryWorkflowBootstrapService(WorkflowProcessRegistryRepository workflowProcessRegistryRepositoryValue,
                                            WorkflowDefinitionConfigRepository workflowDefinitionConfigRepositoryValue,
                                            RepositoryService repositoryServiceValue,
                                            DictionaryLookupService dictionaryLookupServiceValue,
                                            ObjectMapper objectMapperValue) {
        this.workflowProcessRegistryRepository = workflowProcessRegistryRepositoryValue;
        this.workflowDefinitionConfigRepository = workflowDefinitionConfigRepositoryValue;
        this.repositoryService = repositoryServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
        this.objectMapper = objectMapperValue;
    }

    /**
     * 确保库存单据流程默认业务和配置已存在。
     *
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     */
    @Transactional
    public void ensureDefaults(Long groupId, Long operatorId) {
        if (groupId == null) {
            return;
        }
        for (WorkflowProcessSeed seed : EXTRA_PROCESS_SEEDS) {
            WorkflowProcessRegistryDO registry = ensureProcessRegistry(groupId, operatorId, seed.processCode(), seed.businessName());
            ensureDefaultWorkflowConfig(groupId, operatorId, registry, seed.businessName());
        }

        for (InventoryDocumentType type : InventoryDocumentType.workflowTypes()) {
            WorkflowProcessRegistryDO registry = ensureProcessRegistry(groupId, operatorId, type.getBusinessCode(), type.getBusinessName() + "流程");
            ensureDefaultWorkflowConfig(groupId, operatorId, registry, type.getBusinessName() + "流程");
        }
    }

    /** 审批流程配置类，注册框架组件和运行参数。 */
    @Transactional
    public void ensureDefaultWorkflowConfig(Long groupId,
                                            Long operatorId,
                                            WorkflowProcessRegistryDO registry,
                                            String workflowName) {
        if (isInvalidDefaultWorkflowRequest(groupId, registry)) {
            return;
        }
        WorkflowDefinitionConfigDO existing = workflowDefinitionConfigRepository.findByScopeBusinessAndWorkflow(
                SCOPE_GROUP,
                groupId,
                registry.getProcessCode(),
                DEFAULT_WORKFLOW_CODE
        ).orElse(null);
        if (existing == null) {
            WorkflowDefinitionConfigDO config = new WorkflowDefinitionConfigDO();
            config.setScopeType(SCOPE_GROUP);
            config.setScopeId(groupId);
            config.setBusinessCode(registry.getProcessCode());
            config.setWorkflowCode(DEFAULT_WORKFLOW_CODE);
            config.setWorkflowName(StringUtils.hasText(workflowName) ? workflowName : DEFAULT_WORKFLOW_NAME);
            config.setNodeConfigJson(DEFAULT_NODE_CONFIG_JSON);
            config.setStatus(draftStatus());
            config.setVersionNo(0);
            config.setCreatedBy(operatorId);
            config.setUpdatedBy(operatorId);
            workflowDefinitionConfigRepository.save(config);
            publishDefaultConfig(config, operatorId);
        } else if (!publishedStatus().equals(existing.getStatus())
                || !StringUtils.hasText(existing.getProcessDefinitionKey())
                || !StringUtils.hasText(existing.getProcessDefinitionId())
                || requiresDefaultWorkflowRefresh(existing.getNodeConfigJson())) {
            existing.setWorkflowName(StringUtils.hasText(workflowName) ? workflowName : DEFAULT_WORKFLOW_NAME);
            existing.setNodeConfigJson(DEFAULT_NODE_CONFIG_JSON);
            existing.setUpdatedBy(operatorId);
            publishDefaultConfig(existing, operatorId);
        }
        if (!DEFAULT_WORKFLOW_CODE.equals(trimToNull(registry.getTemplateId()))) {
            registry.setTemplateId(DEFAULT_WORKFLOW_CODE);
            registry.setUpdatedBy(operatorId);
            workflowProcessRegistryRepository.update(registry);
        }
    }

    private boolean isInvalidDefaultWorkflowRequest(Long groupId, WorkflowProcessRegistryDO registry) {
        return groupId == null || registry == null || !StringUtils.hasText(registry.getProcessCode());
    }

    private WorkflowProcessRegistryDO ensureProcessRegistry(Long groupId,
                                                            Long operatorId,
                                                            String processCode,
                                                            String businessName) {
        WorkflowProcessRegistryDO registry = workflowProcessRegistryRepository
                .findByScopeAndProcessCode(SCOPE_GROUP, groupId, processCode)
                .orElse(null);
        if (registry != null) {
            if (!StringUtils.hasText(registry.getBusinessName()) && StringUtils.hasText(businessName)) {
                registry.setBusinessName(businessName);
                registry.setUpdatedBy(operatorId);
                workflowProcessRegistryRepository.update(registry);
            }
            return registry;
        }
        WorkflowProcessRegistryDO created = new WorkflowProcessRegistryDO();
        created.setScopeType(SCOPE_GROUP);
        created.setScopeId(groupId);
        created.setProcessCode(processCode);
        created.setBusinessName(businessName);
        created.setTemplateId(null);
        created.setCreatedBy(operatorId);
        created.setUpdatedBy(operatorId);
        workflowProcessRegistryRepository.save(created);
        return created;
    }

    private void publishDefaultConfig(WorkflowDefinitionConfigDO config, Long operatorId) {
        String processDefinitionKey = processDefinitionKey(config.getBusinessCode(), config.getWorkflowCode(), config.getScopeType(), config.getScopeId());
        byte[] xmlBytes = WorkflowBpmnModelSupport.buildBpmnXml(
                processDefinitionKey,
                config.getWorkflowName(),
                WorkflowBpmnModelSupport.parseNodes(config.getNodeConfigJson(), objectMapper),
                objectMapper
        );
        Deployment deployment = repositoryService.createDeployment()
                .name(config.getWorkflowName())
                .key(processDefinitionKey)
                .addBytes(processDefinitionKey + ".bpmn20.xml", xmlBytes)
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new BusinessException("内置审批流程发布失败");
        }
        config.setStatus(publishedStatus());
        config.setVersionNo((config.getVersionNo() == null ? 0 : config.getVersionNo()) + 1);
        config.setProcessDefinitionKey(processDefinition.getKey());
        config.setProcessDefinitionId(processDefinition.getId());
        config.setDeployedAt(LocalDateTime.now());
        config.setUpdatedBy(operatorId);
        workflowDefinitionConfigRepository.update(config);
    }

    private boolean requiresDefaultWorkflowRefresh(String nodeConfigJson) {
        if (!StringUtils.hasText(nodeConfigJson)) {
            return true;
        }
        try {
            JsonNode nodes = objectMapper.readTree(nodeConfigJson);
            if (nodes == null || !nodes.isArray()) {
                return true;
            }
            for (JsonNode node : nodes) {
                if (!"finance_approval".equals(trimToNull(node.path("nodeKey").asText(null)))) {
                    continue;
                }
                String nodeType = trimToNull(node.path("nodeType").asText(null));
                String expression = trimToNull(node.path("conditionExpression").asText(null));
                return !"CONDITION".equalsIgnoreCase(nodeType)
                        || !StringUtils.hasText(expression)
                        || !expression.contains(WorkflowBpmnModelSupport.CONDITION_TRUE_EXPRESSION)
                        || !expression.contains(WorkflowBpmnModelSupport.CONDITION_FALSE_EXPRESSION);
            }
            return true;
        } catch (Exception ex) {
            return true;
        }
    }

    private String processDefinitionKey(String businessCode, String workflowCode, String scopeType, Long scopeId) {
        return normalizeCodeValue(businessCode) + "_" + normalizeCodeValue(workflowCode) + "_" + scopeType.toLowerCase(Locale.ROOT) + "_" + scopeId;
    }

    private String normalizeCodeValue(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String draftStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.WORKFLOW_DEFINITION_STATUS, DictionaryCodes.DRAFT);
    }

    private String publishedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.WORKFLOW_DEFINITION_STATUS, DictionaryCodes.PUBLISHED);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record WorkflowProcessSeed(String processCode, String businessName) {
    }

}
