package com.boboboom.jxc.workflow.application.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
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

/**
 * 库存单据流程默认配置初始化服务。
 */
@Service
public class InventoryWorkflowBootstrapService {

    private static final String SCOPE_GROUP = "GROUP";
    public static final String DEFAULT_WORKFLOW_CODE = "BUILTIN_DEFAULT_APPROVAL";
    private static final String DEFAULT_WORKFLOW_NAME = "内置通用审批流程";
    private static final String NODE_TYPE_START = "START";
    private static final String NODE_TYPE_SUCCESS = "SUCCESS";
    private static final String NODE_TYPE_FAIL = "FAIL";
    private static final String NODE_TYPE_END = "END";
    private static final String DEFAULT_NODE_CONFIG_JSON = """
            [
              {"nodeKey":"start_node","nodeName":"开始","x":88,"y":76,"approverRoleCode":"","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":false,"nodeType":"START","conditionExpression":"","triggerActions":[]},
              {"nodeKey":"business_fill","nodeName":"业务填报","x":340,"y":76,"approverRoleCode":"SALESMAN","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":false,"nodeType":"NORMAL","conditionExpression":"","triggerActions":["CREATE","UPDATE","DELETE"]},
              {"nodeKey":"finance_approval","nodeName":"财务审批","x":632,"y":76,"approverRoleCode":"FINANCE","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":false,"nodeType":"NORMAL","conditionExpression":"","triggerActions":[]},
              {"nodeKey":"success_node","nodeName":"成功","x":924,"y":76,"approverRoleCode":"","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":true,"nodeType":"SUCCESS","conditionExpression":"","triggerActions":[]},
              {"nodeKey":"fail_node","nodeName":"失败","x":412,"y":324,"approverRoleCode":"","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":false,"nodeType":"FAIL","conditionExpression":"","triggerActions":[]},
              {"nodeKey":"end_node","nodeName":"结束","x":722,"y":324,"approverRoleCode":"","roleSignMode":"OR","approverUserId":null,"allowReject":false,"allowUnapprove":false,"nodeType":"END","conditionExpression":"","triggerActions":[]}
            ]
            """;
    private static final List<NodeConfig> DEFAULT_NODES = List.of(
            new NodeConfig("start_node", "开始", "", NODE_TYPE_START, List.of()),
            new NodeConfig("business_fill", "业务填报", "SALESMAN", "NORMAL", List.of("CREATE", "UPDATE", "DELETE")),
            new NodeConfig("finance_approval", "财务审批", "FINANCE", "NORMAL", List.of()),
            new NodeConfig("success_node", "成功", "", NODE_TYPE_SUCCESS, List.of()),
            new NodeConfig("fail_node", "失败", "", NODE_TYPE_FAIL, List.of()),
            new NodeConfig("end_node", "结束", "", NODE_TYPE_END, List.of())
    );
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
    private final RepositoryService repositoryService;
    private final DictionaryLookupService dictionaryLookupService;

    /** 审批流程服务，负责相关业务规则和流程协作。 */
    public InventoryWorkflowBootstrapService(WorkflowProcessRegistryRepository workflowProcessRegistryRepositoryValue,
                                            WorkflowDefinitionConfigRepository workflowDefinitionConfigRepositoryValue,
                                            RepositoryService repositoryServiceValue,
                                            DictionaryLookupService dictionaryLookupServiceValue) {
        this.workflowProcessRegistryRepository = workflowProcessRegistryRepositoryValue;
        this.workflowDefinitionConfigRepository = workflowDefinitionConfigRepositoryValue;
        this.repositoryService = repositoryServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
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
                || !StringUtils.hasText(existing.getProcessDefinitionId())) {
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
        byte[] xmlBytes = buildBpmnXml(processDefinitionKey, config.getWorkflowName(), DEFAULT_NODES);
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

    private byte[] buildBpmnXml(String processDefinitionKey, String workflowName, List<NodeConfig> nodes) {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(processDefinitionKey);
        process.setName(workflowName);
        bpmnModel.addProcess(process);

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start_event");
        startEvent.setName("开始");
        process.addFlowElement(startEvent);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("end_event");
        endEvent.setName("结束");
        process.addFlowElement(endEvent);

        String sourceRef = startEvent.getId();
        for (int i = 0; i < nodes.size(); i++) {
            NodeConfig node = nodes.get(i);
            if (NODE_TYPE_START.equals(node.nodeType())) {
                continue;
            }
            if (isTerminalNode(node.nodeType())) {
                process.addFlowElement(sequenceFlow("flow_" + i, sourceRef, endEvent.getId()));
                sourceRef = endEvent.getId();
                break;
            }

            String taskId = "task_" + node.nodeKey();
            UserTask userTask = new UserTask();
            userTask.setId(taskId);
            userTask.setName(node.nodeName());
            userTask.setDocumentation(
                    "nodeType=" + node.nodeType()
                            + ";approverRoleCode=" + node.approverRoleCode()
                            + ";approverUserId="
                            + ";triggerActions=" + String.join(",", node.triggerActions())
            );
            process.addFlowElement(userTask);

            process.addFlowElement(sequenceFlow("flow_" + i, sourceRef, taskId));
            sourceRef = taskId;
        }
        if (!endEvent.getId().equals(sourceRef)) {
            process.addFlowElement(sequenceFlow("flow_end", sourceRef, endEvent.getId()));
        }
        return new BpmnXMLConverter().convertToXML(bpmnModel, StandardCharsets.UTF_8.name());
    }

    private SequenceFlow sequenceFlow(String id, String sourceRef, String targetRef) {
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId(id);
        sequenceFlow.setSourceRef(sourceRef);
        sequenceFlow.setTargetRef(targetRef);
        return sequenceFlow;
    }

    private boolean isTerminalNode(String nodeType) {
        return NODE_TYPE_END.equals(nodeType)
                || NODE_TYPE_SUCCESS.equals(nodeType)
                || NODE_TYPE_FAIL.equals(nodeType);
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

    private record NodeConfig(String nodeKey,
                              String nodeName,
                              String approverRoleCode,
                              String nodeType,
                              List<String> triggerActions) {
    }
}
