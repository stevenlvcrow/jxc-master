package com.boboboom.jxc.workflow.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.workflow.application.service.WorkflowBpmnModelSupport.NodeConfig;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowConfigSaveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 流程配置业务服务，负责审批节点配置保存和读取。 */
@Service
public class WorkflowConfigApplicationService {

    private static final int DEFAULT_PUBLISH_HISTORY_LIMIT = 200;
    private static final int MAX_PUBLISH_HISTORY_LIMIT = 1000;

    private static final String NODE_TYPE_NORMAL = WorkflowBpmnModelSupport.NODE_TYPE_NORMAL;
    private static final String ROLE_SIGN_MODE_OR = "OR";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String UNSUPPORTED_ADVANCED_FIELD_MESSAGE = "当前版本暂不支持会签方式、允许驳回或允许反审核配置";

    private final WorkflowDefinitionConfigRepository configRepository;
    private final WorkflowProcessRegistryRepository processRegistryRepository;
    private final ObjectMapper objectMapper;
    private final RepositoryService repositoryService;
    private final OrgScopeService orgScopeService;
    private final DictionaryLookupService dictionaryLookupService;

    /** 流程配置业务服务，负责审批节点配置保存和读取。 */
    public WorkflowConfigApplicationService(WorkflowDefinitionConfigRepository configRepositoryValue,
                                            WorkflowProcessRegistryRepository processRegistryRepositoryValue,
                                            ObjectMapper objectMapperValue,
                                            RepositoryService repositoryServiceValue,
                                            OrgScopeService orgScopeServiceValue,
                                            DictionaryLookupService dictionaryLookupServiceValue) {
        this.configRepository = configRepositoryValue;
        this.processRegistryRepository = processRegistryRepositoryValue;
        this.objectMapper = objectMapperValue;
        this.repositoryService = repositoryServiceValue;
        this.orgScopeService = orgScopeServiceValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 获取Current。 */
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

    /** 保存业务数据。 */
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
        config.setStatus(draftStatus());
        config.setUpdatedBy(operatorId);
        config.setProcessDefinitionKey(null);
        config.setProcessDefinitionId(null);
        config.setDeployedAt(null);
        if (config.getId() == null) {
            configRepository.save(config);
        } else {
            configRepository.update(config);
        }
    }

    /** 发布流程配置。 */
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

    /** 查询流程发布历史。 */
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

    /** 分页查询流程发布历史。 */
    public List<WorkflowPublishHistoryManageView> publishHistories(String orgId, Integer limit) {
        Scope scope = resolveScope(orgId);
        int safeLimit = limit == null || limit < 1 ? DEFAULT_PUBLISH_HISTORY_LIMIT : Math.min(limit, MAX_PUBLISH_HISTORY_LIMIT);
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

    /** 审批流程配置类，注册框架组件和运行参数。 */
    @Transactional
    public void deleteConfig(String orgId, Long id) {
        Scope scope = resolveScope(orgId);
        WorkflowDefinitionConfigDO config = configRepository.findById(id).orElse(null);
        if (config == null
                || !scope.scopeType().equals(config.getScopeType())
                || !scope.scopeId().equals(config.getScopeId())) {
            throw new BusinessException("流程模板不存在");
        }
        WorkflowProcessRegistryDO registry = processRegistryRepository
                .findByScopeAndProcessCode(config.getScopeType(), config.getScopeId(), config.getBusinessCode())
                .orElse(null);
        if (registry != null && config.getWorkflowCode().equals(registry.getTemplateId())) {
            throw new BusinessException("正在使用的流程版本不允许删除");
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
                draftStatus(),
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
        return new NodeView(
                node.nodeKey(),
                node.nodeName(),
                node.x(),
                node.y(),
                roleNode ? trimNullable(node.approverRoleCode()) : "",
                ROLE_SIGN_MODE_OR,
                supportsApproverUser(nodeType) ? node.approverUserId() : null,
                false,
                false,
                nodeType,
                node.conditionExpression(),
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
        config.setStatus(publishedStatus());
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
            validateUnsupportedAdvancedFields(node);
            String roleCode = normalizeApproverRoleCode(node, roleNode);
            Long approverUserId = supportsApproverUser(nodeType) ? node.approverUserId() : null;
            List<String> triggerActions = normalizeTriggerActions(node.triggerActions(), nodeType);
            String conditionExpression = normalizeConditionExpression(node.conditionExpression());
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
                    ROLE_SIGN_MODE_OR,
                    approverUserId,
                    false,
                    false,
                    nodeType,
                    conditionExpression,
                    triggerActions
            ));
        }
        WorkflowBpmnModelSupport.validateAndBuildGraph(normalized, objectMapper);
        return normalized;
    }

