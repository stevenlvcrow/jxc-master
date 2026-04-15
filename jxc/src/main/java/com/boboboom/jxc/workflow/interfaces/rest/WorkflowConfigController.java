package com.boboboom.jxc.workflow.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowDefinitionConfigMapper;
import com.boboboom.jxc.workflow.interfaces.rest.request.WorkflowConfigSaveRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/workflow/configs")
public class WorkflowConfigController {

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

    private final WorkflowDefinitionConfigMapper configMapper;
    private final GroupMapper groupMapper;
    private final RoleMapper roleMapper;
    private final StoreMapper storeMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final ObjectMapper objectMapper;
    private final RepositoryService repositoryService;

    public WorkflowConfigController(WorkflowDefinitionConfigMapper configMapper,
                                    GroupMapper groupMapper,
                                    RoleMapper roleMapper,
                                    StoreMapper storeMapper,
                                    UserRoleRelMapper userRoleRelMapper,
                                    ObjectMapper objectMapper,
                                    RepositoryService repositoryService) {
        this.configMapper = configMapper;
        this.groupMapper = groupMapper;
        this.roleMapper = roleMapper;
        this.storeMapper = storeMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.objectMapper = objectMapper;
        this.repositoryService = repositoryService;
    }

    @GetMapping("/current")
    public CodeDataResponse<WorkflowConfigView> getCurrent(@RequestParam(required = false) String orgId,
                                                           @RequestParam String businessCode,
                                                           @RequestParam String workflowCode) {
        Scope scope = resolveScope(orgId);
        String normalizedBusinessCode = normalizeCode(businessCode, "业务编码不能为空");
        String normalizedWorkflowCode = normalizeCode(workflowCode, "流程编码不能为空");
        WorkflowDefinitionConfigDO config = findConfig(scope, normalizedBusinessCode, normalizedWorkflowCode);
        if (config == null) {
            if (SCOPE_STORE.equals(scope.scopeType())) {
                Scope groupScope = new Scope(SCOPE_GROUP, scope.groupId(), scope.groupId());
                WorkflowDefinitionConfigDO groupConfig = findConfig(groupScope, normalizedBusinessCode, normalizedWorkflowCode);
                if (groupConfig != null) {
                    return CodeDataResponse.ok(toView(groupConfig, parseNodes(groupConfig.getNodeConfigJson()), scope, groupScope, true));
                }
            }
            return CodeDataResponse.ok(defaultConfig(scope, normalizedBusinessCode, normalizedWorkflowCode));
        }
        return CodeDataResponse.ok(toView(config, parseNodes(config.getNodeConfigJson()), scope, scope, false));
    }

    @PutMapping("/current")
    @Transactional
    public CodeDataResponse<Void> save(@RequestParam(required = false) String orgId,
                                       @Valid @RequestBody WorkflowConfigSaveRequest request) {
        Scope scope = resolveScope(orgId);
        Long operatorId = currentUserId();
        String businessCode = normalizeCode(request.businessCode(), "业务编码不能为空");
        String workflowCode = normalizeCode(request.workflowCode(), "流程编码不能为空");
        String workflowName = request.workflowName().trim();
        List<NodeConfig> nodes = normalizeNodes(request.nodes());
        String nodeConfigJson = toConfigJson(nodes);

        WorkflowDefinitionConfigDO config = findConfig(scope, businessCode, workflowCode);
        if (config == null) {
            config = new WorkflowDefinitionConfigDO();
            config.setScopeType(scope.scopeType());
            config.setScopeId(scope.scopeId());
            config.setBusinessCode(businessCode);
            config.setWorkflowCode(workflowCode);
            config.setVersionNo(0);
            config.setCreatedBy(operatorId);
        }
        config.setWorkflowName(workflowName);
        config.setNodeConfigJson(nodeConfigJson);
        config.setStatus(STATUS_DRAFT);
        config.setDeployedAt(LocalDateTime.now());
        config.setUpdatedBy(operatorId);
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return CodeDataResponse.ok();
    }

    @PostMapping("/publish")
    @Transactional
    public CodeDataResponse<PublishResultView> publish(@RequestParam(required = false) String orgId,
                                                       @RequestParam String businessCode,
                                                       @RequestParam String workflowCode) {
        Scope scope = resolveScope(orgId);
        Long operatorId = currentUserId();
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

        String processDefinitionKey = processDefinitionKey(scope, normalizedBusinessCode, normalizedWorkflowCode);
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
        configMapper.updateById(config);

        return CodeDataResponse.ok(new PublishResultView(
                processDefinition.getId(),
                processDefinition.getKey(),
                processDefinition.getVersion(),
                formatDateTime(deployedAt)
        ));
    }

