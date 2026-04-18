package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存单据流程默认配置初始化服务。
 */
@Service
public class InventoryWorkflowBootstrapService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SOURCE_BUSINESS_CODE = "PURCHASE_INBOUND";

    private final WorkflowProcessRegistryRepository workflowProcessRegistryRepository;
    private final WorkflowDefinitionConfigRepository workflowDefinitionConfigRepository;

    public InventoryWorkflowBootstrapService(WorkflowProcessRegistryRepository workflowProcessRegistryRepository,
                                            WorkflowDefinitionConfigRepository workflowDefinitionConfigRepository) {
        this.workflowProcessRegistryRepository = workflowProcessRegistryRepository;
        this.workflowDefinitionConfigRepository = workflowDefinitionConfigRepository;
    }

    /**
     * 确保库存单据流程默认业务和配置已存在。
     *
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     */
    @Transactional
    public void ensureDefaults(Long groupId, Long operatorId) {
        if (groupId == null || operatorId == null) {
            return;
        }
        WorkflowProcessRegistryDO sourceRegistry = workflowProcessRegistryRepository
                .findByScopeAndProcessCode(SCOPE_GROUP, groupId, SOURCE_BUSINESS_CODE)
                .orElse(null);
        String sourceTemplateId = sourceRegistry == null ? null : trimToNull(sourceRegistry.getTemplateId());
        WorkflowDefinitionConfigDO sourceConfig = null;
        if (StringUtils.hasText(sourceTemplateId)) {
            sourceConfig = workflowDefinitionConfigRepository.findByScopeBusinessAndWorkflow(
                    SCOPE_GROUP,
                    groupId,
                    SOURCE_BUSINESS_CODE,
                    sourceTemplateId
            ).orElse(null);
        }

        for (InventoryDocumentType type : InventoryDocumentType.workflowTypes()) {
            if (type == InventoryDocumentType.PURCHASE_INBOUND) {
                continue;
            }
            WorkflowProcessRegistryDO registry = workflowProcessRegistryRepository
                    .findByScopeAndProcessCode(SCOPE_GROUP, groupId, type.getBusinessCode())
                    .orElse(null);
            if (registry == null) {
                registry = new WorkflowProcessRegistryDO();
                registry.setScopeType(SCOPE_GROUP);
                registry.setScopeId(groupId);
                registry.setProcessCode(type.getBusinessCode());
                registry.setBusinessName(type.getBusinessName() + "流程");
                registry.setTemplateId(sourceTemplateId);
                registry.setCreatedBy(operatorId);
                registry.setUpdatedBy(operatorId);
                workflowProcessRegistryRepository.save(registry);
            } else if (!StringUtils.hasText(registry.getTemplateId()) && StringUtils.hasText(sourceTemplateId)) {
                registry.setTemplateId(sourceTemplateId);
                registry.setUpdatedBy(operatorId);
                workflowProcessRegistryRepository.update(registry);
            }
            if (sourceConfig == null || !StringUtils.hasText(sourceTemplateId)) {
                continue;
            }
            WorkflowDefinitionConfigDO targetConfig = workflowDefinitionConfigRepository.findByScopeBusinessAndWorkflow(
                    SCOPE_GROUP,
                    groupId,
                    type.getBusinessCode(),
                    sourceTemplateId
            ).orElse(null);
            if (targetConfig != null) {
                continue;
            }
            WorkflowDefinitionConfigDO copied = new WorkflowDefinitionConfigDO();
            copied.setScopeType(sourceConfig.getScopeType());
            copied.setScopeId(sourceConfig.getScopeId());
            copied.setBusinessCode(type.getBusinessCode());
            copied.setWorkflowCode(sourceConfig.getWorkflowCode());
            copied.setWorkflowName(type.getBusinessName() + "流程");
            copied.setNodeConfigJson(sourceConfig.getNodeConfigJson());
            copied.setStatus(sourceConfig.getStatus());
            copied.setVersionNo(sourceConfig.getVersionNo());
            copied.setProcessDefinitionKey(sourceConfig.getProcessDefinitionKey());
            copied.setProcessDefinitionId(sourceConfig.getProcessDefinitionId());
            copied.setDeployedAt(sourceConfig.getDeployedAt());
            copied.setCreatedBy(operatorId);
            copied.setUpdatedBy(operatorId);
            workflowDefinitionConfigRepository.save(copied);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