    private String normalizeApproverRoleCode(WorkflowConfigSaveRequest.NodeItem node, boolean roleNode) {
        String roleCode = trimNullable(node.approverRoleCode());
        return roleNode && roleCode != null ? roleCode : "";
    }

    private List<NodeConfig> parseNodes(String configJson) {
        return WorkflowBpmnModelSupport.parseNodes(configJson, objectMapper);
    }

    private String toConfigJson(List<NodeConfig> nodes) {
        return WorkflowBpmnModelSupport.toConfigJson(nodes, objectMapper);
    }

    private byte[] buildBpmnXml(String processDefinitionKey, String workflowName, List<NodeConfig> nodes) {
        return WorkflowBpmnModelSupport.buildBpmnXml(processDefinitionKey, workflowName, nodes, objectMapper);
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
        return WorkflowBpmnModelSupport.normalizeNodeType(value);
    }

    private void validateUnsupportedAdvancedFields(WorkflowConfigSaveRequest.NodeItem node) {
        if (node == null) {
            return;
        }
        if (Boolean.TRUE.equals(node.allowReject())
                || Boolean.TRUE.equals(node.allowUnapprove())) {
            throw new BusinessException(UNSUPPORTED_ADVANCED_FIELD_MESSAGE);
        }
        String roleSignMode = trimNullable(node.roleSignMode());
        if (roleSignMode != null && !ROLE_SIGN_MODE_OR.equalsIgnoreCase(roleSignMode)) {
            throw new BusinessException(UNSUPPORTED_ADVANCED_FIELD_MESSAGE);
        }
    }

    private boolean supportsRoleAssignment(String nodeType) {
        return WorkflowBpmnModelSupport.supportsRoleAssignment(nodeType);
    }

    private boolean supportsApproverUser(String nodeType) {
        return WorkflowBpmnModelSupport.supportsApproverUser(nodeType);
    }

    private String normalizeConditionExpression(String value) {
        String normalized = trimNullable(value);
        if (normalized == null) {
            return "";
        }
        try {
            List<WorkflowEdgeLink> links = objectMapper.readValue(normalized, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            if (links == null || links.isEmpty()) {
                return "";
            }
            List<WorkflowEdgeLink> normalizedLinks = new ArrayList<>();
            for (WorkflowEdgeLink link : links) {
                String targetKey = trimNullable(link.to());
                if (!StringUtils.hasText(targetKey)) {
                    throw new BusinessException("流程连线目标节点不能为空");
                }
                normalizedLinks.add(new WorkflowEdgeLink(
                        targetKey,
                        WorkflowBpmnModelSupport.normalizeConditionExpression(link.expression())
                ));
            }
            return objectMapper.writeValueAsString(normalizedLinks);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("流程连线配置数据损坏");
        }
    }

    private String draftStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.WORKFLOW_DEFINITION_STATUS, DictionaryCodes.DRAFT);
    }

    private String publishedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.WORKFLOW_DEFINITION_STATUS, DictionaryCodes.PUBLISHED);
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

    /** 审批流程视图模型，承载页面展示数据。 */
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

    /** 审批流程视图模型，承载页面展示数据。 */
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

    /** 审批流程视图模型，承载页面展示数据。 */
    public record PublishResultView(String processDefinitionId,
                                    String processDefinitionKey,
                                    Integer version,
                                    String deployedAt) {
    }

    /** 审批流程视图模型，承载页面展示数据。 */
    public record WorkflowPublishHistoryView(String processDefinitionId,
                                             String processDefinitionKey,
                                             Integer version,
                                             String deploymentId) {
    }

    /** 审批流程视图模型，承载页面展示数据。 */
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

    private record WorkflowEdgeLink(String to, String expression) {
    }
}
