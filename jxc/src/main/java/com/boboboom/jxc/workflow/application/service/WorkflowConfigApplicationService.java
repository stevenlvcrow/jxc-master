package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowConfigSaveRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WorkflowConfigApplicationService {

    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String NODE_TYPE_NORMAL = "NORMAL";
    private static final String NODE_TYPE_CONDITION = "CONDITION";
    private static final String NODE_TYPE_SUCCESS = "SUCCESS";
    private static final String NODE_TYPE_FAIL = "FAIL";
    private static final String NODE_TYPE_START = "START";
    private static final String NODE_TYPE_END = "END";
    private static final String ROLE_SIGN_MODE_OR = "OR";
    private static final String ROLE_SIGN_MODE_AND = "AND";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final WorkflowDefinitionConfigRepository configRepository;
    private final ObjectMapper objectMapper;
    private final RepositoryService repositoryService;
    private final OrgScopeService orgScopeService;

    public WorkflowConfigApplicationService(WorkflowDefinitionConfigRepository configRepository,
                                            ObjectMapper objectMapper,
                                            RepositoryService repositoryService,
                                            OrgScopeService orgScopeService) {
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
        this.repositoryService = repositoryService;
        this.orgScopeService = orgScopeService;
    }

    public WorkflowConfigView getCurrent(String orgId, String businessCode, String workflowCode) {
        Scope scope = resolveScope(orgId);
        String normalizedBusinessCode = normalizeCode(businessCode, "业务编码不能为空");
        String normalizedWorkflowCode = normalizeCode(workflowCode, "流程编码不能为空");
        WorkflowDefinitionConfigDO config = findConfig(scope, normalizedBusinessCode, normalizedWorkflowCode);
        if (config == null) {
            return emptyConfig(scope, normalizedBusinessCode, normalizedWorkflowCode);
        }
        return toView(config, parseNodes(config.getNodeConfigJson()), scope, scope, false);
    }

    @Transactional
    public void save(String orgId, WorkflowConfigSaveRequest request) {
        Scope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String businessCode = normalizeCode(request.businessCode(), "业务编码不能为空");
        String workflowCode = normalizeCode(request.workflowCode(), "流程编码不能为空");
        String workflowName = requiredTrim(request.workflowName(), "流程名称不能为空");
        List<NodeConfig> nodes = normalizeNodes(request.nodes());

        WorkflowDefinitionConfigDO config = findConfig(scope, businessCode, workflowCode);
        if (config == null) {
            config = new WorkflowDefinitionConfigDO();
            config.setScopeType(scope.scopeType());
            config.setScopeId(scope.scopeId());
            config.setBusinessCode(businessCode);
            config.setWorkflowCode(workflowCode);
            config.setCreatedBy(operatorId);
        }
        config.setWorkflowName(workflowName);
        config.setNodeConfigJson(toConfigJson(nodes));
        config.setStatus(STATUS_DRAFT);
        config.setUpdatedBy(operatorId);
        config.setProcessDefinitionKey(null);
        config.setProcessDefinitionId(null);
        config.setDeployedAt(null);
        if (config.getId() == null) {
            configRepository.save(config);
        } else {
            configRepository.update(config);
        }
        publishConfig(scope, config, nodes, operatorId);
    }

    @Transactional
    public PublishResultView publish(String orgId, String businessCode, String workflowCode) {
        Scope scope = resolveScope(orgId);
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        String normalizedBusinessCode = normalizeCode(businessCode, "业务编码不能为空");
        String normalizedWorkflowCode = normalizeCode(workflowCode, "流程编码不能为空");
        WorkflowDefinitionConfigDO config = findConfig(scope, normalizedBusinessCode, normalizedWorkflowCode);
        if (config == null) {
            throw new BusinessException("请先保存流程节点配置");
        }
        List<NodeConfig> nodes = parseNodes(config.getNodeConfigJson());
        if (nodes.isEmpty()) {
            throw new BusinessException("流程节点不能为空");
        }
        return publishConfig(scope, config, nodes, operatorId);
    }

    public List<WorkflowPublishHistoryView> history(String orgId, String businessCode, String workflowCode) {
        Scope scope = resolveScope(orgId);
        String normalizedBusinessCode = normalizeCode(businessCode, "业务编码不能为空");
        String normalizedWorkflowCode = normalizeCode(workflowCode, "流程编码不能为空");
        List<WorkflowDefinitionConfigDO> configs = configRepository.findByScopeBusinessAndWorkflowOrdered(
                scope.scopeType(),
                scope.scopeId(),
                normalizedBusinessCode,
                normalizedWorkflowCode
        );
        List<WorkflowPublishHistoryView> rows = new ArrayList<>();
        for (WorkflowDefinitionConfigDO config : configs) {
            rows.add(new WorkflowPublishHistoryView(
                    config.getProcessDefinitionId(),
                    config.getProcessDefinitionKey(),
                    config.getVersionNo() == null ? 0 : config.getVersionNo(),
                    formatDateTime(config.getDeployedAt())
            ));
        }
        return rows;
    }

    public List<WorkflowPublishHistoryManageView> publishHistories(String orgId, Integer limit) {
        Scope scope = resolveScope(orgId);
        int safeLimit = limit == null || limit < 1 ? 200 : Math.min(limit, 1000);
        List<WorkflowDefinitionConfigDO> configs = configRepository.findByScopeOrdered(
                scope.scopeType(),
                scope.scopeId(),
                safeLimit
        );
        List<WorkflowPublishHistoryManageView> rows = new ArrayList<>();
        for (WorkflowDefinitionConfigDO config : configs) {
            rows.add(new WorkflowPublishHistoryManageView(
                    config.getId(),
                    config.getBusinessCode(),
                    config.getWorkflowCode(),
                    config.getWorkflowName(),
                    config.getStatus(),
                    config.getVersionNo() == null ? 0 : config.getVersionNo(),
                    formatDateTime(config.getUpdatedAt())
            ));
        }
        return rows;
    }

    @Transactional
    public void deleteConfig(String orgId, Long id) {
        Scope scope = resolveScope(orgId);
        WorkflowDefinitionConfigDO config = configRepository.findById(id)
                .filter(row -> scope.scopeType().equals(row.getScopeType()))
                .filter(row -> scope.scopeId().equals(row.getScopeId()))
                .orElse(null);
        if (config == null) {
            throw new BusinessException("流程模板不存在");
        }
        configRepository.deleteById(config.getId());
    }

    private WorkflowConfigView emptyConfig(Scope scope, String businessCode, String workflowCode) {
        return new WorkflowConfigView(
                scope.scopeId(),
                scope.scopeType(),
                businessCode,
                workflowCode,
                "",
                STATUS_DRAFT,
                0,
                List.of(),
                null,
                null,
                "",
                false,
                scope.scopeType(),
                scope.scopeId()
        );
    }

    private WorkflowConfigView toView(WorkflowDefinitionConfigDO config,
                                      List<NodeConfig> nodes,
                                      Scope appliedScope,
                                      Scope sourceScope,
                                      boolean inherited) {
        return new WorkflowConfigView(
                appliedScope.scopeId(),
                appliedScope.scopeType(),
                config.getBusinessCode(),
                config.getWorkflowCode(),
                config.getWorkflowName(),
                config.getStatus(),
                config.getVersionNo() == null ? 0 : config.getVersionNo(),
                nodes.stream().map(this::toNodeView).toList(),
                config.getProcessDefinitionKey(),
                config.getProcessDefinitionId(),
                formatDateTime(config.getDeployedAt()),
                inherited,
                sourceScope.scopeType(),
                sourceScope.scopeId()
        );
    }

    private NodeView toNodeView(NodeConfig node) {
        String nodeType = normalizeNodeType(node.nodeType());
        boolean roleNode = supportsRoleAssignment(nodeType);
        boolean conditionNode = NODE_TYPE_CONDITION.equals(nodeType);
        return new NodeView(
                node.nodeKey(),
                node.nodeName(),
                node.x(),
                node.y(),
                roleNode ? trimNullable(node.approverRoleCode()) : "",
                conditionNode ? normalizeRoleSignMode(node.roleSignMode()) : ROLE_SIGN_MODE_OR,
                supportsApproverUser(nodeType) ? node.approverUserId() : null,
                false,
                node.allowUnapprove(),
                nodeType,
                node.conditionExpression() == null ? "" : node.conditionExpression(),
                node.triggerActions() == null ? List.of() : node.triggerActions()
        );
    }

    private WorkflowDefinitionConfigDO findConfig(Scope scope, String businessCode, String workflowCode) {
        return configRepository.findByScopeBusinessAndWorkflow(scope.scopeType(), scope.scopeId(), businessCode, workflowCode)
                .orElse(null);
    }

    private PublishResultView publishConfig(Scope scope,
                                            WorkflowDefinitionConfigDO config,
                                            List<NodeConfig> nodes,
                                            Long operatorId) {
        String processDefinitionKey = processDefinitionKey(scope, config.getBusinessCode(), config.getWorkflowCode());
        byte[] xmlBytes = buildBpmnXml(processDefinitionKey, config.getWorkflowName(), nodes);
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
            throw new BusinessException("流程发布失败，请稍后重试");
        }

        int nextVersion = (config.getVersionNo() == null ? 0 : config.getVersionNo()) + 1;
        LocalDateTime deployedAt = LocalDateTime.now();
        config.setStatus(STATUS_PUBLISHED);
        config.setVersionNo(nextVersion);
        config.setProcessDefinitionKey(processDefinition.getKey());
        config.setProcessDefinitionId(processDefinition.getId());
        config.setDeployedAt(deployedAt);
        config.setUpdatedBy(operatorId);
        configRepository.update(config);
        return new PublishResultView(
                processDefinition.getId(),
                processDefinition.getKey(),
                processDefinition.getVersion(),
                formatDateTime(deployedAt)
        );
    }

    private List<NodeConfig> normalizeNodes(List<WorkflowConfigSaveRequest.NodeItem> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("流程节点不能为空");
        }
        List<NodeConfig> normalized = new ArrayList<>();
        Set<String> keySet = new LinkedHashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowConfigSaveRequest.NodeItem node = nodes.get(i);
            String nodeType = normalizeNodeType(node.nodeType());
            String nodeName = requiredTrim(node.nodeName(), "节点名称不能为空");
            boolean roleNode = supportsRoleAssignment(nodeType);
            boolean conditionNode = NODE_TYPE_CONDITION.equals(nodeType);
            String roleCode = roleNode && trimNullable(node.approverRoleCode()) != null ? trimNullable(node.approverRoleCode()) : "";
            String roleSignMode = conditionNode ? normalizeRoleSignMode(node.roleSignMode()) : ROLE_SIGN_MODE_OR;
            Long approverUserId = supportsApproverUser(nodeType) ? node.approverUserId() : null;
            List<String> triggerActions = normalizeTriggerActions(node.triggerActions(), nodeType);
            if (NODE_TYPE_NORMAL.equals(nodeType) && !triggerActions.isEmpty() && !StringUtils.hasText(roleCode)) {
                throw new BusinessException("普通节点已选择触发动作，请同时配置审批角色");
            }
            String nodeKey = normalizeNodeKey(node.nodeKey(), i + 1);
            if (!keySet.add(nodeKey)) {
                throw new BusinessException("节点编码重复：" + nodeKey);
            }
            normalized.add(new NodeConfig(
                    nodeKey,
                    nodeName,
                    node.x(),
                    node.y(),
                    roleCode,
                    roleSignMode,
                    approverUserId,
                    false,
                    NODE_TYPE_SUCCESS.equals(nodeType) && Boolean.TRUE.equals(node.allowUnapprove()),
                    nodeType,
                    trimNullable(node.conditionExpression()) == null ? "" : trimNullable(node.conditionExpression()),
                    triggerActions
            ));
        }
        return normalized;
    }

    private List<NodeConfig> parseNodes(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return List.of();
        }
        try {
            List<NodeConfig> nodes = objectMapper.readValue(configJson, new TypeReference<>() {
            });
            return nodes == null ? List.of() : nodes;
        } catch (Exception ex) {
            throw new BusinessException("流程节点配置数据损坏");
        }
    }

    private String toConfigJson(List<NodeConfig> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception ex) {
            throw new BusinessException("流程配置序列化失败");
        }
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
            String nodeType = normalizeNodeType(node.nodeType());
            if (NODE_TYPE_START.equals(nodeType)) {
                continue;
            }
            if (NODE_TYPE_END.equals(nodeType)) {
                process.addFlowElement(sequenceFlow("flow_" + i, sourceRef, endEvent.getId()));
                sourceRef = endEvent.getId();
                break;
            }

            String taskId = "task_" + node.nodeKey();
            UserTask userTask = new UserTask();
            userTask.setId(taskId);
            userTask.setName(node.nodeName());
            userTask.setDocumentation(
                    "nodeType=" + nodeType
                            + ";allowReject=" + node.allowReject()
                            + ";allowUnapprove=" + node.allowUnapprove()
                            + ";approverRoleCode=" + (node.approverRoleCode() == null ? "" : node.approverRoleCode())
                            + ";roleSignMode=" + normalizeRoleSignMode(node.roleSignMode())
                            + ";approverUserId=" + (node.approverUserId() == null ? "" : node.approverUserId())
                            + ";triggerActions=" + String.join(",", node.triggerActions() == null ? List.of() : node.triggerActions())
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

    private Scope resolveScope(String orgId) {
        OrgScopeService.WorkflowScope scope = orgScopeService.resolveWorkflowScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new Scope(scope.scopeType(), scope.scopeId(), scope.groupId());
    }

    private String processDefinitionKey(Scope scope, String businessCode, String workflowCode) {
        return normalizeCodeValue(businessCode) + "_" + normalizeCodeValue(workflowCode) + "_" + scope.scopeType().toLowerCase(Locale.ROOT) + "_" + scope.scopeId();
    }

    private String normalizeCode(String value, String message) {
        String normalized = requiredTrim(value, message).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_\\-]+")) {
            throw new BusinessException("编码仅支持字母、数字、下划线和中划线");
        }
        return normalized;
    }

    private String normalizeCodeValue(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private List<String> normalizeTriggerActions(List<String> triggerActions, String nodeType) {
        if (!NODE_TYPE_NORMAL.equals(nodeType) || triggerActions == null || triggerActions.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String action : triggerActions) {
            String value = trimNullable(action);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            if ("CREATE".equals(upper) || "UPDATE".equals(upper) || "DELETE".equals(upper)) {
                normalized.add(upper);
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeNodeKey(String value, int index) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            normalized = "node_" + index;
        }
        normalized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (!StringUtils.hasText(normalized)) {
            return "node_" + index;
        }
        return normalized;
    }

    private String normalizeNodeType(String value) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return NODE_TYPE_NORMAL;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (NODE_TYPE_CONDITION.equals(upper)
                || NODE_TYPE_SUCCESS.equals(upper)
                || NODE_TYPE_FAIL.equals(upper)
                || NODE_TYPE_START.equals(upper)
                || NODE_TYPE_END.equals(upper)
                || NODE_TYPE_NORMAL.equals(upper)) {
            return upper;
        }
        return NODE_TYPE_NORMAL;
    }

    private String normalizeRoleSignMode(String value) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return ROLE_SIGN_MODE_OR;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (ROLE_SIGN_MODE_AND.equals(upper)) {
            return ROLE_SIGN_MODE_AND;
        }
        return ROLE_SIGN_MODE_OR;
    }

    private boolean supportsRoleAssignment(String nodeType) {
        return NODE_TYPE_NORMAL.equals(nodeType) || NODE_TYPE_CONDITION.equals(nodeType);
    }

    private boolean supportsApproverUser(String nodeType) {
        return NODE_TYPE_CONDITION.equals(nodeType);
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

    public record WorkflowConfigView(Long scopeId,
                                     String scopeType,
                                     String businessCode,
                                     String workflowCode,
                                     String workflowName,
                                     String status,
                                     Integer versionNo,
                                     List<NodeView> nodes,
                                     String processDefinitionKey,
                                     String processDefinitionId,
                                     String deployedAt,
                                     boolean inherited,
                                     String sourceScopeType,
                                     Long sourceScopeId) {
    }

    public record NodeView(String nodeKey,
                           String nodeName,
                           Integer x,
                           Integer y,
                           String approverRoleCode,
                           String roleSignMode,
                           Long approverUserId,
                           boolean allowReject,
                           boolean allowUnapprove,
                           String nodeType,
                           String conditionExpression,
                           List<String> triggerActions) {
    }

    public record PublishResultView(String processDefinitionId,
                                    String processDefinitionKey,
                                    Integer version,
                                    String deployedAt) {
    }

    public record WorkflowPublishHistoryView(String processDefinitionId,
                                             String processDefinitionKey,
                                             Integer version,
                                             String deploymentId) {
    }

    public record WorkflowPublishHistoryManageView(Long id,
                                                   String businessCode,
                                                   String workflowCode,
                                                   String workflowName,
                                                   String status,
                                                   Integer versionNo,
                                                   String savedAt) {
    }

    private record Scope(String scopeType, Long scopeId, Long groupId) {
    }

    private record NodeConfig(String nodeKey,
                              String nodeName,
                              Integer x,
                              Integer y,
                              String approverRoleCode,
                              String roleSignMode,
                              Long approverUserId,
                              boolean allowReject,
                              boolean allowUnapprove,
                              String nodeType,
                              String conditionExpression,
                              List<String> triggerActions) {
    }
}
