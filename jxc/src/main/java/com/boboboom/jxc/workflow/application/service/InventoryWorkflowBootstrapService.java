package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库存单据流程默认配置初始化服务。
 */
@Service
public class InventoryWorkflowBootstrapService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SOURCE_BUSINESS_CODE = "PURCHASE_INBOUND";
    private static final List<WorkflowProcessSeed> EXTRA_PROCESS_SEEDS = List.of(
            new WorkflowProcessSeed("PURCHASE_APPLICATION", "采购单申请流程"),
            new WorkflowProcessSeed("PURCHASE_ORDER", "采购订单流程"),
            new WorkflowProcessSeed("PURCHASE_RECEIPT", "采购收货单流程"),
            new WorkflowProcessSeed("PURCHASE_RETURN", "采购退货单流程"),
            new WorkflowProcessSeed("INVENTORY_CHECK", "盘点单流程"),
            new WorkflowProcessSeed("MULTI_INVENTORY_CHECK", "多人盘点单流程")
    );

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
        if (sourceRegistry == null) {
            sourceRegistry = new WorkflowProcessRegistryDO();
            sourceRegistry.setScopeType(SCOPE_GROUP);
            sourceRegistry.setScopeId(groupId);
            sourceRegistry.setProcessCode(SOURCE_BUSINESS_CODE);
            sourceRegistry.setBusinessName(InventoryDocumentType.PURCHASE_INBOUND.getBusinessName() + "流程");
            sourceRegistry.setTemplateId(null);
            sourceRegistry.setCreatedBy(operatorId);
            sourceRegistry.setUpdatedBy(operatorId);
            workflowProcessRegistryRepository.save(sourceRegistry);
        } else if (!StringUtils.hasText(sourceRegistry.getBusinessName())) {
            sourceRegistry.setBusinessName(InventoryDocumentType.PURCHASE_INBOUND.getBusinessName() + "流程");
            sourceRegistry.setUpdatedBy(operatorId);
            workflowProcessRegistryRepository.update(sourceRegistry);
        }
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

        for (WorkflowProcessSeed seed : EXTRA_PROCESS_SEEDS) {
            ensureProcessRegistry(groupId, operatorId, seed.processCode(), seed.businessName());
        }

        for (InventoryDocumentType type : InventoryDocumentType.workflowTypes()) {
            if (type == InventoryDocumentType.PURCHASE_INBOUND) {
                continue;
            }
            WorkflowProcessRegistryDO registry = ensureProcessRegistry(groupId, operatorId, type.getBusinessCode(), type.getBusinessName() + "流程");
            if (!StringUtils.hasText(registry.getTemplateId()) && StringUtils.hasText(sourceTemplateId)) {
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record WorkflowProcessSeed(String processCode, String businessName) {
    }
}