    @GetMapping("/history")
    public CodeDataResponse<List<WorkflowPublishHistoryView>> history(@RequestParam(required = false) String orgId,
                                                                      @RequestParam String businessCode,
                                                                      @RequestParam String workflowCode,
                                                                      @RequestParam(defaultValue = "10") Integer limit) {
        Scope scope = resolveScope(orgId);
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        String processDefinitionKey = processDefinitionKey(
                scope,
                normalizeCode(businessCode, "业务编码不能为空"),
                normalizeCode(workflowCode, "流程编码不能为空")
        );
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion()
                .desc()
                .listPage(0, safeLimit);
        List<WorkflowPublishHistoryView> result = new ArrayList<>();
        for (ProcessDefinition definition : definitions) {
            result.add(new WorkflowPublishHistoryView(
                    definition.getId(),
                    definition.getKey(),
                    definition.getVersion(),
                    definition.getDeploymentId()
            ));
        }
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/publish-histories")
    public CodeDataResponse<List<WorkflowPublishHistoryManageView>> publishHistories(@RequestParam(required = false) String orgId,
                                                                                      @RequestParam(defaultValue = "200") Integer limit) {
        Scope scope = resolveScope(orgId);
        int safeLimit = limit == null || limit < 1 ? 200 : Math.min(limit, 1000);
        List<WorkflowDefinitionConfigDO> configs = configMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scope.scopeType())
                .eq(WorkflowDefinitionConfigDO::getScopeId, scope.scopeId())
                .orderByDesc(WorkflowDefinitionConfigDO::getUpdatedAt)
                .last("limit " + safeLimit));
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
        return CodeDataResponse.ok(rows);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public CodeDataResponse<Void> deleteConfig(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId) {
        Scope scope = resolveScope(orgId);
        WorkflowDefinitionConfigDO config = configMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getId, id)
                .eq(WorkflowDefinitionConfigDO::getScopeType, scope.scopeType())
                .eq(WorkflowDefinitionConfigDO::getScopeId, scope.scopeId())
                .last("limit 1"));
        if (config == null) {
            throw new BusinessException("流程模板不存在");
        }
        configMapper.deleteById(config.getId());
        return CodeDataResponse.ok();
    }

    private WorkflowConfigView defaultConfig(Scope scope, String businessCode, String workflowCode) {
        List<NodeConfig> defaultNodes = List.of(
                new NodeConfig("step_1", "步骤一", 64, 96, "", ROLE_SIGN_MODE_OR, null, false, false, NODE_TYPE_NORMAL, ""),
                new NodeConfig("step_2", "步骤二", 278, 96, "", ROLE_SIGN_MODE_OR, null, false, false, NODE_TYPE_NORMAL, "")
        );
        return new WorkflowConfigView(
                scope.scopeId(),
                scope.scopeType(),
                businessCode,
                workflowCode,
                "通用审批流程",
                STATUS_DRAFT,
                0,
                defaultNodes.stream().map(this::toNodeView).toList(),
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
        boolean conditionNode = NODE_TYPE_CONDITION.equals(nodeType);
        return new NodeView(
                node.nodeKey(),
                node.nodeName(),
                node.x(),
                node.y(),
                conditionNode ? trimNullable(node.approverRoleCode()) : "",
                conditionNode ? normalizeRoleSignMode(node.roleSignMode()) : ROLE_SIGN_MODE_OR,
                conditionNode ? node.approverUserId() : null,
                false,
                node.allowUnapprove(),
                nodeType,
                node.conditionExpression() == null ? "" : node.conditionExpression()
        );
    }

    private WorkflowDefinitionConfigDO findConfig(Scope scope, String businessCode, String workflowCode) {
        return configMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scope.scopeType())
                .eq(WorkflowDefinitionConfigDO::getScopeId, scope.scopeId())
                .eq(WorkflowDefinitionConfigDO::getBusinessCode, businessCode)
                .eq(WorkflowDefinitionConfigDO::getWorkflowCode, workflowCode)
                .last("limit 1"));
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
            boolean conditionNode = NODE_TYPE_CONDITION.equals(nodeType);
            String roleCode = conditionNode && trimNullable(node.approverRoleCode()) != null ? trimNullable(node.approverRoleCode()) : "";
            String roleSignMode = conditionNode ? normalizeRoleSignMode(node.roleSignMode()) : ROLE_SIGN_MODE_OR;
            Long approverUserId = conditionNode ? node.approverUserId() : null;
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
                    trimNullable(node.conditionExpression()) == null ? "" : trimNullable(node.conditionExpression())
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
        Long userId = currentUserId();
        Scope scope = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return scope;
        }
        if (SCOPE_GROUP.equals(scope.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, scope.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return scope;
        }
        boolean hasStoreScope = hasScope(userId, SCOPE_STORE, scope.scopeId());
        boolean hasGroupScope = hasScope(userId, SCOPE_GROUP, scope.groupId());
        if (!hasStoreScope && !hasGroupScope) {
            throw new BusinessException("当前账号无该门店权限");
        }
        return scope;
    }

    private Scope parseScope(String orgId) {
        String value = requiredTrim(orgId, "请先选择集团或门店机构");
        if (value.startsWith("group-")) {
            Long groupId = parseNumericId(value.substring("group-".length()));
            GroupDO group = groupMapper.selectById(groupId);
            if (group == null) {
                throw new BusinessException("集团不存在");
            }
            return new Scope(SCOPE_GROUP, groupId, groupId);
        }
        if (value.startsWith("store-")) {
            Long storeId = parseNumericId(value.substring("store-".length()));
            StoreDO store = storeMapper.selectById(storeId);
            if (store == null) {
                throw new BusinessException("门店不存在");
            }
            if (store.getGroupId() == null) {
                throw new BusinessException("门店未绑定集团");
            }
            return new Scope(SCOPE_STORE, storeId, store.getGroupId());
        }
        throw new BusinessException("请在集团或门店工作台中配置流程");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private Long currentUserId() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        return AuthContextHolder.get().getUserId();
    }

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, SCOPE_PLATFORM)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }

    private boolean hasScope(Long userId, String scopeType, Long scopeId) {
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
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
                           String conditionExpression) {
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
                              String conditionExpression) {
    }
}
